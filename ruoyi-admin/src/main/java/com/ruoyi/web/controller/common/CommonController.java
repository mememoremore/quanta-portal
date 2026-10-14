package com.ruoyi.web.controller.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.annotation.RateLimiter;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.LimitType;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.file.FileUploadUtils;
import com.ruoyi.common.utils.file.FileUtils;
import com.ruoyi.common.utils.file.FileValidator;
import com.ruoyi.common.utils.file.MimeTypeUtils;
import com.ruoyi.framework.config.ServerConfig;
import com.ruoyi.framework.security.ProfileAccessSigner;
import com.ruoyi.qt.util.QtAuthUtils;

/**
 * 通用请求处理
 * 
 * @author ruoyi
 */
@RestController
@RequestMapping("/common")
public class CommonController
{
    private static final Logger log = LoggerFactory.getLogger(CommonController.class);

    @Autowired
    private ServerConfig serverConfig;

    @Autowired
    private ProfileAccessSigner profileAccessSigner;

    private static final String FILE_DELIMITER = ",";
    private static final Set<String> ALLOWED_UPLOAD_SUB_DIRS = Set.of("qt/clothing-item", "qt/payment-qr", "qt/payment-proof");

    /** 未指定 subDir 时的档位（富文本编辑器、通用文件上传都走这条） */
    private static final String[] DEFAULT_SUBDIR_EXTENSION = MimeTypeUtils.DEFAULT_ALLOWED_EXTENSION;

    private static final java.util.Map<String, UploadPolicy> UPLOAD_POLICIES = java.util.Map.of(
            "qt/payment-proof", new UploadPolicy(MimeTypeUtils.IMAGE_EXTENSION, FileValidator.SIZE_IMAGE),
            "qt/payment-qr", new UploadPolicy(MimeTypeUtils.IMAGE_EXTENSION, FileValidator.SIZE_IMAGE),
            "qt/clothing-item", new UploadPolicy(MimeTypeUtils.IMAGE_EXTENSION, FileValidator.SIZE_IMAGE));

    private record UploadPolicy(String[] allowedExtension, long maxSize)
    {
    }

    private static UploadPolicy policyOf(String subDir)
    {
        if (StringUtils.isEmpty(subDir))
        {
            return new UploadPolicy(DEFAULT_SUBDIR_EXTENSION, FileValidator.SIZE_DOCUMENT);
        }
        return UPLOAD_POLICIES.getOrDefault(subDir.replace("\\", "/").trim(),
                new UploadPolicy(DEFAULT_SUBDIR_EXTENSION, FileValidator.SIZE_DOCUMENT));
    }

    /**
     * 通用下载请求
     * 
     * @param fileName 文件名称
     * @param delete 是否删除
     */
    @GetMapping("/download")
    public void fileDownload(String fileName, Boolean delete, HttpServletResponse response, HttpServletRequest request)
    {
        try
        {
            if (!FileUtils.checkAllowDownload(fileName))
            {
                throw new Exception(StringUtils.format("文件名称({})非法，不允许下载。 ", fileName));
            }
            String realFileName = System.currentTimeMillis() + fileName.substring(fileName.indexOf("_") + 1);
            String filePath = RuoYiConfig.getDownloadPath() + fileName;

            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
            FileUtils.setAttachmentResponseHeader(response, realFileName);
            FileUtils.writeBytes(filePath, response.getOutputStream());
            if (delete)
            {
                FileUtils.deleteFile(filePath);
            }
        }
        catch (Exception e)
        {
            log.error("下载文件失败", e);
        }
    }

    /**
     * 通用上传请求（单个）
     */
    @PostMapping("/upload")
    @RateLimiter(time = 60, count = 10, limitType = LimitType.USER, key = "rate_limit:upload:")
    public AjaxResult uploadFile(MultipartFile file, String subDir) throws Exception
    {
        try
        {
            assertMemberUpload(subDir);
            UploadPolicy policy = policyOf(subDir);
            // 上传文件路径
            String filePath = resolveUploadPath(subDir);
            // 上传并返回新文件名称
            String fileName = FileUploadUtils.upload(filePath, file, policy.allowedExtension(), policy.maxSize());
            String url = serverConfig.getUrl() + fileName;
            AjaxResult ajax = AjaxResult.success();
            ajax.put("url", url);
            ajax.put("fileName", fileName);
            ajax.put("newFileName", FileUtils.getName(fileName));
            ajax.put("originalFilename", file.getOriginalFilename());
            return ajax;
        }
        catch (Exception e)
        {
            return AjaxResult.error(e.getMessage());
        }
    }

    /**
     * 通用上传请求（多个）
     */
    @PostMapping("/uploads")
    @RateLimiter(time = 60, count = 10, limitType = LimitType.USER, key = "rate_limit:uploads:")
    public AjaxResult uploadFiles(List<MultipartFile> files, String subDir) throws Exception
    {
        try
        {
            assertMemberUpload(subDir);
            UploadPolicy policy = policyOf(subDir);
            // 上传文件路径
            String filePath = resolveUploadPath(subDir);
            List<String> urls = new ArrayList<String>();
            List<String> fileNames = new ArrayList<String>();
            List<String> newFileNames = new ArrayList<String>();
            List<String> originalFilenames = new ArrayList<String>();
            for (MultipartFile file : files)
            {
                // 上传并返回新文件名称
                String fileName = FileUploadUtils.upload(filePath, file, policy.allowedExtension(), policy.maxSize());
                String url = serverConfig.getUrl() + fileName;
                urls.add(url);
                fileNames.add(fileName);
                newFileNames.add(FileUtils.getName(fileName));
                originalFilenames.add(file.getOriginalFilename());
            }
            AjaxResult ajax = AjaxResult.success();
            ajax.put("urls", StringUtils.join(urls, FILE_DELIMITER));
            ajax.put("fileNames", StringUtils.join(fileNames, FILE_DELIMITER));
            ajax.put("newFileNames", StringUtils.join(newFileNames, FILE_DELIMITER));
            ajax.put("originalFilenames", StringUtils.join(originalFilenames, FILE_DELIMITER));
            return ajax;
        }
        catch (Exception e)
        {
            return AjaxResult.error(e.getMessage());
        }
    }

    private void assertMemberUpload(String subDir)
    {
        if (StringUtils.isNotEmpty(subDir))
        {
            QtAuthUtils.requireQuantaMember();
        }
    }

    private String resolveUploadPath(String subDir)
    {
        String basePath = RuoYiConfig.getUploadPath();
        if (StringUtils.isEmpty(subDir))
        {
            return basePath;
        }
        String normalizedSubDir = subDir.replace("\\", "/").trim();
        if (normalizedSubDir.startsWith("/") || normalizedSubDir.contains("..") || !ALLOWED_UPLOAD_SUB_DIRS.contains(normalizedSubDir))
        {
            throw new IllegalArgumentException("非法上传目录");
        }
        return basePath + "/" + normalizedSubDir;
    }

    /**
     * 本地资源通用下载
     */
    @GetMapping("/download/resource")
    public void resourceDownload(String resource, HttpServletRequest request, HttpServletResponse response)
            throws Exception
    {
        try
        {
            if (profileAccessSigner.isSensitiveDownloadResource(resource))
            {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            if (!FileUtils.checkAllowDownload(resource))
            {
                throw new Exception(StringUtils.format("资源文件({})非法，不允许下载。 ", resource));
            }
            // 本地资源路径
            String localPath = RuoYiConfig.getProfile();
            // 数据库资源地址
            String downloadPath = localPath + FileUtils.stripPrefix(resource);
            // 下载名称
            String downloadName = StringUtils.substringAfterLast(downloadPath, "/");
            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
            FileUtils.setAttachmentResponseHeader(response, downloadName);
            FileUtils.writeBytes(downloadPath, response.getOutputStream());
        }
        catch (Exception e)
        {
            log.error("下载文件失败", e);
        }
    }
}
