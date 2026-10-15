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
import com.ruoyi.qt.util.QtLabMemberImportParser;

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
        validateUploadPolicy(material);
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
