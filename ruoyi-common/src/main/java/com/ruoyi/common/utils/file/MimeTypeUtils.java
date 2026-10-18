package com.ruoyi.common.utils.file;

/**
 * 媒体类型工具类
 * 
 * @author ruoyi
 */
public class MimeTypeUtils
{
    public static final String IMAGE_PNG = "image/png";

    public static final String IMAGE_JPG = "image/jpg";

    public static final String IMAGE_JPEG = "image/jpeg";

    public static final String IMAGE_BMP = "image/bmp";

    public static final String IMAGE_GIF = "image/gif";

    public static final String IMAGE_WEBP = "image/webp";
    
    public static final String[] IMAGE_EXTENSION = { "bmp", "gif", "jpg", "jpeg", "png", "webp" };

    /** 塔员头像：JPG / PNG / WebP */
    public static final String[] AVATAR_IMAGE_EXTENSION = { "jpg", "jpeg", "png", "webp" };

    public static final String[] FLASH_EXTENSION = { "swf", "flv" };

    public static final String[] MEDIA_EXTENSION = { "swf", "flv", "mp3", "wav", "wma", "wmv", "mid", "avi", "mpg",
            "asf", "rm", "rmvb" };

    public static final String[] VIDEO_EXTENSION = { "mp4", "avi", "rmvb" };

    /** 表格导入用 */
    public static final String[] EXCEL_EXTENSION = { "xls", "xlsx" };

    /** 学习资料上传白名单：PDF、Word、PPT、Excel、TXT、Markdown 和常见图片（不含压缩包、视频） */
    public static final String[] MATERIAL_EXTENSION = { "pdf", "doc", "docx", "ppt", "pptx", "xls", "xlsx", "txt", "md",
            "jpg", "jpeg", "png", "webp" };

    /** 论坛帖子图片：仅 PNG/JPEG/WEBP/GIF */
    public static final String[] FORUM_IMAGE_EXTENSION = { "png", "jpg", "jpeg", "gif", "webp" };

    public static final String[] PDF_EXTENSION = { "pdf" };

    public static final String[] DEFAULT_ALLOWED_EXTENSION = {
            // 图片
            "bmp", "gif", "jpg", "jpeg", "png", "webp",
            // word excel powerpoint
            "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt",
            // 压缩文件
            "rar", "zip", "gz", "bz2",
            // 视频格式
            "mp4", "avi", "rmvb",
            // pdf
            "pdf" };

    public static String getExtension(String prefix)
    {
        switch (prefix)
        {
            case IMAGE_PNG:
                return "png";
            case IMAGE_JPG:
                return "jpg";
            case IMAGE_JPEG:
                return "jpeg";
            case IMAGE_BMP:
                return "bmp";
            case IMAGE_GIF:
                return "gif";
            case IMAGE_WEBP:
                return "webp";
            default:
                return "";
        }
    }
}
