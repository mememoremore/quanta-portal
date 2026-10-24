package com.ruoyi.qt.pages;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

/**
 * tar.gz 归档的安全解包与校验。
 *
 * 限制在流式读取时生效（不信任声明大小）：展开总量、单文件、普通文件数与条目数。
 * 移除恰好一层 GitHub 根目录后逐条校验：拒绝绝对路径、..、反斜杠、盘符、NUL、
 * 重复路径、符号/硬链接、设备与 FIFO；隐藏路径（任一段以 . 开头）不进入产物。
 * 只写出普通目录与普通文件，普通文件不带可执行权限。
 */
public class QtStaticArchiveValidator
{
    public record ValidatedArtifact(Path root, int fileCount, long totalBytes)
    {
    }

    public static class ArchiveValidationException extends RuntimeException
    {
        private static final long serialVersionUID = 1L;

        private final String errorCode;

        public ArchiveValidationException(String errorCode, String message)
        {
            super(message);
            this.errorCode = errorCode;
        }

        public String getErrorCode()
        {
            return errorCode;
        }
    }

    public static final String ERROR_LIMIT_EXCEEDED = "LIMIT_EXCEEDED";

    public static final String ERROR_PATH_ESCAPES = "PATH_ESCAPES";

    public static final String ERROR_SPECIAL_ENTRY = "SPECIAL_ENTRY";

    public static final String ERROR_DUPLICATE_PATH = "DUPLICATE_PATH";

    public static final String ERROR_INDEX_MISSING = "INDEX_MISSING";

    public static final String ERROR_PUBLISH_DIR_MISSING = "PUBLISH_DIR_MISSING";

    public static final String ERROR_CORRUPT_ARCHIVE = "CORRUPT_ARCHIVE";

    private static final Pattern DRIVE_PATTERN = Pattern.compile("^[A-Za-z]:");

    private static final Pattern PUBLISH_DIR_PATTERN = Pattern.compile("^/[A-Za-z0-9_./-]+$");

    private final long extractedLimitBytes;

    private final long maxFileBytes;

    private final int maxFiles;

    private final int maxEntries;

    public QtStaticArchiveValidator(long extractedLimitBytes, long maxFileBytes, int maxFiles, int maxEntries)
    {
        this.extractedLimitBytes = extractedLimitBytes;
        this.maxFileBytes = maxFileBytes;
        this.maxFiles = maxFiles;
        this.maxEntries = maxEntries;
    }

    /**
     * 解包到 staging（staging 会被重建为干净目录），返回最终发布目录及其统计。
     * 任何校验失败都会清空 staging 并抛出带 errorCode 的异常。
     */
    public ValidatedArtifact extract(Path archive, String publishDir, Path staging) throws IOException
    {
        String relativePublishDir = validatePublishDir(publishDir);
        deleteRecursively(staging);
        Files.createDirectories(staging);
        Set<String> seen = new HashSet<>();
        Counters counters = new Counters();
        String gitRoot = null;
        try (TarArchiveInputStream tar = new TarArchiveInputStream(
                new GzipCompressorInputStream(Files.newInputStream(archive))))
        {
            TarArchiveEntry entry;
            while ((entry = tar.getNextEntry()) != null)
            {
                if (++counters.entries > maxEntries)
                {
                    throw new ArchiveValidationException(ERROR_LIMIT_EXCEEDED, "archive has too many entries");
                }
                String name = normalizeName(entry.getName());
                if (name.isEmpty())
                {
                    continue;
                }
                if (gitRoot == null)
                {
                    gitRoot = name.contains("/") ? name.substring(0, name.indexOf('/')) : name;
                }
                if (!name.equals(gitRoot) && !name.startsWith(gitRoot + "/"))
                {
                    throw new ArchiveValidationException(ERROR_PATH_ESCAPES, "entry outside github root: " + name);
                }
                String relative = name.equals(gitRoot) ? "" : name.substring(gitRoot.length() + 1);
                if (relative.isEmpty())
                {
                    continue;
                }
                checkEntryType(entry, relative);
                rejectParentSegments(relative);
                if (isHidden(relative))
                {
                    continue;
                }
                Path target = staging.resolve(relative);
                if (!target.normalize().startsWith(staging.normalize()))
                {
                    throw new ArchiveValidationException(ERROR_PATH_ESCAPES, "entry escapes staging: " + relative);
                }
                String canonical = relative.replace('\\', '/');
                if (!seen.add(canonical))
                {
                    throw new ArchiveValidationException(ERROR_DUPLICATE_PATH, "duplicate entry: " + relative);
                }
                if (entry.isDirectory())
                {
                    Files.createDirectories(target);
                    applyPosixDirectoryPermissions(target);
                    continue;
                }
                writeFile(tar, target, relative, counters);
                applyPosixFilePermissions(target);
            }
        }
        catch (ArchiveValidationException e)
        {
            deleteRecursively(staging);
            throw e;
        }
        catch (IOException e)
        {
            deleteRecursively(staging);
            throw new ArchiveValidationException(ERROR_CORRUPT_ARCHIVE, "unreadable archive: " + e.getMessage());
        }
        catch (RuntimeException e)
        {
            deleteRecursively(staging);
            throw e;
        }

        Path root = relativePublishDir.isEmpty() ? staging : staging.resolve(relativePublishDir);
        if (!Files.isDirectory(root))
        {
            deleteRecursively(staging);
            throw new ArchiveValidationException(ERROR_PUBLISH_DIR_MISSING, "publish dir missing: " + publishDir);
        }
        Path index = root.resolve("index.html");
        if (!Files.isRegularFile(index))
        {
            deleteRecursively(staging);
            throw new ArchiveValidationException(ERROR_INDEX_MISSING, "publish dir has no index.html");
        }
        Published published = countPublished(root);
        return new ValidatedArtifact(root, published.files, published.bytes);
    }

    private void checkEntryType(TarArchiveEntry entry, String relative)
    {
        if (entry.isSymbolicLink() || entry.isLink() || entry.isFIFO()
                || entry.isCharacterDevice() || entry.isBlockDevice())
        {
            throw new ArchiveValidationException(ERROR_SPECIAL_ENTRY, "special entry not allowed: " + relative);
        }
        if (!entry.isDirectory() && !entry.isFile())
        {
            throw new ArchiveValidationException(ERROR_SPECIAL_ENTRY, "unsupported entry type: " + relative);
        }
    }

    private void writeFile(TarArchiveInputStream tar, Path target, String relative, Counters counters)
    throws IOException
    {
        if (++counters.files > maxFiles)
        {
            throw new ArchiveValidationException(ERROR_LIMIT_EXCEEDED, "archive has too many files");
        }
        Files.createDirectories(target.toAbsolutePath().getParent());
        long fileBytes = 0;
        try (OutputStream out = Files.newOutputStream(target))
        {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = tar.read(buffer)) != -1)
            {
                fileBytes += read;
                if (fileBytes > maxFileBytes)
                {
                    throw new ArchiveValidationException(ERROR_LIMIT_EXCEEDED,
                            "file exceeds size limit: " + relative);
                }
                counters.bytes += read;
                if (counters.bytes > extractedLimitBytes)
                {
                    throw new ArchiveValidationException(ERROR_LIMIT_EXCEEDED, "archive exceeds extracted limit");
                }
                out.write(buffer, 0, read);
            }
        }
    }

    private String validatePublishDir(String publishDir)
    {
        String value = publishDir == null ? "/" : publishDir.trim();
        if (value.contains("..") || value.contains("\\") || value.contains("\0")
                || !(value.equals("/") || PUBLISH_DIR_PATTERN.matcher(value).matches()))
        {
            throw new ArchiveValidationException(ERROR_PATH_ESCAPES, "invalid publish dir: " + publishDir);
        }
        if (!value.equals("/"))
        {
            for (String segment : value.substring(1).split("/"))
            {
                if (segment.isEmpty() || segment.equals(".") || segment.startsWith("."))
                {
                    throw new ArchiveValidationException(ERROR_PATH_ESCAPES, "invalid publish dir segment: " + publishDir);
                }
            }
        }
        return value.equals("/") ? "" : value.substring(1);
    }

    /** 包可见以便单元测试直接覆盖无法通过 tar 往返保存的字节级名称（反斜杠/盘符/NUL）。 */
    static String normalizeName(String name)
    {
        String value = name == null ? "" : name;
        if (value.contains("\0"))
        {
            throw new ArchiveValidationException(ERROR_PATH_ESCAPES, "entry name contains NUL");
        }
        if (DRIVE_PATTERN.matcher(value).find() || value.contains("\\")
                || value.startsWith("/") || value.startsWith("../"))
        {
            throw new ArchiveValidationException(ERROR_PATH_ESCAPES, "unsafe entry name: " + name);
        }
        while (value.startsWith("./"))
        {
            value = value.substring(2);
        }
        while (value.endsWith("/"))
        {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    private static void rejectParentSegments(String relative)
    {
        for (String segment : relative.split("/"))
        {
            if (segment.equals("..") || segment.equals("."))
            {
                throw new ArchiveValidationException(ERROR_PATH_ESCAPES,
                        "entry contains parent segment: " + relative);
            }
        }
    }

    private static boolean isHidden(String relative)
    {
        for (String segment : relative.split("/"))
        {
            if (segment.startsWith("."))
            {
                return true;
            }
        }
        return false;
    }

    private static Published countPublished(Path root) throws IOException
    {
        Published published = new Published();
        try (var stream = Files.walk(root))
        {
            for (Path path : stream.sorted(Comparator.naturalOrder()).toList())
            {
                if (Files.isRegularFile(path))
                {
                    published.files++;
                    published.bytes += Files.size(path);
                }
            }
        }
        return published;
    }

    private static void applyPosixFilePermissions(Path file)
    {
        try
        {
            Files.setPosixFilePermissions(file, Set.of(
                    PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE,
                    PosixFilePermission.GROUP_READ, PosixFilePermission.OTHERS_READ));
        }
        catch (UnsupportedOperationException | IOException ignored)
        {
            // 非 POSIX 文件系统（如 Windows 开发机）跳过；Linux 上由 CI 验证
        }
    }

    private static void applyPosixDirectoryPermissions(Path dir)
    {
        try
        {
            Files.setPosixFilePermissions(dir, Set.of(
                    PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE,
                    PosixFilePermission.GROUP_READ, PosixFilePermission.GROUP_EXECUTE,
                    PosixFilePermission.OTHERS_READ, PosixFilePermission.OTHERS_EXECUTE));
        }
        catch (UnsupportedOperationException | IOException ignored)
        {
        }
    }

    /** 递归删除目录树；供 Pages 组件清理 staging 与回收 release 使用。 */
    public static void deleteRecursively(Path path) throws IOException
    {
        if (path == null || !Files.exists(path))
        {
            return;
        }
        try (var stream = Files.walk(path))
        {
            for (Path child : stream.sorted(Comparator.reverseOrder()).toList())
            {
                Files.deleteIfExists(child);
            }
        }
    }

    private static class Counters
    {
        int entries;
        int files;
        long bytes;
    }

    private static class Published
    {
        int files;
        long bytes;
    }
}
