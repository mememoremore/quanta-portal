package com.ruoyi.qt.pages;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QtStaticArchiveValidatorTest
{
    @TempDir
    Path tempDir;

    private QtStaticArchiveValidator validator;

    @BeforeEach
    void setUp()
    {
        validator = new QtStaticArchiveValidator(100_000, 10_000, 100, 200);
    }

    @Test
    void extractsGitHubRootAndRequiresRegularIndexHtml()
    throws Exception
    {
        Path archive = tar(builder -> {
            builder.dir("octo-hello-abc123/");
            builder.bytes("octo-hello-abc123/index.html", "<h1>ok</h1>");
            builder.bytes("octo-hello-abc123/assets/app.js", "console.log(1)");
        });
        Path staging = tempDir.resolve("staging");

        QtStaticArchiveValidator.ValidatedArtifact artifact =
                validator.extract(archive, "/", staging);

        assertTrue(Files.exists(staging.resolve("index.html")));
        assertTrue(Files.exists(staging.resolve("assets/app.js")));
        assertFalse(Files.exists(staging.resolve("octo-hello-abc123")));
        assertEquals(2, artifact.fileCount());
        assertEquals(Files.size(staging.resolve("index.html")) + Files.size(staging.resolve("assets/app.js")),
                artifact.totalBytes());
        assertTrue(staging.resolve("index.html").toAbsolutePath().startsWith(staging.toAbsolutePath()));
    }

    @Test
    void missingIndexIsRejected()
    throws Exception
    {
        Path archive = tar(builder -> {
            builder.dir("root/");
            builder.bytes("root/readme.md", "no index here");
        });
        QtStaticArchiveValidator.ArchiveValidationException exception = assertThrows(
                QtStaticArchiveValidator.ArchiveValidationException.class,
                () -> validator.extract(archive, "/", tempDir.resolve("staging")));
        assertEquals("INDEX_MISSING", exception.getErrorCode());
    }

    @Test
    void publishDirSelectsSubtreeButWholeArchiveCountsTowardBudget()
    throws Exception
    {
        Path archive = tar(builder -> {
            builder.dir("root/");
            builder.bytes("root/dist/index.html", "<h1>dist</h1>");
            builder.bytes("root/vendor.bin", "x".repeat(9_000));
        });
        Path staging = tempDir.resolve("staging");
        QtStaticArchiveValidator.ValidatedArtifact artifact =
                validator.extract(archive, "/dist", staging);
        assertTrue(Files.exists(staging.resolve("dist/index.html")));
        assertEquals(1, artifact.fileCount());
        // 发布统计只算发布目录；整份归档超限由下面单独验证
        assertEquals(Files.size(staging.resolve("dist/index.html")), artifact.totalBytes());
    }

    @Test
    void extractedBudgetCoversWholeArchiveOutsidePublishDir()
    throws Exception
    {
        Path archive = tar(builder -> {
            builder.dir("root/");
            builder.bytes("root/dist/index.html", "<h1>dist</h1>");
            builder.bytes("root/vendor.bin", "x".repeat(9_000));
        });
        QtStaticArchiveValidator tight = new QtStaticArchiveValidator(5_000, 10_000, 100, 200);
        QtStaticArchiveValidator.ArchiveValidationException exception = assertThrows(
                QtStaticArchiveValidator.ArchiveValidationException.class,
                () -> tight.extract(archive, "/dist", tempDir.resolve("staging")));
        assertEquals("LIMIT_EXCEEDED", exception.getErrorCode());
    }

    @Test
    void nestedPublishDirSelectsOnlySelectedTemplate()
    throws Exception
    {
        Path archive = tar(builder -> {
            builder.dir("root/");
            builder.bytes("root/README.md", "templates");
            builder.bytes("root/templates/classic/index.html", "<h1>classic</h1>");
            builder.bytes("root/templates/classic/assets/app.js", "console.log('classic')");
            builder.bytes("root/templates/paper/index.html", "<h1>paper</h1>");
        });
        Path staging = tempDir.resolve("staging");

        QtStaticArchiveValidator.ValidatedArtifact artifact =
                validator.extract(archive, "/templates/classic", staging);

        assertTrue(artifact.root().endsWith("templates/classic"));
        assertTrue(Files.exists(artifact.root().resolve("index.html")));
        assertTrue(Files.exists(artifact.root().resolve("assets/app.js")));
        assertFalse(Files.exists(artifact.root().resolve("paper/index.html")));
        // 整份归档仍被解包，兄弟模板留在 staging，但不会进入发布统计
        assertTrue(Files.exists(staging.resolve("templates/paper/index.html")));
        assertEquals(2, artifact.fileCount());
        assertEquals(Files.size(artifact.root().resolve("index.html"))
                + Files.size(artifact.root().resolve("assets/app.js")), artifact.totalBytes());
    }

    @Test
    void nestedPublishDirWithoutIndexIsRejected()
    throws Exception
    {
        Path archive = tar(builder -> {
            builder.dir("root/");
            builder.bytes("root/templates/classic/about.html", "no index");
        });
        QtStaticArchiveValidator.ArchiveValidationException exception = assertThrows(
                QtStaticArchiveValidator.ArchiveValidationException.class,
                () -> validator.extract(archive, "/templates/classic", tempDir.resolve("staging")));
        assertEquals("INDEX_MISSING", exception.getErrorCode());
    }

    @Test
    void missingPublishDirIsRejected()
    throws Exception
    {
        Path archive = tar(builder -> {
            builder.dir("root/");
            builder.bytes("root/index.html", "<h1>ok</h1>");
        });
        QtStaticArchiveValidator.ArchiveValidationException exception = assertThrows(
                QtStaticArchiveValidator.ArchiveValidationException.class,
                () -> validator.extract(archive, "/dist", tempDir.resolve("staging")));
        assertEquals("PUBLISH_DIR_MISSING", exception.getErrorCode());
    }

    @Test
    void rejectsTraversalAbsoluteBackslashAndDrivePaths()
    throws Exception
    {
        List<String> evilNames = List.of(
                "../evil.html",
                "/abs.html",
                "root/sub/../../../escape.html",
                "root/C:evil.html");
        for (String evil : evilNames)
        {
            Path archive = tar(builder -> {
                builder.dir("root/");
                builder.bytes("root/index.html", "<h1>ok</h1>");
                builder.bytes(evil, "nope");
            });
            Path staging = tempDir.resolve("staging-" + Math.abs(evil.hashCode()));
            QtStaticArchiveValidator.ArchiveValidationException exception = assertThrows(
                    QtStaticArchiveValidator.ArchiveValidationException.class,
                    () -> validator.extract(archive, "/", staging),
                    "must reject: " + evil);
            assertEquals("PATH_ESCAPES", exception.getErrorCode(), "must reject: " + evil);
            assertDirectoryEmpty(staging);
        }
    }

    @Test
    void nameNormalizerRejectsByteLevelUnsafeNames()
    {
        // 反斜杠/盘符/NUL 名称无法原样通过 TarArchiveOutputStream 往返，直接校验归一化入口
        for (String evil : List.of("root\\evil.html", "C:\\evil.html", "bad\0.txt", "..\\escape.html"))
        {
            QtStaticArchiveValidator.ArchiveValidationException exception = assertThrows(
                    QtStaticArchiveValidator.ArchiveValidationException.class,
                    () -> QtStaticArchiveValidator.normalizeName(evil),
                    "must reject: " + evil);
            assertEquals("PATH_ESCAPES", exception.getErrorCode(), "must reject: " + evil);
        }
    }

    @Test
    void rejectsPaxLongPathEscape()
    throws Exception
    {
        // > 100 字符的长路径迫使 ustar 写 PAX 扩展头；其中夹带 ../ 逃逸
        String evil = "root/" + "dir/".repeat(30) + "../" + "../".repeat(31) + "escape.html";
        assertTrue(evil.length() > 100);
        Path archive = tar(builder -> {
            builder.dir("root/");
            builder.bytes("root/index.html", "<h1>ok</h1>");
            builder.bytes(evil, "nope");
        });
        QtStaticArchiveValidator.ArchiveValidationException exception = assertThrows(
                QtStaticArchiveValidator.ArchiveValidationException.class,
                () -> validator.extract(archive, "/", tempDir.resolve("staging")));
        assertEquals("PATH_ESCAPES", exception.getErrorCode());
    }

    @Test
    void rejectsLinksDevicesAndFifoEntries()
    throws Exception
    {
        record Special(String name, byte linkFlag) {}
        List<Special> specials = List.of(
                new Special("root/link", TarConstants.LF_SYMLINK),
                new Special("root/hard", TarConstants.LF_LINK),
                new Special("root/pipe", TarConstants.LF_FIFO),
                new Special("root/zero", TarConstants.LF_CHR),
                new Special("root/disk", TarConstants.LF_BLK));
        for (Special special : specials)
        {
            Path archive = tar(builder -> {
                builder.dir("root/");
                builder.bytes("root/index.html", "<h1>ok</h1>");
                builder.special(special.name(), special.linkFlag());
            });
            QtStaticArchiveValidator.ArchiveValidationException exception = assertThrows(
                    QtStaticArchiveValidator.ArchiveValidationException.class,
                    () -> validator.extract(archive, "/", tempDir.resolve("staging-" + special.name().hashCode())),
                    "must reject: " + special.name());
            assertEquals("SPECIAL_ENTRY", exception.getErrorCode(), "must reject: " + special.name());
        }
    }

    @Test
    void duplicatePathsAreRejected()
    throws Exception
    {
        Path archive = tar(builder -> {
            builder.dir("root/");
            builder.bytes("root/index.html", "<h1>ok</h1>");
            builder.bytes("root/dup.txt", "first");
            builder.bytes("root/dup.txt", "second");
        });
        QtStaticArchiveValidator.ArchiveValidationException exception = assertThrows(
                QtStaticArchiveValidator.ArchiveValidationException.class,
                () -> validator.extract(archive, "/", tempDir.resolve("staging")));
        assertEquals("DUPLICATE_PATH", exception.getErrorCode());
    }

    @Test
    void hiddenPathsAreExcludedFromArtifact()
    throws Exception
    {
        Path archive = tar(builder -> {
            builder.dir("root/");
            builder.bytes("root/index.html", "<h1>ok</h1>");
            builder.bytes("root/.env", "SECRET=1");
            builder.bytes("root/.git/config", "[core]");
            builder.bytes("root/.github/workflows/ci.yml", "on: push");
            builder.bytes("root/assets/app.js", "console.log(1)");
        });
        Path staging = tempDir.resolve("staging");
        validator.extract(archive, "/", staging);
        assertFalse(Files.exists(staging.resolve(".env")));
        assertFalse(Files.exists(staging.resolve(".git")));
        assertFalse(Files.exists(staging.resolve(".github")));
        assertTrue(Files.exists(staging.resolve("index.html")));
        assertTrue(Files.exists(staging.resolve("assets/app.js")));
    }

    @Test
    void enforcesFileCountEntryCountAndSingleFileLimits()
    throws Exception
    {
        Path tooManyFiles = tar(builder -> {
            builder.dir("root/");
            builder.bytes("root/index.html", "x");
            builder.bytes("root/a.txt", "x");
            builder.bytes("root/b.txt", "x");
        });
        QtStaticArchiveValidator fileLimit = new QtStaticArchiveValidator(100_000, 10_000, 2, 200);
        QtStaticArchiveValidator.ArchiveValidationException files = assertThrows(
                QtStaticArchiveValidator.ArchiveValidationException.class,
                () -> fileLimit.extract(tooManyFiles, "/", tempDir.resolve("s1")));
        assertEquals("LIMIT_EXCEEDED", files.getErrorCode());

        Path tooManyEntries = tar(builder -> {
            builder.dir("root/");
            builder.dir("root/a/");
            builder.dir("root/a/b/");
            builder.dir("root/a/b/c/");
            builder.bytes("root/a/b/c/index.html", "x");
        });
        QtStaticArchiveValidator entryLimit = new QtStaticArchiveValidator(100_000, 10_000, 100, 3);
        QtStaticArchiveValidator.ArchiveValidationException entries = assertThrows(
                QtStaticArchiveValidator.ArchiveValidationException.class,
                () -> entryLimit.extract(tooManyEntries, "/", tempDir.resolve("s2")));
        assertEquals("LIMIT_EXCEEDED", entries.getErrorCode());

        Path bigFile = tar(builder -> {
            builder.dir("root/");
            builder.bytes("root/index.html", "y".repeat(20_000));
        });
        QtStaticArchiveValidator sizeLimit = new QtStaticArchiveValidator(100_000, 10_000, 100, 200);
        QtStaticArchiveValidator.ArchiveValidationException size = assertThrows(
                QtStaticArchiveValidator.ArchiveValidationException.class,
                () -> sizeLimit.extract(bigFile, "/", tempDir.resolve("s3")));
        assertEquals("LIMIT_EXCEEDED", size.getErrorCode());
    }

    @Test
    void corruptArchiveIsRejected()
    throws Exception
    {
        Path archive = tempDir.resolve("corrupt.tgz");
        Files.writeString(archive, "this is not a gzip stream at all");
        QtStaticArchiveValidator.ArchiveValidationException exception = assertThrows(
                QtStaticArchiveValidator.ArchiveValidationException.class,
                () -> validator.extract(archive, "/", tempDir.resolve("staging")));
        assertEquals("CORRUPT_ARCHIVE", exception.getErrorCode());
    }

    private static void assertDirectoryEmpty(Path path)
    {
        if (Files.exists(path))
        {
            try (var stream = Files.list(path))
            {
                assertEquals(0, stream.count(), "staging must stay empty on rejection");
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        }
    }

    @FunctionalInterface
    interface TarConfigurator
    {
        void configure(TarBuilder builder) throws IOException;
    }

    private Path tar(TarConfigurator configurer) throws IOException
    {
        Path archive = tempDir.resolve("fixture-" + System.nanoTime() + ".tar.gz");
        try (TarArchiveOutputStream out = new TarArchiveOutputStream(
                new java.util.zip.GZIPOutputStream(Files.newOutputStream(archive)), "UTF-8"))
        {
            out.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
            out.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_POSIX);
            TarBuilder builder = new TarBuilder(out);
            configurer.configure(builder);
        }
        return archive;
    }

    private static class TarBuilder
    {
        private final TarArchiveOutputStream out;

        TarBuilder(TarArchiveOutputStream out)
        {
            this.out = out;
        }

        void dir(String name) throws IOException
        {
            TarArchiveEntry entry = new TarArchiveEntry(name.endsWith("/") ? name : name + "/");
            out.putArchiveEntry(entry);
            out.closeArchiveEntry();
        }

        void bytes(String name, String content) throws IOException
        {
            byte[] data = content.getBytes(StandardCharsets.UTF_8);
            TarArchiveEntry entry = new TarArchiveEntry(name);
            entry.setSize(data.length);
            out.putArchiveEntry(entry);
            out.write(data);
            out.closeArchiveEntry();
        }

        void special(String name, byte linkFlag) throws IOException
        {
            TarArchiveEntry entry = new TarArchiveEntry(name, linkFlag, false);
            if (linkFlag == TarConstants.LF_SYMLINK || linkFlag == TarConstants.LF_LINK)
            {
                entry.setLinkName("elsewhere.html");
            }
            out.putArchiveEntry(entry);
            out.closeArchiveEntry();
        }
    }
}
