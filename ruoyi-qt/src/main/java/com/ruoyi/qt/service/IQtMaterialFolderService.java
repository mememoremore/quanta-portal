package com.ruoyi.qt.service;

import java.util.List;
import com.ruoyi.qt.domain.QtMaterial;
import com.ruoyi.qt.domain.QtMaterialFolder;
import com.ruoyi.qt.domain.vo.QtMaterialFolderBreadcrumbItem;
import com.ruoyi.qt.domain.vo.QtMaterialFolderDetailVo;

public interface IQtMaterialFolderService
{
    List<QtMaterialFolder> listFolders(String category, Long parentId);

    QtMaterialFolderDetailVo getFolderDetail(Long folderId);

    QtMaterialFolder createFolder(QtMaterialFolder folder);

    QtMaterialFolder updateFolder(Long folderId, QtMaterialFolder patch);

    void deleteFolder(Long folderId);

    void validateMaterialFolderTarget(QtMaterial material);

    void applyFolderFilter(QtMaterial query, Long folderIdParam);

    void enrichFolderPaths(List<QtMaterial> materials);

    List<QtMaterialFolderBreadcrumbItem> buildBreadcrumb(Long folderId);

    String formatFolderPath(String category, Long folderId);
}
