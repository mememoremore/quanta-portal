package com.ruoyi.qt.domain.vo;

import java.util.List;
import com.ruoyi.qt.domain.QtMaterialFolder;

/**
 * 目录详情与面包屑
 */
public class QtMaterialFolderDetailVo
{
    private QtMaterialFolder folder;
    private List<QtMaterialFolderBreadcrumbItem> breadcrumb;

    public QtMaterialFolder getFolder()
    {
        return folder;
    }

    public void setFolder(QtMaterialFolder folder)
    {
        this.folder = folder;
    }

    public List<QtMaterialFolderBreadcrumbItem> getBreadcrumb()
    {
        return breadcrumb;
    }

    public void setBreadcrumb(List<QtMaterialFolderBreadcrumbItem> breadcrumb)
    {
        this.breadcrumb = breadcrumb;
    }
}
