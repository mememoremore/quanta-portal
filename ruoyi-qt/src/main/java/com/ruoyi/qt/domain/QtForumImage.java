package com.ruoyi.qt.domain;

/** 仅在服务层使用的图片资源记录，不直接序列化返回客户端。 */
public class QtForumImage
{
    private String imageId;
    private Long ownerId;
    private Long postId;
    private String storagePath;
    private String mediaType;
    public String getImageId() { return imageId; }
    public void setImageId(String value) { imageId = value; }
    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long value) { ownerId = value; }
    public Long getPostId() { return postId; }
    public void setPostId(Long value) { postId = value; }
    public String getStoragePath() { return storagePath; }
    public void setStoragePath(String value) { storagePath = value; }
    public String getMediaType() { return mediaType; }
    public void setMediaType(String value) { mediaType = value; }
}
