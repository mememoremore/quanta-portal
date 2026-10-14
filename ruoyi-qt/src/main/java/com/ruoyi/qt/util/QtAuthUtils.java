package com.ruoyi.qt.util;

import java.util.function.Consumer;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;

/**
 * Quanta 管理端鉴权与数据范围工具
 */
public final class QtAuthUtils
{
    public static final String PERM_MEMBER_LIST = "qt:member:list";
    public static final String PERM_ACTIVITY_LIST = "qt:activity:list";
    public static final String PERM_SIGNUP_LIST = "qt:signup:list";
    public static final String PERM_BORROW_LIST = "qt:borrow:list";
    public static final String PERM_RESERVATION_LIST = "qt:reservation:list";
    public static final String PERM_ORDER_LIST = "qt:order:list";
    public static final String PERM_INTERVIEW_LIST = "qt:interview:admin:list";
    public static final String PERM_INTERVIEW_EVALUATE = "qt:interview:admin:evaluate";
    public static final String PERM_INTERVIEW_OFFER = "qt:interview:admin:offer";
    public static final String PERM_MATERIAL_LIST = "qt:material:list";
    public static final String PERM_DASHBOARD = "qt:dashboard:stats";
    public static final String PERM_REGISTRATIONS = "qt:activity:registrations";
    public static final String ROLE_CEO = "ceo";
    public static final String ROLE_MGMT = "qt_mgmt";
    public static final String ROLE_MANAGER = "qt_manager";

    private QtAuthUtils()
    {
    }

    public static boolean isCeo()
    {
        return SecurityUtils.isAdmin() || SecurityUtils.hasRole(ROLE_CEO);
    }

    public static boolean isManagement()
    {
        return isCeo() || SecurityUtils.hasRole(ROLE_MGMT);
    }

    public static boolean isBackofficeUser()
    {
        return isManagement() || SecurityUtils.hasRole(ROLE_MANAGER);
    }

    public static boolean isQuantaMember()
    {
        SysUser user = SecurityUtils.getLoginUser().getUser();
        return user != null && "1".equals(user.getIsQuantaMember());
    }

    public static void requireQuantaMember()
    {
        if (!isQuantaMember())
        {
            throw new ServiceException("仅塔员可访问该功能");
        }
    }

    public static boolean hasPermi(String permission)
    {
        return SecurityUtils.hasPermi(permission);
    }

    public static boolean hasAdminList(String listPermission)
    {
        return isCeo() || hasPermi(listPermission);
    }

    public static void requireCeo()
    {
        if (!isCeo())
        {
            throw new ServiceException("仅 CEO 可执行该操作");
        }
    }

    public static void requireManagement()
    {
        if (!isManagement())
        {
            throw new ServiceException("仅管理层可执行该操作");
        }
    }

    public static void requireCeoIfQuantaMember(String isQuantaMember)
    {
        if ("1".equals(isQuantaMember))
        {
            requireCeo();
        }
    }

    public static String currentDepartment()
    {
        SysUser user = SecurityUtils.getLoginUser().getUser();
        return user == null ? null : user.getMemberDepartment();
    }

    public static boolean isManagerOnly()
    {
        return SecurityUtils.hasRole(ROLE_MANAGER) && !isCeo() && !SecurityUtils.hasRole(ROLE_MGMT);
    }

    /**
     * 无管理列表权限时，强制只查当前用户自己的记录。
     */
    public static void restrictToSelfIfNoAdmin(String listPermission, Consumer<Long> setUserId)
    {
        if (!hasAdminList(listPermission))
        {
            setUserId.accept(SecurityUtils.getUserId());
        }
    }

    /**
     * 门户「我的服务」：始终只查当前登录用户，忽略客户端传入的 userId。
     */
    public static void restrictToSelf(Consumer<Long> setUserId)
    {
        setUserId.accept(SecurityUtils.getUserId());
    }

    /**
     * 活动报名管理：后台管理身份可查看全量，新生端只能查看本人。
     */
    public static void restrictSignupToSelfIfNoBackoffice(Consumer<Long> setUserId)
    {
        if (!isBackofficeUser())
        {
            setUserId.accept(SecurityUtils.getUserId());
        }
    }

    public static void assertSignupOwnerOrBackoffice(Long ownerUserId)
    {
        if (isBackofficeUser())
        {
            return;
        }
        Long current = SecurityUtils.getUserId();
        if (ownerUserId == null || current == null || !current.equals(ownerUserId))
        {
            throw new ServiceException("无权查看该报名记录");
        }
    }

    /**
     * 详情/导出等按 ID 读取：管理员或记录主人可看，否则拒绝。
     */
    public static void assertOwnerOrAdmin(Long ownerUserId, String listPermission)
    {
        if (hasAdminList(listPermission))
        {
            return;
        }
        Long current = SecurityUtils.getUserId();
        if (ownerUserId == null || current == null || !current.equals(ownerUserId))
        {
            throw new ServiceException("无权查看该记录");
        }
    }

    /**
     * 招新：仅 CEO/超管可看全部部门。部门 VP、经理层都只看本部门志愿相关的候选人。
     */
    public static String scopedDepartment()
    {
        if (isCeo())
        {
            return null;
        }
        String dept = currentDepartment();
        if (StringUtils.isEmpty(dept))
        {
            throw new ServiceException("当前账号未绑定成员部门，无法访问招新数据");
        }
        return dept;
    }

    public static void assertApplicationScope(String firstChoice, String secondChoice)
    {
        String scoped = scopedDepartment();
        if (scoped == null)
        {
            return;
        }
        if (!scoped.equals(firstChoice) && !scoped.equals(secondChoice))
        {
            throw new ServiceException("无权访问其他部门候选人");
        }
    }

    public static void assertDepartmentScope(String department)
    {
        String scoped = scopedDepartment();
        if (scoped != null && (StringUtils.isEmpty(department) || !scoped.equals(department)))
        {
            throw new ServiceException("只能操作本部门数据");
        }
    }
}
