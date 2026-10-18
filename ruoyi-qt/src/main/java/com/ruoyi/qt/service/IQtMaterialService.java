package com.ruoyi.qt.service;

import java.util.List;
import com.fasterxml.jackson.databind.JsonNode;
import com.ruoyi.qt.domain.QtMaterial;
import com.ruoyi.qt.domain.vo.QtMaterialBatchDeleteResult;

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

    /**
     * CEO 批量删除。鉴权与参数校验先于任何资料读取；失败整批回滚。
     */
    QtMaterialBatchDeleteResult batchDelete(JsonNode body);

    /**
     * 提交后清理已登记的资料文件。失败只更新清理任务，不回滚已删除的资料记录。
     */
    void purgePendingFileCleanups();

    boolean canView(QtMaterial material);

    boolean canModify(QtMaterial material);
}
