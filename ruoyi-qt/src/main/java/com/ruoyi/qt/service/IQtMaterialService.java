package com.ruoyi.qt.service;

import java.util.List;
import com.ruoyi.qt.domain.QtMaterial;

public interface IQtMaterialService
{
    List<QtMaterial> selectMaterialList(QtMaterial query);

    QtMaterial selectMaterialById(Long materialId);

    int insertMaterial(QtMaterial material);

    /**
     * 落盘前校验分类、可见性与部门绑定，避免校验失败留下孤儿文件。
     */
    void validateUploadPolicy(QtMaterial material);

    int updateMaterial(QtMaterial material);

    int deleteMaterialById(Long materialId);

    boolean canView(QtMaterial material);

    boolean canModify(QtMaterial material);
}
