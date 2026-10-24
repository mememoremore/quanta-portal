package com.ruoyi.qt.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.file.FileUploadUtils;
import com.ruoyi.common.utils.file.FileValidator;
import com.ruoyi.common.utils.file.MimeTypeUtils;
import com.ruoyi.common.utils.uuid.IdUtils;
import com.ruoyi.qt.domain.QtForumImage;
import com.ruoyi.qt.domain.QtForumPost;
import com.ruoyi.qt.mapper.QtForumImageMapper;
import com.ruoyi.qt.mapper.QtForumPostMapper;
import com.ruoyi.qt.util.QtAuthUtils;
import com.ruoyi.qt.util.QtForumVisibility;

/** 图片只能通过鉴权API访问；不签发可转发的匿名链接，也不抓取远端图片。 */
@Service
public class QtForumImageService
{
    public static final String API_PREFIX = "/qt/forum/images/";
    private static final String DIRECTORY = "upload/qt/forum-private";
    private static final Pattern ID = Pattern.compile("[a-f0-9]{32}");
    private static final Pattern RESOURCE = Pattern.compile("/qt/forum/images/([a-f0-9]{32})(?![a-f0-9])");
    private static final Pattern INLINE_IMAGE = Pattern.compile("!\\[[^\\]\\r\\n]*\\]\\((/qt/forum/images/[a-f0-9]{32})\\)");
    @Autowired private QtForumImageMapper imageMapper;
    @Autowired private QtForumPostMapper postMapper;

    public String upload(MultipartFile file) throws Exception
    {
        QtAuthUtils.requireQuantaMember();
        if (file == null || file.isEmpty()) throw new ServiceException("请选择要上传的图片", 400);
        FileUploadUtils.assertAllowed(file, MimeTypeUtils.FORUM_IMAGE_EXTENSION, FileValidator.SIZE_IMAGE);
        String extension = FileUploadUtils.getExtension(file).toLowerCase(Locale.ROOT);
        String mediaType = Map.of("png", "image/png", "jpg", "image/jpeg", "jpeg", "image/jpeg",
                "webp", "image/webp", "gif", "image/gif").get(extension);
        if (mediaType == null) throw new ServiceException("不支持的图片类型", 400);
        String id = IdUtils.fastSimpleUUID();
        QtForumImage image = new QtForumImage();
        image.setImageId(id);
        image.setOwnerId(SecurityUtils.getUserId());
        image.setStoragePath(DIRECTORY + "/" + id + "." + extension);
        image.setMediaType(mediaType);
        Path filePath = resolve(image);
        Files.createDirectories(filePath.getParent());
        try
        {
            file.transferTo(filePath);
            if (imageMapper.insertImage(image) != 1) throw new IllegalStateException("图片记录写入失败");
        }
        catch (Exception error)
        {
            // 仅清理本次新写入的随机文件，绝不删除已有公开图片。
            try { Files.deleteIfExists(filePath); }
            catch (Exception cleanupError) { error.addSuppressed(cleanupError); }
            throw error;
        }
        return API_PREFIX + id;
    }

    /** 受限帖只接受上传工具生成的规范站内图片语法，拒绝旧公开图、外链和引用式图片。 */
    public void validateContent(String content, String visibility)
    {
        if (!QtForumVisibility.MANAGERS.equals(visibility)) return;
        String rest = INLINE_IMAGE.matcher(content).replaceAll("");
        if (rest.contains("![") || Pattern.compile("<\\s*(?:img|picture|svg|iframe)\\b", Pattern.CASE_INSENSITIVE).matcher(rest).find())
            throw new ServiceException("受限帖子中的图片请使用上传按钮重新上传，不支持公开图片、外链或引用式图片", 400);
    }

    /** 调用方持有帖子行锁/创建事务；按固定顺序锁资源，防止两个草稿抢绑同一张图。 */
    public void bindImages(String content, Long postId)
    {
        for (String id : imageIds(content))
        {
            QtForumImage image = imageMapper.lockImage(id);
            if (image == null || !SecurityUtils.getUserId().equals(image.getOwnerId())
                    || (image.getPostId() != null && !image.getPostId().equals(postId)))
                throw new ServiceException("图片不可用或不属于此帖子，请重新上传", 400);
            if (imageMapper.bindImage(id, postId) != 1) throw new ServiceException("图片关联失败，请重试", 409);
        }
    }

    public QtForumImage readableImage(String id)
    {
        QtAuthUtils.requireQuantaMember();
        if (id == null || !ID.matcher(id).matches()) throw notFound();
        QtForumImage image = imageMapper.selectImage(id);
        if (image == null) throw notFound();
        if (image.getPostId() == null)
        {
            if (!SecurityUtils.getUserId().equals(image.getOwnerId())) throw notFound();
        }
        else
        {
            QtForumPost post = postMapper.selectPostById(image.getPostId());
            QtForumVisibility.requireReadable(post, () -> postMapper.canManagePosts(SecurityUtils.getUserId()));
            if (!imageIds(post.getContent()).contains(id)) throw notFound();
        }
        return image;
    }

    public Path resolve(QtForumImage image)
    {
        Path root = Path.of(RuoYiConfig.getProfile()).toAbsolutePath().normalize();
        Path file = root.resolve(image.getStoragePath()).normalize();
        if (!file.startsWith(root.resolve(DIRECTORY)) || file.equals(root.resolve(DIRECTORY))) throw notFound();
        return file;
    }

    private Set<String> imageIds(String content)
    {
        Set<String> ids = new TreeSet<>();
        Matcher matcher = RESOURCE.matcher(content == null ? "" : content);
        while (matcher.find()) ids.add(matcher.group(1));
        return ids;
    }

    private ServiceException notFound() { return new ServiceException("图片不存在或不可访问", 404); }
}
