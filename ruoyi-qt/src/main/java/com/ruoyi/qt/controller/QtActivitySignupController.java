package com.ruoyi.qt.controller;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;
import com.ruoyi.qt.validation.Create;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.annotation.RepeatSubmit;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.qt.domain.QtActivitySignup;
import com.ruoyi.qt.service.IQtActivitySignupService;
import com.ruoyi.qt.util.QtAuthUtils;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 活动报名Controller
 * 
 * @author ruoyi
 * @date 2026-04-24
 */
@RestController
@RequestMapping("/qt/signup")
public class QtActivitySignupController extends BaseController
{
    @Autowired
    private IQtActivitySignupService qtActivitySignupService;

    /**
     * 查询活动报名列表
     */
    @GetMapping("/list")
    public TableDataInfo list(QtActivitySignup qtActivitySignup)
    {
        QtAuthUtils.restrictSignupToSelfIfNoBackoffice(qtActivitySignup::setUserId);
        startPage();
        List<QtActivitySignup> list = qtActivitySignupService.selectQtActivitySignupList(qtActivitySignup);
        return getDataTable(list);
    }

    /**
     * 查询活动报名详情列表
     */
    @GetMapping("/detailList")
    public TableDataInfo detailList(QtActivitySignup qtActivitySignup)
    {
        QtAuthUtils.restrictSignupToSelfIfNoBackoffice(qtActivitySignup::setUserId);
        startPage();
        List<QtActivitySignup> list = qtActivitySignupService.selectQtActivitySignupDetailList(qtActivitySignup);
        return getDataTable(list);
    }

    /**
     * 导出活动报名列表
     */
    @PreAuthorize("@ss.hasPermi('qt:signup:export')")
    @Log(title = "活动报名", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, QtActivitySignup qtActivitySignup)
    {
        QtAuthUtils.restrictToSelfIfNoAdmin(QtAuthUtils.PERM_SIGNUP_LIST, qtActivitySignup::setUserId);
        List<QtActivitySignup> list = qtActivitySignupService.selectQtActivitySignupList(qtActivitySignup);
        ExcelUtil<QtActivitySignup> util = new ExcelUtil<QtActivitySignup>(QtActivitySignup.class);
        util.exportExcel(response, list, "活动报名数据");
    }

    /**
     * 获取活动报名详细信息。登录即可，非管理员只能看自己的。
     */
    @GetMapping(value = "/{signupId}")
    public AjaxResult getInfo(@PathVariable("signupId") Long signupId)
    {
        QtActivitySignup signup = qtActivitySignupService.selectQtActivitySignupBySignupId(signupId);
        if (signup != null)
        {
            QtAuthUtils.assertSignupOwnerOrBackoffice(signup.getUserId());
        }
        return success(signup);
    }

    /**
     * 新增活动报名（C端：自动绑定当前登录用户）
     */
    // C 端报名保持登录即可
    @Log(title = "活动报名", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @PostMapping
    public AjaxResult add(@Validated(Create.class) @RequestBody QtActivitySignup qtActivitySignup)
    {
        if (qtActivitySignup.getActivityId() == null)
        {
            return AjaxResult.error("活动ID不能为空");
        }
        qtActivitySignup.setUserId(getUserId());
        qtActivitySignup.setCreateBy(getUsername());
        if (qtActivitySignup.getStatus() == null || qtActivitySignup.getStatus().isEmpty())
        {
            qtActivitySignup.setStatus("APPLIED");
        }
        if (qtActivitySignup.getSignupTime() == null)
        {
            qtActivitySignup.setSignupTime(new java.util.Date());
        }
        QtActivitySignup query = new QtActivitySignup();
        query.setActivityId(qtActivitySignup.getActivityId());
        query.setUserId(getUserId());
        if (!qtActivitySignupService.selectQtActivitySignupList(query).isEmpty())
        {
            return AjaxResult.error("您已报名该活动");
        }
        return toAjax(qtActivitySignupService.insertQtActivitySignup(qtActivitySignup));
    }

    /**
     * 修改活动报名
     */
    @PreAuthorize("@ss.hasPermi('qt:signup:edit')")
    @Log(title = "活动报名", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody QtActivitySignup qtActivitySignup)
    {
        return toAjax(qtActivitySignupService.updateQtActivitySignup(qtActivitySignup));
    }

    /**
     * 删除活动报名
     */
    @PreAuthorize("@ss.hasPermi('qt:signup:remove')")
    @Log(title = "活动报名", businessType = BusinessType.DELETE)
	@DeleteMapping("/{signupIds}")
    public AjaxResult remove(@PathVariable Long[] signupIds)
    {
        return toAjax(qtActivitySignupService.deleteQtActivitySignupBySignupIds(signupIds));
    }
}
