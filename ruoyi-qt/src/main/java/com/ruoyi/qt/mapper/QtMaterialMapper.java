package com.ruoyi.qt.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.qt.domain.QtMaterial;

public interface QtMaterialMapper
{
    QtMaterial selectMaterialById(Long materialId);

    List<QtMaterial> selectMaterialList(QtMaterial query);

    int insertMaterial(QtMaterial material);

    int updateMaterial(QtMaterial material);

    int deleteMaterialById(Long materialId);

    List<QtMaterial> selectMaterialsByIdsForUpdate(@Param("ids") List<Long> ids);

    int deleteMaterialsByIds(@Param("ids") List<Long> ids);

    int countVisible(@Param("userId") Long userId, @Param("isMember") String isMember,
            @Param("dept") String dept, @Param("isAdmin") String isAdmin);
}
