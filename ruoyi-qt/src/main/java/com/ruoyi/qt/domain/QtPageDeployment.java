package com.ruoyi.qt.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 成员静态站点的一次部署（意图 + 执行记录）。
 *
 * 配置快照（repo/branch/publishDir）在入队时固定，执行期间不读取站点当前值；
 * 同一提交可因手动重试或配置变化产生多条记录，不做 (site_id, commit_sha) 唯一约束。
 */
public class QtPageDeployment extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    public static final String TRIGGER_CREATE = "CREATE";

    public static final String TRIGGER_CONFIG = "CONFIG";

    public static final String TRIGGER_POLL = "POLL";

    public static final String TRIGGER_MANUAL = "MANUAL";

    public static final String TRIGGER_ROLLBACK = "ROLLBACK";

    public static final String STATUS_PENDING = "PENDING";

    public static final String STATUS_RESOLVING = "RESOLVING";

    public static final String STATUS_DOWNLOADING = "DOWNLOADING";

    public static final String STATUS_VALIDATING = "VALIDATING";

    public static final String STATUS_PUBLISHING = "PUBLISHING";

    public static final String STATUS_SUCCESS = "SUCCESS";

    public static final String STATUS_FAILED = "FAILED";

    public static final String STATUS_SUPERSEDED = "SUPERSEDED";

    private Long deploymentId;

    private Long siteId;

    private Long requestVersion;

    private Long configVersion;

    private String repoOwner;

    private String repoName;

    private String branch;

    private String publishDir;

    private String commitSha;

    private String triggerType;

    private String status;

    private Long releaseId;

    private Long previousReleaseId;

    private Integer attemptCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date nextAttemptTime;

    private String errorCode;

    private String errorMessage;

    private String logText;

    private Integer fileCount;

    private Long totalBytes;

    private Boolean releaseAvailable;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date finishedTime;

    public Long getDeploymentId()
    {
        return deploymentId;
    }

    public void setDeploymentId(Long deploymentId)
    {
        this.deploymentId = deploymentId;
    }

    public Long getSiteId()
    {
        return siteId;
    }

    public void setSiteId(Long siteId)
    {
        this.siteId = siteId;
    }

    public Long getRequestVersion()
    {
        return requestVersion;
    }

    public void setRequestVersion(Long requestVersion)
    {
        this.requestVersion = requestVersion;
    }

    public Long getConfigVersion()
    {
        return configVersion;
    }

    public void setConfigVersion(Long configVersion)
    {
        this.configVersion = configVersion;
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

    public String getCommitSha()
    {
        return commitSha;
    }

    public void setCommitSha(String commitSha)
    {
        this.commitSha = commitSha;
    }

    public String getTriggerType()
    {
        return triggerType;
    }

    public void setTriggerType(String triggerType)
    {
        this.triggerType = triggerType;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public Long getReleaseId()
    {
        return releaseId;
    }

    public void setReleaseId(Long releaseId)
    {
        this.releaseId = releaseId;
    }

    public Long getPreviousReleaseId()
    {
        return previousReleaseId;
    }

    public void setPreviousReleaseId(Long previousReleaseId)
    {
        this.previousReleaseId = previousReleaseId;
    }

    public Integer getAttemptCount()
    {
        return attemptCount;
    }

    public void setAttemptCount(Integer attemptCount)
    {
        this.attemptCount = attemptCount;
    }

    public Date getNextAttemptTime()
    {
        return nextAttemptTime;
    }

    public void setNextAttemptTime(Date nextAttemptTime)
    {
        this.nextAttemptTime = nextAttemptTime;
    }

    public String getErrorCode()
    {
        return errorCode;
    }

    public void setErrorCode(String errorCode)
    {
        this.errorCode = errorCode;
    }

    public String getErrorMessage()
    {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage)
    {
        this.errorMessage = errorMessage;
    }

    public String getLogText()
    {
        return logText;
    }

    public void setLogText(String logText)
    {
        this.logText = logText;
    }

    public Integer getFileCount()
    {
        return fileCount;
    }

    public void setFileCount(Integer fileCount)
    {
        this.fileCount = fileCount;
    }

    public Long getTotalBytes()
    {
        return totalBytes;
    }

    public void setTotalBytes(Long totalBytes)
    {
        this.totalBytes = totalBytes;
    }

    public Boolean getReleaseAvailable()
    {
        return releaseAvailable;
    }

    public void setReleaseAvailable(Boolean releaseAvailable)
    {
        this.releaseAvailable = releaseAvailable;
    }

    public Date getFinishedTime()
    {
        return finishedTime;
    }

    public void setFinishedTime(Date finishedTime)
    {
        this.finishedTime = finishedTime;
    }
}
