package com.ruoyi.qt.pages;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.ruoyi.qt.domain.QtPageSite;

/**
 * Pages 站点的磁盘布局与实体版本操作。
 *
 * 目录布局：{root}/{slug}/.deploy.lock、.staging/{deploymentId}/、releases/{releaseId}/、
 * current -> releases/{releaseId}（相对符号链接）。current 只能由 switchCurrent 以
 * 同目录 ATOMIC_MOVE + REPLACE_EXISTING 生成；不支持原子移动时直接失败，绝不先删后建。
 * slug 由服务端创建时校验，不接收客户端完整路径。
 */
public class QtPageReleaseStore
{
    /** 每站磁盘配额超限：保护引用无法回收时拒绝新部署，绝不删除正在使用的目录 */
    public static class QuotaExceededException extends RuntimeException
    {
        private static final long serialVersionUID = 1L;

        public QuotaExceededException(String message)
        {
            super(message);
        }
    }

    /** 回滚目标实体目录已不存在 */
    public static class ReleaseMissingException extends RuntimeException
    {
        private static final long serialVersionUID = 1L;

        public ReleaseMissingException(String message)
        {
            super(message);
        }
    }

    private static final String LOCK_FILE = ".deploy.lock";

    private static final String STAGING_DIR = ".staging";

    private static final String RELEASES_DIR = "releases";

    private static final String CURRENT_LINK = "current";

    private static final Pattern RELEASE_ID_PATTERN = Pattern.compile("^(\\d+)$");

    private final Path sitesRoot;

    public QtPageReleaseStore(Path sitesRoot)
    {
        this.sitesRoot = sitesRoot.toAbsolutePath().normalize();
    }

    /**
     * 站点级进程间文件锁；部署、回滚、清理与恢复都必须在锁内执行。
     * 拿不到锁返回 empty（其他实例正在处理该站点）。
     */
    public Optional<SiteLock> tryLock(QtPageSite site) throws IOException
    {
        Path siteDir = siteDirectory(site);
        Files.createDirectories(siteDir);
        FileChannel channel = FileChannel.open(siteDir.resolve(LOCK_FILE),
                StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE);
        try
        {
            FileLock lock = channel.tryLock();
            if (lock == null)
            {
                channel.close();
                return Optional.empty();
            }
            return Optional.of(new SiteLock(channel, lock));
        }
        catch (OverlappingFileLockException e)
        {
            channel.close();
            return Optional.empty();
        }
        catch (IOException e)
        {
            channel.close();
            throw e;
        }
    }

    /**
     * 把验证完成的 staging 发布目录固化为 releases/{releaseId}（同文件系统原子移动）。
     */
    public Path prepare(QtPageSite site, long releaseId, QtStaticArchiveValidator.ValidatedArtifact artifact)
    throws IOException
    {
        Path source = artifact.root().toAbsolutePath().normalize();
        Path release = releaseDirectory(site, releaseId);
        Files.createDirectories(release.getParent());
        if (Files.exists(release))
        {
            throw new IOException("release already exists: " + releaseId);
        }
        try
        {
            Files.move(source, release, StandardCopyOption.ATOMIC_MOVE);
        }
        catch (AtomicMoveNotSupportedException e)
        {
            Files.move(source, release, StandardCopyOption.REPLACE_EXISTING);
        }
        // 清掉本次部署在 .staging 下残留的外壳（发布根已整体移入 releases）
        deleteQuietly(source.getParent());
        return release;
    }

    /**
     * 读取 current 实际指向的平台版本号；未上线或链接不是平台生成的 releases/{id} 时返回 null。
     */
    public Long readCurrentRelease(QtPageSite site) throws IOException
    {
        Path current = currentLink(site);
        if (!Files.isSymbolicLink(current))
        {
            return null;
        }
        Path target = Files.readSymbolicLink(current);
        Matcher matcher = RELEASE_ID_PATTERN.matcher(target.toString());
        if (!matcher.matches())
        {
            return null;
        }
        Path resolved = current.getParent().resolve(target).normalize();
        if (!resolved.startsWith(releaseDirectory(site, 0).getParent()) || !Files.isDirectory(resolved))
        {
            return null;
        }
        return Long.parseLong(matcher.group(1));
    }

    /**
     * 返回 current 链接路径（不要求存在）；恢复协议用它判断真实指向。
     */
    public Path currentLink(QtPageSite site)
    {
        return siteDirectory(site).resolve(CURRENT_LINK);
    }

    public Path siteDirectory(QtPageSite site)
    {
        return sitesRoot.resolve(site.getSlug());
    }

    public Path stagingRoot(QtPageSite site)
    {
        return siteDirectory(site).resolve(STAGING_DIR);
    }

    public Path stagingDirectory(QtPageSite site, long deploymentId)
    {
        return stagingRoot(site).resolve(Long.toString(deploymentId));
    }

    public Path releaseDirectory(QtPageSite site, long releaseId)
    {
        return siteDirectory(site).resolve(RELEASES_DIR).resolve(Long.toString(releaseId));
    }

    /**
     * 原子切换 current：先在同目录创建临时相对符号链接，再 ATOMIC_MOVE+REPLACE_EXISTING。
     */
    public void switchCurrent(QtPageSite site, long releaseId) throws IOException
    {
        Path siteDir = siteDirectory(site);
        Path release = releaseDirectory(site, releaseId);
        if (!Files.isDirectory(release))
        {
            throw new IOException("release not found: " + releaseId);
        }
        Path temp = siteDir.resolve(".current.tmp." + Long.toHexString(System.nanoTime())
                + "." + Long.toHexString(Thread.currentThread().getId()));
        Files.deleteIfExists(temp);
        try
        {
            Files.createSymbolicLink(temp, Path.of(RELEASES_DIR, Long.toString(releaseId)));
            Files.move(temp, siteDir.resolve(CURRENT_LINK),
                    StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        }
        catch (AtomicMoveNotSupportedException e)
        {
            Files.deleteIfExists(temp);
            throw new IOException("atomic rename of current link not supported on this filesystem", e);
        }
        catch (IOException e)
        {
            Files.deleteIfExists(temp);
            throw e;
        }
    }

    /**
     * 删除不受保护的 release 与全部 staging 残留；调用方必须持有站点文件锁，
     * 并已在站点行锁下确认保护集合（当前版本 + 待执行回滚引用 + 保留策略内的版本）。
     */
    public void prune(QtPageSite site, Set<Long> protectedReleaseIds) throws IOException
    {
        Path stagingRoot = stagingRoot(site);
        if (Files.isDirectory(stagingRoot))
        {
            QtStaticArchiveValidator.deleteRecursively(stagingRoot);
        }
        Path releases = siteDirectory(site).resolve(RELEASES_DIR);
        if (!Files.isDirectory(releases))
        {
            return;
        }
        try (var stream = Files.list(releases))
        {
            for (Path child : stream.toList())
            {
                String name = child.getFileName().toString();
                Matcher matcher = RELEASE_ID_PATTERN.matcher(name);
                if (!matcher.matches() || protectedReleaseIds.contains(Long.parseLong(matcher.group(1))))
                {
                    continue;
                }
                QtStaticArchiveValidator.deleteRecursively(child);
            }
        }
    }

    /** 目录磁盘占用（含子目录），用于每站预算检查。 */
    public long directorySize(Path directory) throws IOException
    {
        if (directory == null || !Files.exists(directory))
        {
            return 0L;
        }
        try (var stream = Files.walk(directory))
        {
            return stream.filter(Files::isRegularFile).mapToLong(path -> {
                try
                {
                    return Files.size(path);
                }
                catch (IOException e)
                {
                    return 0L;
                }
            }).sum();
        }
    }

    /** 站点目录总占用（含 .staging 临时文件与全部 release）。 */
    public long siteUsageBytes(QtPageSite site) throws IOException
    {
        return directorySize(siteDirectory(site));
    }

    /**
     * 列出不受保护的实体版本目录（调用方持有站点文件锁与行锁）。
     * 只认数字命名目录；不在保护集合内的即为回收候选。
     */
    public java.util.List<Long> listDeletableReleases(QtPageSite site, Set<Long> protectedReleaseIds)
    throws IOException
    {
        java.util.List<Long> deletable = new java.util.ArrayList<>();
        Path releases = siteDirectory(site).resolve(RELEASES_DIR);
        if (!Files.isDirectory(releases))
        {
            return deletable;
        }
        try (var stream = Files.list(releases))
        {
            for (Path child : stream.toList())
            {
                Matcher matcher = RELEASE_ID_PATTERN.matcher(child.getFileName().toString());
                if (!matcher.matches())
                {
                    continue;
                }
                long releaseId = Long.parseLong(matcher.group(1));
                if (!protectedReleaseIds.contains(releaseId))
                {
                    deletable.add(releaseId);
                }
            }
        }
        deletable.sort(java.util.Comparator.naturalOrder());
        return deletable;
    }

    private static void deleteQuietly(Path path)
    {
        if (path == null)
        {
            return;
        }
        try
        {
            QtStaticArchiveValidator.deleteRecursively(path);
        }
        catch (IOException ignored)
        {
        }
    }

    /**
     * 站点文件锁句柄。
     */
    public static final class SiteLock implements AutoCloseable
    {
        private final FileChannel channel;

        private final FileLock lock;

        private SiteLock(FileChannel channel, FileLock lock)
        {
            this.channel = channel;
            this.lock = lock;
        }

        public boolean isValid()
        {
            return lock != null && lock.isValid();
        }

        @Override
        public void close()
        {
            try
            {
                lock.release();
            }
            catch (IOException ignored)
            {
            }
            finally
            {
                try
                {
                    channel.close();
                }
                catch (IOException ignored)
                {
                }
            }
        }
    }
}
