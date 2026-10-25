package com.ruoyi.qt.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 成员静态站点（Quanta Pages）。
 *
 * 是否在线由 currentReleaseId 决定；最近一次部署失败不代表旧版本下线。
 */
public class QtPageSite extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    public static final String STATUS_ENABLED = "ENABLED";

    public static final String STATUS_SUSPENDED = "SUSPENDED";

    private Long siteId;

    private Long userId;

    private String slug;

    private String repoUrl;

    private String repoOwner;

    private String repoName;

    private String branch;

    private String publishDir;

    private Long configVersion;

    private Long requestVersion;

    private String lastObservedSha;

    private String desiredSha;

    private String currentSha;

    private Long currentDeploymentId;

    private Long currentReleaseId;

    private String status;

    private String pollEtag;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date nextPollTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastCheckedTime;

    private String lastCheckError;

    public Long getSiteId()
    {
        return siteId;
    }

    public void setSiteId(Long siteId)
    {
        this.siteId = siteId;
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

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

    public String getRepoOwner()
    {
        return repoOwner;
    }

    public void setRepoOwner(String repoOwner)
    {
        this.repoOwner = repoOwner;
    }

    public String getRepoName()
    {
        return repoName;
    }

    public void setRepoName(String repoName)
    {
        this.repoName = repoName;
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

    public Long getConfigVersion()
    {
        return configVersion;
    }

    public void setConfigVersion(Long configVersion)
    {
        this.configVersion = configVersion;
    }

    public Long getRequestVersion()
    {
        return requestVersion;
    }

    public void setRequestVersion(Long requestVersion)
    {
        this.requestVersion = requestVersion;
    }

    public String getLastObservedSha()
    {
        return lastObservedSha;
    }

    public void setLastObservedSha(String lastObservedSha)
    {
        this.lastObservedSha = lastObservedSha;
    }

    public String getDesiredSha()
    {
        return desiredSha;
    }

    public void setDesiredSha(String desiredSha)
    {
        this.desiredSha = desiredSha;
    }

    public String getCurrentSha()
    {
        return currentSha;
    }

    public void setCurrentSha(String currentSha)
    {
        this.currentSha = currentSha;
    }

    public Long getCurrentDeploymentId()
    {
        return currentDeploymentId;
    }

    public void setCurrentDeploymentId(Long currentDeploymentId)
    {
        this.currentDeploymentId = currentDeploymentId;
    }

    public Long getCurrentReleaseId()
    {
        return currentReleaseId;
    }

    public void setCurrentReleaseId(Long currentReleaseId)
    {
        this.currentReleaseId = currentReleaseId;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getPollEtag()
    {
        return pollEtag;
    }

    public void setPollEtag(String pollEtag)
    {
        this.pollEtag = pollEtag;
    }

    public Date getNextPollTime()
    {
        return nextPollTime;
    }

    public void setNextPollTime(Date nextPollTime)
    {
        this.nextPollTime = nextPollTime;
    }

    public Date getLastCheckedTime()
    {
        return lastCheckedTime;
    }

    public void setLastCheckedTime(Date lastCheckedTime)
    {
        this.lastCheckedTime = lastCheckedTime;
    }

    public String getLastCheckError()
    {
        return lastCheckError;
    }

    public void setLastCheckError(String lastCheckError)
    {
        this.lastCheckError = lastCheckError;
    }
}
