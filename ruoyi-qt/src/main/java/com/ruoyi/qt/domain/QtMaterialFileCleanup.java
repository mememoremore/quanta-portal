package com.ruoyi.qt.domain;

/**
 * 资料删除提交后的文件清理任务。路径只来自资料表，不接受客户端传入。
 */
public class QtMaterialFileCleanup
{
    private Long cleanupId;

    private Long materialId;

    private String filePath;

    private String status;

    private Integer attemptCount;

    private String lastError;

    private String createBy;

    public Long getCleanupId()
    {
        return cleanupId;
    }

    public void setCleanupId(Long cleanupId)
    {
        this.cleanupId = cleanupId;
    }

    public Long getMaterialId()
    {
        return materialId;
    }

    public void setMaterialId(Long materialId)
    {
        this.materialId = materialId;
    }

    public String getFilePath()
    {
        return filePath;
    }

    public void setFilePath(String filePath)
    {
        this.filePath = filePath;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public Integer getAttemptCount()
    {
        return attemptCount;
    }

    public void setAttemptCount(Integer attemptCount)
    {
        this.attemptCount = attemptCount;
    }

    public String getLastError()
    {
        return lastError;
    }

    public void setLastError(String lastError)
    {
        this.lastError = lastError;
    }

    public String getCreateBy()
    {
        return createBy;
    }

    public void setCreateBy(String createBy)
    {
        this.createBy = createBy;
    }
}
