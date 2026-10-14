package com.ruoyi.web.controller.system;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Anonymous;
import com.ruoyi.common.annotation.RateLimiter;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.model.RegisterBody;
import com.ruoyi.common.enums.LimitType;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.framework.web.service.SysRegisterService;
import com.ruoyi.system.service.ISysConfigService;

/**
 * 注册验证
 * 
 * @author ruoyi
 */
@RestController
public class SysRegisterController extends BaseController
{
    @Autowired
    private SysRegisterService registerService;

    @Autowired
    private ISysConfigService configService;

    @Anonymous
    @PostMapping("/register/emailCode")
    @RateLimiter(time = 60, count = 3, limitType = LimitType.IP, key = "rate_limit:register_email:")
    public AjaxResult sendEmailCode(@RequestBody RegisterBody user)
    {
        registerService.sendRegisterEmailCode(user);
        return success("验证码已发送，5 分钟内有效");
    }

    @PostMapping("/register")
    @RateLimiter(time = 3600, count = 5, limitType = LimitType.IP, key = "rate_limit:register:")
    public AjaxResult register(@RequestBody RegisterBody user)
    {
        if (!("true".equals(configService.selectConfigByKey("sys.account.registerUser"))))
        {
            return error("当前系统没有开启注册功能！");
        }
        String loginType = user.getLoginType();
        if ("1".equals(loginType) || "member".equalsIgnoreCase(loginType))
        {
            return error("塔员账号需由管理员开通，不支持自助注册");
        }
        // 自助注册固定按新生处理
        user.setLoginType("0");
        String msg = registerService.register(user);
        return StringUtils.isEmpty(msg) ? success() : error(msg);
    }
}
