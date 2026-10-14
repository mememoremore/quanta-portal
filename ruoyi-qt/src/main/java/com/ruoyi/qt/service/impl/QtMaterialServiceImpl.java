package com.ruoyi.qt.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.qt.domain.QtMaterial;
import com.ruoyi.qt.mapper.QtMaterialMapper;
import com.ruoyi.qt.service.IQtMaterialService;
import com.ruoyi.qt.util.QtAuthUtils;
import com.ruoyi.qt.util.QtDictUtils;

@Service
public class QtMaterialServiceImpl implements IQtMaterialService
{
    @Autowired
    private QtMaterialMapper qtMaterialMapper;

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
        if (StringUtils.isEmpty(material.getVisibility()))
        {
            material.setVisibility("MEMBER");
        }
        QtDictUtils.requireValue(QtDictUtils.MATERIAL_VISIBILITY, material.getVisibility(), "visibility");
        return qtMaterialMapper.insertMaterial(material);
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
