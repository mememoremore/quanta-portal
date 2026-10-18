package com.ruoyi.qt.domain.vo;

/**
 * 面包屑节点（不含虚拟分类根时可由前端拼接部门名）
 */
public class QtMaterialFolderBreadcrumbItem
{
    private Long folderId;
    private String folderName;
    private Integer depth;

    public QtMaterialFolderBreadcrumbItem()
    {
    }

    public QtMaterialFolderBreadcrumbItem(Long folderId, String folderName, Integer depth)
    {
        this.folderId = folderId;
        this.folderName = folderName;
        this.depth = depth;
    }

    public Long getFolderId()
    {
        return folderId;
    }

    public void setFolderId(Long folderId)
    {
        this.folderId = folderId;
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
}
