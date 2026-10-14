package com.ruoyi.qt.service;

import java.util.List;
import com.ruoyi.qt.domain.QtMaterial;

public interface IQtMaterialService
{
    List<QtMaterial> selectMaterialList(QtMaterial query);

    QtMaterial selectMaterialById(Long materialId);

    int insertMaterial(QtMaterial material);

    int updateMaterial(QtMaterial material);

    int deleteMaterialById(Long materialId);

    boolean canView(QtMaterial material);

    boolean canModify(QtMaterial material);
}
