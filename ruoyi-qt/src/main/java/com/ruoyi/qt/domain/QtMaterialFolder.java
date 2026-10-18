package com.ruoyi.qt.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;
import java.util.Date;

/**
 * 学习资料目录 qt_material_folder
 */
public class QtMaterialFolder extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    public static final int MAX_DEPTH = 5;

    private Long folderId;
    private Long parentId;
    private String category;
    private String folderName;
    private Integer depth;

    public Long getFolderId()
    {
        return folderId;
    }

    public void setFolderId(Long folderId)
    {
        this.folderId = folderId;
    }

    public Long getParentId()
    {
        return parentId;
    }

    public void setParentId(Long parentId)
    {
        this.parentId = parentId;
    }

    public String getCategory()
    {
        return category;
    }

    public void setCategory(String category)
    {
        this.category = category;
    }

    public String getFolderName()
    {
        return folderName;
    }

    public void setFolderName(String folderName)
    {
        this.folderName = folderName;
    }

    public Integer getDepth()
    {
        return depth;
    }

    public void setDepth(Integer depth)
    {
        this.depth = depth;
    }

    @Override
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public Date getCreateTime()
    {
        return super.getCreateTime();
    }
}
