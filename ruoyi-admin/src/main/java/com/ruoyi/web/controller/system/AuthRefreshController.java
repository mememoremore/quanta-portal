package com.ruoyi.web.controller.system;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Anonymous;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.enums.UserStatus;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.framework.web.service.RefreshSession;
import com.ruoyi.framework.web.service.SysPermissionService;
import com.ruoyi.framework.web.service.TokenService;
import com.ruoyi.system.service.ISysUserService;

/**
 * 用 HttpOnly Refresh Cookie 换新的短期 Access Token。
 */
@RestController
public class AuthRefreshController
{
    @Autowired
    private TokenService tokenService;

    @Autowired
    private ISysUserService userService;

    @Autowired
    private SysPermissionService permissionService;

    @Anonymous
    @PostMapping("/auth/refresh")
    public AjaxResult refresh(HttpServletRequest request, HttpServletResponse response)
    {
        RefreshSession session = tokenService.consumeRefresh(request, response);
        if (session == null || session.getUserId() == null || StringUtils.isEmpty(session.getUsername()))
        {
            return expired();
        }
        try
        {
            SysUser user = userService.selectUserByUserName(session.getUsername());
            if (!isActive(user) || !session.getUserId().equals(user.getUserId()))
            {
                tokenService.revokeUserSessions(session.getUserId());
                tokenService.clearRefreshCookie(response);
                return expired();
            }
            LoginUser loginUser = new LoginUser(user.getUserId(), user.getDeptId(), user,
                    permissionService.getMenuPermission(user));
            String token = tokenService.createToken(loginUser);
            tokenService.issueRefresh(response, loginUser, session.getFamilyId());
            AjaxResult ajax = AjaxResult.success();
            ajax.put(Constants.TOKEN, token);
            ajax.put("expiresIn", tokenService.getAccessExpiresIn());
            return ajax;
        }
        catch (Exception ex)
        {
            tokenService.revokeUserSessions(session.getUserId());
            tokenService.clearRefreshCookie(response);
            return expired();
        }
    }

    private static boolean isActive(SysUser user)
    {
        return user != null
                && !UserStatus.DELETED.getCode().equals(user.getDelFlag())
                && !UserStatus.DISABLE.getCode().equals(user.getStatus());
    }

    private static AjaxResult expired()
    {
        return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录状态已过期，请重新登录");
    }
}
