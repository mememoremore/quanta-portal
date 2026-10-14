package com.ruoyi.qt.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.qt.domain.QtLabMember;
import com.ruoyi.qt.domain.QtMemberRetainBody;
import com.ruoyi.qt.service.IQtLabMemberService;
import com.ruoyi.qt.util.QtAuthUtils;

@RestController
@RequestMapping("/qt/member")
public class QtLabMemberController extends BaseController
{
    @Autowired
    private IQtLabMemberService qtLabMemberService;

    @GetMapping("/list")
    public TableDataInfo list(QtLabMember query)
    {
        QtAuthUtils.requireQuantaMember();
        startPage();
        List<QtLabMember> list;
        if (QtAuthUtils.hasAdminList(QtAuthUtils.PERM_MEMBER_LIST))
        {
            list = qtLabMemberService.selectAdminMemberList(query);
        }
        else
        {
            list = qtLabMemberService.selectLabMemberList(query);
        }
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('qt:member:list')")
    @GetMapping("/cohorts")
    public AjaxResult cohorts()
    {
        return success(qtLabMemberService.selectCohorts());
    }

    @PreAuthorize("@ss.hasPermi('qt:member:retain')")
    @Log(title = "成员留任", businessType = BusinessType.UPDATE)
    @PutMapping("/{userId}/retain")
    public AjaxResult retain(@PathVariable Long userId, @RequestBody QtMemberRetainBody body)
    {
        QtAuthUtils.requireCeo();
        return success(qtLabMemberService.retain(userId, body, getUsername()));
    }
}
