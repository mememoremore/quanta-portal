package com.ruoyi.qt.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.qt.domain.QtLabMember;
import com.ruoyi.qt.domain.QtMemberManageBody;
import com.ruoyi.qt.domain.QtMemberProfileUpdateBody;
import com.ruoyi.qt.domain.QtMemberRetainBody;
import com.ruoyi.qt.service.IQtForumService;
import com.ruoyi.qt.service.IQtLabMemberService;
import com.ruoyi.qt.service.IQtMemberProfileService;
import com.ruoyi.qt.util.QtAuthUtils;

@RestController
@RequestMapping("/qt/member")
public class QtLabMemberController extends BaseController
{
    @Autowired
    private IQtLabMemberService qtLabMemberService;

    @Autowired
    private IQtMemberProfileService qtMemberProfileService;

    @Autowired
    private IQtForumService qtForumService;

    @GetMapping("/profile")
    public AjaxResult ownProfile()
    {
        QtAuthUtils.requireQuantaMember();
        return success(qtMemberProfileService.selectOwnProfile());
    }

    @Log(title = "塔员个人档案", businessType = BusinessType.UPDATE)
    @PutMapping("/profile")
    public AjaxResult saveProfile(@RequestBody QtMemberProfileUpdateBody body)
    {
        QtAuthUtils.requireQuantaMember();
        qtMemberProfileService.saveOwnProfile(body);
        return success();
    }

    @GetMapping("/{userId:\\d+}/profile")
    public AjaxResult publicProfile(@PathVariable Long userId)
    {
        QtAuthUtils.requireQuantaMember();
        return success(qtMemberProfileService.selectPublicProfile(userId));
    }

    @GetMapping("/{userId:\\d+}/posts")
    public TableDataInfo memberPosts(@PathVariable Long userId)
    {
        QtAuthUtils.requireQuantaMember();
        qtMemberProfileService.requireVisibleMember(userId);
        startPage();
        return getDataTable(qtForumService.selectPostListByAuthor(userId));
    }

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

    @GetMapping("/manage/options")
    public AjaxResult manageOptions()
    {
        return success(qtLabMemberService.managementOptions());
    }

    @GetMapping("/manage/{userId}")
    public AjaxResult managedMember(@PathVariable Long userId,
            @RequestParam(required = false) String memberCohort)
    {
        return success(qtLabMemberService.managedMember(userId, memberCohort));
    }

    @Log(title = "成员维护", businessType = BusinessType.INSERT)
    @PostMapping("/manage")
    public AjaxResult createManagedMember(@RequestBody QtMemberManageBody body)
    {
        return success(qtLabMemberService.createManagedMember(body, getUsername()));
    }

    @Log(title = "成员维护", businessType = BusinessType.UPDATE)
    @PutMapping("/manage/{userId}")
    public AjaxResult updateManagedMember(@PathVariable Long userId, @RequestBody QtMemberManageBody body)
    {
        return success(qtLabMemberService.updateManagedMember(userId, body, getUsername()));
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
