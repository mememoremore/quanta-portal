package com.ruoyi.qt.controller;

import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.utils.file.FileUploadUtils;
import com.ruoyi.common.utils.file.FileValidator;
import com.ruoyi.common.utils.file.MimeTypeUtils;
import com.ruoyi.framework.config.ServerConfig;
import com.ruoyi.qt.domain.QtForumComment;
import com.ruoyi.qt.domain.QtForumPost;
import com.ruoyi.qt.service.IQtForumService;
import com.ruoyi.qt.util.QtAuthUtils;

/**
 * 社区论坛。所有接口仅限已登录塔员：浏览、发帖、评论；作者可编辑自己的内容，
 * 作者或持有 qt:forum:moderate（或 *:*:*）的塔员可删除。
 */
@RestController
@RequestMapping("/qt/forum")
public class QtForumController extends BaseController
{
    @Autowired
    private IQtForumService qtForumService;

    @Autowired
    private ServerConfig serverConfig;

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/posts")
    public TableDataInfo listPosts(QtForumPost query,
            @RequestParam(value = "scope", required = false) String scope)
    {
        QtAuthUtils.requireQuantaMember();
        startPage();
        return getDataTable(qtForumService.selectPostList(query, "mine".equals(scope)));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/posts/{postId:\\d+}")
    public AjaxResult getPost(@PathVariable Long postId)
    {
        QtAuthUtils.requireQuantaMember();
        return success(qtForumService.viewPost(postId));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me/likes")
    public TableDataInfo likedPosts(QtForumPost query)
    {
        QtAuthUtils.requireQuantaMember();
        // 探测必须早于startPage，避免元数据查询误消耗PageHelper分页上下文。
        qtForumService.checkReactionsAvailable();
        startPage();
        return getDataTable(qtForumService.selectPersonalPostList(query, "liked"));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me/favorites")
    public TableDataInfo favoritePosts(QtForumPost query)
    {
        QtAuthUtils.requireQuantaMember();
        qtForumService.checkReactionsAvailable();
        startPage();
        return getDataTable(qtForumService.selectPersonalPostList(query, "favorites"));
    }

    /** 轻量核对互动，不增加正文浏览量。 */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/posts/{postId:\\d+}/reactions")
    public AjaxResult reactions(@PathVariable Long postId)
    {
        QtAuthUtils.requireQuantaMember();
        return success(qtForumService.getReactions(postId));
    }

    @PreAuthorize("isAuthenticated()")
    @PutMapping("/posts/{postId:\\d+}/like")
    public AjaxResult like(@PathVariable Long postId)
    {
        return changeReaction(postId, "liked", true);
    }

    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/posts/{postId:\\d+}/like")
    public AjaxResult unlike(@PathVariable Long postId)
    {
        return changeReaction(postId, "liked", false);
    }

    @PreAuthorize("isAuthenticated()")
    @PutMapping("/posts/{postId:\\d+}/favorite")
    public AjaxResult favorite(@PathVariable Long postId)
    {
        return changeReaction(postId, "favorites", true);
    }

    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/posts/{postId:\\d+}/favorite")
    public AjaxResult unfavorite(@PathVariable Long postId)
    {
        return changeReaction(postId, "favorites", false);
    }

    private AjaxResult changeReaction(Long postId, String kind, boolean active)
    {
        QtAuthUtils.requireQuantaMember();
        return success(qtForumService.setReaction(postId, kind, active));
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/posts")
    public AjaxResult addPost(@RequestBody QtForumPost body)
    {
        QtAuthUtils.requireQuantaMember();
        return success(qtForumService.insertPost(body));
    }

    @PreAuthorize("isAuthenticated()")
    @PutMapping("/posts/{postId:\\d+}")
    public AjaxResult editPost(@PathVariable Long postId, @RequestBody QtForumPost body)
    {
        QtAuthUtils.requireQuantaMember();
        return success(qtForumService.updatePost(postId, body));
    }

    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/posts/{postId:\\d+}")
    public AjaxResult removePost(@PathVariable Long postId)
    {
        QtAuthUtils.requireQuantaMember();
        qtForumService.deletePostById(postId);
        return success();
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/posts/{postId:\\d+}/comments")
    public TableDataInfo listComments(@PathVariable Long postId)
    {
        QtAuthUtils.requireQuantaMember();
        startPage();
        return getDataTable(qtForumService.selectCommentList(postId));
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/posts/{postId:\\d+}/comments")
    public AjaxResult addComment(@PathVariable Long postId, @RequestBody QtForumComment body)
    {
        QtAuthUtils.requireQuantaMember();
        return success(qtForumService.insertComment(postId, body));
    }

    @PreAuthorize("isAuthenticated()")
    @PutMapping("/comments/{commentId:\\d+}")
    public AjaxResult editComment(@PathVariable Long commentId, @RequestBody QtForumComment body)
    {
        QtAuthUtils.requireQuantaMember();
        return success(qtForumService.updateComment(commentId, body));
    }

    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/comments/{commentId:\\d+}")
    public AjaxResult removeComment(@PathVariable Long commentId)
    {
        QtAuthUtils.requireQuantaMember();
        qtForumService.deleteCommentById(commentId);
        return success();
    }

    /**
     * 帖子图片上传：仅 PNG/JPEG/WEBP/GIF，单张不超过 5 MiB，魔数校验 + UUID 安全文件名。
     * 返回可在普通 <img> 中直接读取的绝对地址（目录走匿名公开前缀）。
     */
    @PreAuthorize("isAuthenticated()")
    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AjaxResult uploadImage(@RequestParam("file") MultipartFile file) throws Exception
    {
        QtAuthUtils.requireQuantaMember();
        if (file == null || file.isEmpty())
        {
            return error("请选择要上传的图片");
        }
        String stored = FileUploadUtils.upload(RuoYiConfig.getUploadPath() + "/qt/forum", file,
                MimeTypeUtils.FORUM_IMAGE_EXTENSION, true, FileValidator.SIZE_IMAGE);
        Map<String, Object> data = new HashMap<>();
        data.put("url", serverConfig.getUrl() + stored);
        return success(data);
    }
}
