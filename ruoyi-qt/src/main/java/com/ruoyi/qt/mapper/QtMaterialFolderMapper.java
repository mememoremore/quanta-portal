package com.ruoyi.qt.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.qt.domain.QtMaterialFolder;

public interface QtMaterialFolderMapper
{
    QtMaterialFolder selectFolderById(Long folderId);

    List<QtMaterialFolder> selectFolderList(@Param("category") String category, @Param("parentId") Long parentId);

    QtMaterialFolder selectFolderByParentAndName(@Param("parentId") Long parentId, @Param("category") String category,
            @Param("folderName") String folderName);

    int insertFolder(QtMaterialFolder folder);

    int updateFolder(QtMaterialFolder folder);

    int deleteFolderById(Long folderId);

    int countChildFolders(Long folderId);

    int countMaterialsInFolder(Long folderId);

    int updateFolderDepth(@Param("folderId") Long folderId, @Param("depth") int depth);

    List<Long> selectChildFolderIds(Long parentId);

    List<QtMaterialFolder> selectFoldersByCategory(String category);
}
