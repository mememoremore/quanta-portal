package com.ruoyi.qt.domain;

/**
 * 创建站点请求体。只接受这四个字段；userId/状态等一律取自服务端登录态。
 */
public class QtPageSiteCreateBody
{
    /** 可选；留空由服务端随机分配 */
    private String slug;

    private String repoUrl;

    /** 可选，默认 pages */
    private String branch;

    /** 可选，默认 / */
    private String publishDir;

    public String getSlug()
    {
        return slug;
    }

    public void setSlug(String slug)
    {
        this.slug = slug;
    }

    public String getRepoUrl()
    {
        return repoUrl;
    }

    public void setRepoUrl(String repoUrl)
    {
        this.repoUrl = repoUrl;
    }

    public String getBranch()
    {
        return branch;
    }

    public void setBranch(String branch)
    {
        this.branch = branch;
    }

    public String getPublishDir()
    {
        return publishDir;
    }

    public void setPublishDir(String publishDir)
    {
        this.publishDir = publishDir;
    }
}
