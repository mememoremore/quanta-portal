package com.ruoyi.qt.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.nio.file.Path;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.qt.domain.QtMaterial;
import com.ruoyi.qt.domain.QtMaterialFileCleanup;
import com.ruoyi.qt.domain.vo.QtMaterialBatchDeleteResult;
import com.ruoyi.qt.mapper.QtMaterialFileCleanupMapper;
import com.ruoyi.qt.mapper.QtMaterialMapper;
import com.ruoyi.qt.service.IQtMaterialService;
import com.ruoyi.qt.util.QtAuthUtils;
import com.ruoyi.qt.util.QtDictUtils;
import com.ruoyi.qt.util.QtLabMemberImportParser;
import com.ruoyi.qt.util.QtMaterialStoragePaths;

@Service
public class QtMaterialServiceImpl implements IQtMaterialService
{
    private static final long JS_MAX_SAFE_INTEGER = 9007199254740991L;

    @Autowired
    private QtMaterialMapper qtMaterialMapper;

    @Autowired
    private QtMaterialFileCleanupMapper qtMaterialFileCleanupMapper;

    @Lazy
    @Autowired
    private com.ruoyi.qt.service.IQtMaterialFolderService qtMaterialFolderService;

    @Override
    public List<QtMaterial> selectMaterialList(QtMaterial query)
    {
        applyVisibilityParams(query);
        return qtMaterialMapper.selectMaterialList(query);
    }

    @Override
    public QtMaterial selectMaterialById(Long materialId)
    {
        QtMaterial material = qtMaterialMapper.selectMaterialById(materialId);
        if (material == null)
        {
            throw new ServiceException("资料不存在");
        }
        if (!canView(material))
        {
            throw new ServiceException("无权查看该资料");
        }
        return material;
    }

    @Override
    public int insertMaterial(QtMaterial material)
    {
        validateUploadPolicy(material);
        if (material.getFolderId() == null)
        {
            material.setFolderId(0L);
        }
        qtMaterialFolderService.validateMaterialFolderTarget(material);
        return qtMaterialMapper.insertMaterial(material);
    }

    /**
     * 新上传资料的业务规则：仅四个标准分类、仅塔员共享、分类与上传者部门绑定。
     * CEO / 超管可跨部门；普通账号先把成员部门归一为 FRONTEND 等标准码再比较。
     */
    @Override
    public void validateUploadPolicy(QtMaterial material)
    {
        String category = material.getCategory();
        if (StringUtils.isEmpty(category))
        {
            throw new ServiceException("请选择资料分类", com.ruoyi.common.constant.HttpStatus.BAD_REQUEST);
        }
        if (!QtMaterial.isStandardCategory(category))
        {
            throw new ServiceException("资料分类仅支持 FRONTEND/BACKEND/PRODUCT/DESIGN",
                    com.ruoyi.common.constant.HttpStatus.BAD_REQUEST);
        }
        if (StringUtils.isNotEmpty(material.getVisibility()) && !"MEMBER".equals(material.getVisibility()))
        {
            throw new ServiceException("学习资料仅支持塔员共享（visibility=MEMBER）",
                    com.ruoyi.common.constant.HttpStatus.BAD_REQUEST);
        }
        material.setVisibility("MEMBER");
        boolean unrestricted = QtAuthUtils.isCeo() || QtAuthUtils.hasPermi("*:*:*");
        if (unrestricted)
        {
            return;
        }
        String department = QtLabMemberImportParser.normalizeDepartment(QtAuthUtils.currentDepartment());
        if (StringUtils.isEmpty(department) || !QtMaterial.isStandardCategory(department))
        {
            throw new ServiceException("当前账号未绑定成员部门，无法上传学习资料",
                    com.ruoyi.common.constant.HttpStatus.FORBIDDEN);
        }
        if (!department.equals(category))
        {
            throw new ServiceException("只能上传本部门分类（" + department + "）的学习资料",
                    com.ruoyi.common.constant.HttpStatus.FORBIDDEN);
        }
    }

    @Override
    public int updateMaterial(QtMaterial material)
    {
        QtMaterial old = selectMaterialById(material.getMaterialId());
        if (!canModify(old))
        {
            throw new ServiceException("无权修改该资料");
        }
        if (StringUtils.isNotEmpty(material.getVisibility()))
        {
            QtDictUtils.requireValue(QtDictUtils.MATERIAL_VISIBILITY, material.getVisibility(), "visibility");
        }
        if (material.getFolderId() != null)
        {
            QtMaterial probe = new QtMaterial();
            probe.setCategory(StringUtils.isNotEmpty(material.getCategory()) ? material.getCategory() : old.getCategory());
            probe.setFolderId(material.getFolderId());
            boolean unrestricted = QtAuthUtils.isCeo() || QtAuthUtils.hasPermi("*:*:*");
            if (unrestricted && material.getFolderId() > 0)
            {
                probe.setCategory(qtMaterialFolderService.getFolderDetail(material.getFolderId()).getFolder().getCategory());
                material.setCategory(probe.getCategory());
            }
            qtMaterialFolderService.validateMaterialFolderTarget(probe);
            material.setFolderId(probe.getFolderId());
        }
        return qtMaterialMapper.updateMaterial(material);
    }

    @Override
    public int deleteMaterialById(Long materialId)
    {
        QtMaterial old = selectMaterialById(materialId);
        if (!canModify(old))
        {
            throw new ServiceException("无权删除该资料");
        }
        return qtMaterialMapper.deleteMaterialById(materialId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QtMaterialBatchDeleteResult batchDelete(JsonNode body)
    {
        if (!QtAuthUtils.canBatchDeleteMaterials())
        {
            throw new ServiceException("仅 CEO 可执行批量删除", HttpStatus.FORBIDDEN);
        }
        List<Long> ids = parseBatchIds(body);
        List<QtMaterial> locked = qtMaterialMapper.selectMaterialsByIdsForUpdate(ids);
        Map<Long, QtMaterial> byId = new HashMap<>();
        if (locked != null)
        {
            for (QtMaterial row : locked)
            {
                if (row != null && row.getMaterialId() != null)
                {
                    byId.put(row.getMaterialId(), row);
                }
            }
        }
        List<Long> alreadyDeletedIds = new ArrayList<>();
        List<QtMaterial> found = new ArrayList<>();
        for (Long id : ids)
        {
            QtMaterial row = byId.get(id);
            if (row == null)
            {
                alreadyDeletedIds.add(id);
            }
            else
            {
                found.add(row);
            }
        }
        for (QtMaterial row : found)
        {
            if (StringUtils.isNotEmpty(row.getFilePath()))
            {
                QtMaterialStoragePaths.resolveStoredFile(row.getFilePath());
            }
        }
        List<Long> deletedIds = new ArrayList<>();
        if (!found.isEmpty())
        {
            List<Long> foundIds = new ArrayList<>();
            for (QtMaterial row : found)
            {
                foundIds.add(row.getMaterialId());
            }
            int affected = qtMaterialMapper.deleteMaterialsByIds(foundIds);
            if (affected != foundIds.size())
            {
                throw new ServiceException("存在无法删除的资料，本次未删除任何资料", HttpStatus.CONFLICT);
            }
            deletedIds.addAll(foundIds);
            String operator = SecurityUtils.getUsername();
            for (QtMaterial row : found)
            {
                if (StringUtils.isEmpty(row.getFilePath()))
                {
                    continue;
                }
                QtMaterialFileCleanup cleanup = new QtMaterialFileCleanup();
                cleanup.setMaterialId(row.getMaterialId());
                cleanup.setFilePath(row.getFilePath());
                cleanup.setCreateBy(operator == null ? "" : operator);
                qtMaterialFileCleanupMapper.insertCleanup(cleanup);
            }
        }
        scheduleFilePurge();
        QtMaterialBatchDeleteResult result = new QtMaterialBatchDeleteResult();
        result.setDeletedIds(deletedIds);
        result.setAlreadyDeletedIds(alreadyDeletedIds);
        return result;
    }

    @Override
    public void purgePendingFileCleanups()
    {
        List<QtMaterialFileCleanup> pending = qtMaterialFileCleanupMapper.selectPending();
        if (pending == null)
        {
            return;
        }
        for (QtMaterialFileCleanup task : pending)
        {
            try
            {
                Path path = QtMaterialStoragePaths.resolveStoredFile(task.getFilePath());
                java.io.File file = path.toFile();
                if (file.exists() && !file.isFile())
                {
                    throw new ServiceException("资料文件位置不受信任，未执行删除", HttpStatus.CONFLICT);
                }
                if (file.exists() && !file.delete())
                {
                    qtMaterialFileCleanupMapper.markResult(task.getCleanupId(), "FAILED", "文件删除失败");
                    continue;
                }
                qtMaterialFileCleanupMapper.markResult(task.getCleanupId(), "DONE", null);
            }
            catch (RuntimeException ex)
            {
                qtMaterialFileCleanupMapper.markResult(task.getCleanupId(), "FAILED", "文件清理失败");
            }
        }
    }

    private void scheduleFilePurge()
    {
        if (TransactionSynchronizationManager.isSynchronizationActive())
        {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization()
            {
                @Override
                public void afterCommit()
                {
                    purgePendingFileCleanups();
                }
            });
            return;
        }
        purgePendingFileCleanups();
    }

    private static List<Long> parseBatchIds(JsonNode body)
    {
        if (body == null || !body.isObject() || body.size() != 1 || !body.has("materialIds") || body.get("materialIds") == null
                || !body.get("materialIds").isArray())
        {
            throw invalidBatch();
        }
        JsonNode array = body.get("materialIds");
        if (array.size() < 1 || array.size() > 20)
        {
            throw invalidBatch();
        }
        LinkedHashSet<Long> unique = new LinkedHashSet<>();
        for (JsonNode node : array)
        {
            if (node == null || !node.isIntegralNumber() || node.isFloatingPointNumber())
            {
                throw invalidBatch();
            }
            String text = node.asText();
            if (text == null || !text.matches("[1-9]\\d*"))
            {
                throw invalidBatch();
            }
            long value;
            try
            {
                value = Long.parseLong(text);
            }
            catch (NumberFormatException ex)
            {
                throw invalidBatch();
            }
            if (value <= 0 || value > JS_MAX_SAFE_INTEGER || !unique.add(value))
            {
                throw invalidBatch();
            }
        }
        return new ArrayList<>(unique);
    }

    private static ServiceException invalidBatch()
    {
        return new ServiceException("批量删除参数不合法", HttpStatus.BAD_REQUEST);
    }

    @Override
    public boolean canView(QtMaterial material)
    {
        if (material == null)
        {
            return false;
        }
        if (QtAuthUtils.hasAdminList(QtAuthUtils.PERM_MATERIAL_LIST) || QtAuthUtils.isCeo())
        {
            return true;
        }
        Long userId = SecurityUtils.getUserId();
        if (userId != null && userId.equals(material.getUploaderId()))
        {
            return true;
        }
        String visibility = material.getVisibility();
        if ("ALL".equals(visibility))
        {
            return true;
        }
        SysUser user = SecurityUtils.getLoginUser().getUser();
        if ("MEMBER".equals(visibility))
        {
            return user != null && "1".equals(user.getIsQuantaMember());
        }
        if ("DEPT".equals(visibility))
        {
            return user != null && StringUtils.isNotEmpty(user.getMemberDepartment())
                    && user.getMemberDepartment().equals(material.getUploaderDepartment());
        }
        return false;
    }

    @Override
    public boolean canModify(QtMaterial material)
    {
        if (QtAuthUtils.isCeo() || QtAuthUtils.hasPermi("qt:material:remove") || QtAuthUtils.hasPermi("qt:material:edit"))
        {
            return true;
        }
        Long userId = SecurityUtils.getUserId();
        return userId != null && userId.equals(material.getUploaderId());
    }

    private void applyVisibilityParams(QtMaterial query)
    {
        if (QtAuthUtils.hasAdminList(QtAuthUtils.PERM_MATERIAL_LIST) || QtAuthUtils.isCeo())
        {
            query.getParams().put("admin", "1");
            return;
        }
        SysUser user = SecurityUtils.getLoginUser().getUser();
        query.getParams().put("admin", "0");
        query.getParams().put("userId", SecurityUtils.getUserId());
        query.getParams().put("isMember", user == null ? "0" : user.getIsQuantaMember());
        query.getParams().put("dept", user == null ? "" : user.getMemberDepartment());
    }
}
