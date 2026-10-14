package com.ruoyi.common.exception.file;

/**
 * 文件上传无效扩展名异常类
 * 
 * @author ruoyi
 */
public class InvalidExtensionException extends FileUploadException
{
    private static final long serialVersionUID = 1L;

    private String[] allowedExtension;
    private String extension;
    private String filename;

    public InvalidExtensionException(String[] allowedExtension, String extension, String filename)
    {
        super(friendlyMessage(allowedExtension, extension));
        this.allowedExtension = allowedExtension;
        this.extension = extension;
        this.filename = filename;
    }

    private static String friendlyMessage(String[] allowedExtension, String extension)
    {
        String shown = (extension == null || extension.isEmpty()) ? "无后缀" : extension;
        if (isPdfOnly(allowedExtension))
        {
            return "不支持的文件格式（" + shown + "），请上传 PDF";
        }
        if (isImageSet(allowedExtension))
        {
            return "不支持的文件格式（" + shown + "），请上传 JPG、PNG 或 WebP";
        }
        return "不支持的文件格式（" + shown + "），请上传 " + joinAllowed(allowedExtension) + " 格式";
    }

    private static boolean isPdfOnly(String[] allowed)
    {
        return allowed != null && allowed.length == 1 && "pdf".equalsIgnoreCase(allowed[0]);
    }

    private static boolean isImageSet(String[] allowed)
    {
        if (allowed == null || allowed.length == 0)
        {
            return false;
        }
        for (String item : allowed)
        {
            if (item == null)
            {
                continue;
            }
            String lower = item.toLowerCase();
            if ("jpg".equals(lower) || "jpeg".equals(lower) || "png".equals(lower) || "webp".equals(lower)
                    || "gif".equals(lower) || "bmp".equals(lower))
            {
                return true;
            }
        }
        return false;
    }

    private static String joinAllowed(String[] allowed)
    {
        if (allowed == null || allowed.length == 0)
        {
            return "指定";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < allowed.length; i++)
        {
            if (i > 0)
            {
                sb.append(i == allowed.length - 1 ? " 或 " : "、");
            }
            sb.append(allowed[i]);
        }
        return sb.toString();
    }

    public String[] getAllowedExtension()
    {
        return allowedExtension;
    }

    public String getExtension()
    {
        return extension;
    }

    public String getFilename()
    {
        return filename;
    }

    public static class InvalidImageExtensionException extends InvalidExtensionException
    {
        private static final long serialVersionUID = 1L;

        public InvalidImageExtensionException(String[] allowedExtension, String extension, String filename)
        {
            super(allowedExtension, extension, filename);
        }
    }

    public static class InvalidFlashExtensionException extends InvalidExtensionException
    {
        private static final long serialVersionUID = 1L;

        public InvalidFlashExtensionException(String[] allowedExtension, String extension, String filename)
        {
            super(allowedExtension, extension, filename);
        }
    }

    public static class InvalidMediaExtensionException extends InvalidExtensionException
    {
        private static final long serialVersionUID = 1L;

        public InvalidMediaExtensionException(String[] allowedExtension, String extension, String filename)
        {
            super(allowedExtension, extension, filename);
        }
    }

    public static class InvalidVideoExtensionException extends InvalidExtensionException
    {
        private static final long serialVersionUID = 1L;

        public InvalidVideoExtensionException(String[] allowedExtension, String extension, String filename)
        {
            super(allowedExtension, extension, filename);
        }
    }
}
