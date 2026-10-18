package com.ruoyi.qt.domain.vo;

import java.util.ArrayList;
import java.util.List;

/**
 * 批量删除的完整确认。两个数组都必须返回，并集等于请求 ID。
 */
public class QtMaterialBatchDeleteResult
{
    private List<Long> deletedIds = new ArrayList<>();

    private List<Long> alreadyDeletedIds = new ArrayList<>();

    public List<Long> getDeletedIds()
    {
        return deletedIds;
    }

    public void setDeletedIds(List<Long> deletedIds)
    {
        this.deletedIds = deletedIds == null ? new ArrayList<>() : deletedIds;
    }

    public List<Long> getAlreadyDeletedIds()
    {
        return alreadyDeletedIds;
    }

    public void setAlreadyDeletedIds(List<Long> alreadyDeletedIds)
    {
        this.alreadyDeletedIds = alreadyDeletedIds == null ? new ArrayList<>() : alreadyDeletedIds;
    }
}
