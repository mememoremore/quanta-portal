package com.ruoyi.common.utils.file;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Locale;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.exception.file.InvalidExtensionException;
import com.ruoyi.common.utils.StringUtils;

/**
 * 文件内容校验：大小档位、魔数（真实类型）。不限制图片宽高。
 * <p>
 * 只看文件后缀是可绕过的（把 html 改名成 png 即可上传成功），因此这里统一以文件头字节为准，
 * 并要求后缀与真实类型一致。
 *
 * @author Quanta
 */
public class FileValidator
{
    /** 图片类档位：证件照 / 头像 / 收款码 / 效果图 / 支付凭证 */
    public static final long SIZE_IMAGE = 5L * 1024 * 1024;

    /** 文档类档位：学习资料（pdf / word / excel / ppt / 压缩包） */
    public static final long SIZE_DOCUMENT = 20L * 1024 * 1024;

    /** 招新证件照：微信原图常见 3～8MB */
    public static final long SIZE_INTERVIEW_PHOTO = 10L * 1024 * 1024;

    /** 招新 PDF 简历 */
    public static final long SIZE_RESUME = 10L * 1024 * 1024;

    /** 表格导入档位 */
    public static final long SIZE_IMPORT = 5L * 1024 * 1024;

    /**
     * 校验大小与真实类型。
     *
     * @param file 上传文件
     * @param allowedExtension 允许的后缀白名单
     * @param maxSize 该场景的字节上限
     * @throws InvalidExtensionException 类型/内容不合法
     */
    public static void validate(MultipartFile file, String[] allowedExtension, long maxSize)
            throws InvalidExtensionException
    {
        if (file == null || file.isEmpty())
        {
            throw new ServiceException("上传内容为空");
        }
        if (file.getSize() > maxSize)
        {
            throw new ServiceException(StringUtils.format("文件大小超出限制，最大允许 {}MB", maxSize / 1024 / 1024));
        }

        String extension = FileUploadUtils.getExtension(file);
        if (StringUtils.isEmpty(extension))
        {
            throw new ServiceException("无法识别文件类型，请确认文件带有正确的扩展名");
        }
        String lower = extension.toLowerCase(Locale.ROOT);
        if (!FileUploadUtils.isAllowedExtension(lower, allowedExtension))
        {
            throw new InvalidExtensionException(allowedExtension, extension, file.getOriginalFilename());
        }

        byte[] head = readHead(file, 16);
        if (!matchesKind(lower, head, file.getContentType()))
        {
            throw new ServiceException(StringUtils.format("文件内容不是合法的 {} 格式（疑似改名文件），已拒绝上传",
                    lower.toUpperCase(Locale.ROOT)));
        }
    }

    /**
     * 只校验内容与后缀一致，不做大小与白名单判断。
     * 供 {@link FileUploadUtils#assertAllowed} 在其自身校验之后调用。
     */
    public static void validateContent(MultipartFile file, String[] allowedExtension)
    {
        String extension = FileUploadUtils.getExtension(file);
        if (StringUtils.isEmpty(extension))
        {
            throw new ServiceException("无法识别文件类型，请确认文件带有正确的扩展名");
        }
        String lower = extension.toLowerCase(Locale.ROOT);
        if (allowedExtension != null && !FileUploadUtils.isAllowedExtension(lower, allowedExtension))
        {
            return;
        }
        byte[] head = readHead(file, 16);
        if (!matchesKind(lower, head, file.getContentType()))
        {
            throw new ServiceException(StringUtils.format("文件内容不是合法的 {} 格式（疑似改名文件），已拒绝上传",
                    lower.toUpperCase(Locale.ROOT)));
        }
    }

    /** 仅校验大小（供已在别处做类型校验的入口复用） */
    public static void assertSize(MultipartFile file, long maxSize)
    {
        if (file != null && file.getSize() > maxSize)
        {
            throw new ServiceException(StringUtils.format("文件大小超出限制，最大允许 {}MB", maxSize / 1024 / 1024));
        }
    }

    private static boolean isAnyAllowedImage(byte[] head)
    {
        return isJpeg(head) || isPng(head) || isGif(head) || isBmp(head) || isWebp(head) || isHeic(head);
    }

    private static boolean isJpeg(byte[] head)
    {
        return startsWith(head, 0xFF, 0xD8, 0xFF);
    }

    private static boolean isPng(byte[] head)
    {
        return startsWith(head, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A);
    }

    private static boolean isGif(byte[] head)
    {
        return text(head, 0, "GIF8");
    }

    private static boolean isBmp(byte[] head)
    {
        return text(head, 0, "BM");
    }

    private static boolean isWebp(byte[] head)
    {
        return text(head, 0, "RIFF") && text(head, 8, "WEBP");
    }

    private static boolean isHeic(byte[] head)
    {
        if (!text(head, 4, "ftyp") || head.length < 12)
        {
            return false;
        }
        String brand = new String(head, 8, Math.min(4, head.length - 8), java.nio.charset.StandardCharsets.US_ASCII)
                .toLowerCase(Locale.ROOT);
        return brand.startsWith("hei") || "mif1".equals(brand) || "msf1".equals(brand);
    }

    /**
     * 按后缀比对文件头字节。
     */
    private static boolean matchesKind(String extension, byte[] head, String contentType)
    {
        switch (extension)
        {
            case "jpg":
            case "jpeg":
            case "png":
            case "gif":
            case "bmp":
            case "webp":
                // 微信会把相册图转成 WebP/HEIC 仍带 .jpg 后缀，按真实文件头放行即可
                return isAnyAllowedImage(head);
            case "pdf":
                return text(head, 0, "%PDF");
            // 压缩容器：docx/xlsx/pptx 本质是 zip
            case "zip":
            case "docx":
            case "xlsx":
            case "pptx":
                return startsWith(head, 0x50, 0x4B, 0x03, 0x04) || startsWith(head, 0x50, 0x4B, 0x05, 0x06)
                        || startsWith(head, 0x50, 0x4B, 0x07, 0x08);
            case "rar":
                return text(head, 0, "Rar!");
            case "gz":
            case "tgz":
                return startsWith(head, 0x1F, 0x8B);
            case "bz2":
                return text(head, 0, "BZh");
            // 老式 OLE 复合文档：doc/xls/ppt
            case "doc":
            case "xls":
            case "ppt":
                return startsWith(head, 0xD0, 0xCF, 0x11, 0xE0, 0xA1, 0xB1, 0x1A, 0xE1);
            case "mp4":
            case "avi":
            case "rm":
            case "rmvb":
            case "mp3":
            case "wav":
            case "wma":
            case "wmv":
            case "mid":
            case "asf":
            case "swf":
            case "flv":
                return matchesMedia(head, contentType);
            case "txt":
                return looksLikeText(head, contentType);
            default:
                return false;
        }
    }

    private static boolean matchesMedia(byte[] head, String contentType)
    {
        if (text(head, 0, "RIFF") || text(head, 4, "ftyp") || text(head, 0, ".RMF") || text(head, 0, "ID3")
                || text(head, 0, "FLV") || text(head, 0, "CWS") || text(head, 0, "FWS") || text(head, 0, "ZWS"))
        {
            return true;
        }
        return contentType != null && contentType.toLowerCase(Locale.ROOT).startsWith("video/");
    }

    /**
     * txt 没有文件头签名，因此要求声明为 text/*，且前 16 字节内不出现 NUL（二进制特征）。
     */
    private static boolean looksLikeText(byte[] head, String contentType)
    {
        for (byte b : head)
        {
            if (b == 0x00)
            {
                return false;
            }
        }
        return contentType == null || contentType.toLowerCase(Locale.ROOT).startsWith("text/")
                || "application/octet-stream".equalsIgnoreCase(contentType);
    }

    private static byte[] readHead(MultipartFile file, int len)
    {
        byte[] buf = new byte[len];
        try (InputStream in = file.getInputStream())
        {
            int read = 0;
            while (read < len)
            {
                int n = in.read(buf, read, len - read);
                if (n < 0)
                {
                    break;
                }
                read += n;
            }
            return read < len ? Arrays.copyOf(buf, read) : buf;
        }
        catch (Exception e)
        {
            return new byte[0];
        }
    }

    private static boolean startsWith(byte[] head, int... signature)
    {
        if (head.length < signature.length)
        {
            return false;
        }
        for (int i = 0; i < signature.length; i++)
        {
            if ((head[i] & 0xFF) != (signature[i] & 0xFF))
            {
                return false;
            }
        }
        return true;
    }

    private static boolean text(byte[] head, int offset, String expected)
    {
        if (head.length < offset + expected.length())
        {
            return false;
        }
        for (int i = 0; i < expected.length(); i++)
        {
            if ((char) (head[offset + i] & 0xFF) != expected.charAt(i))
            {
                return false;
            }
        }
        return true;
    }
}
