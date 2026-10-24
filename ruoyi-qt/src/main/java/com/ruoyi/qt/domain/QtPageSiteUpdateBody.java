package com.ruoyi.qt.domain;

/**
 * 更新站点配置请求体。slug 不在此列，创建后不可修改。
 */
public class QtPageSiteUpdateBody
{
    private String repoUrl;

    /** 可选，默认 pages */
    private String branch;

    /** 可选，默认 / */
    private String publishDir;

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
