package com.ruoyi.web.controller.system;

import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Anonymous;
import com.ruoyi.common.annotation.RateLimiter;
import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.PasswordResetBody;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.enums.LimitType;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.MailUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.ip.IpUtils;
import com.ruoyi.framework.web.service.SysRegisterService;
import com.ruoyi.system.service.ISysUserService;

/**
 * Anonymous email password reset.
 */
@RestController
@RequestMapping("/system/password")
public class SysPasswordController extends BaseController
{
    @Autowired
    private ISysUserService userService;

    @Autowired
    private RedisCache redisCache;

    @Autowired
    private MailUtils mailUtils;

    @Anonymous
    @PostMapping("/emailCode")
    @RateLimiter(time = 60, count = 3, limitType = LimitType.IP, key = "rate_limit:reset_email:")
    public AjaxResult sendEmailCode(@RequestBody PasswordResetBody body)
    {
        String username = StringUtils.trim(body.getUsername());
        String email = StringUtils.trim(body.getEmail());
        if (StringUtils.isEmpty(username) || StringUtils.isEmpty(email))
        {
            return error("\u8d26\u53f7\u548c\u90ae\u7bb1\u4e0d\u80fd\u4e3a\u7a7a");
        }
        if (!SysRegisterService.EMAIL_PATTERN.matcher(email).matches())
        {
            return error("\u90ae\u7bb1\u683c\u5f0f\u4e0d\u6b63\u786e");
        }
        SysUser user = userService.selectUserByUserName(username);
        if (user == null || StringUtils.isEmpty(user.getEmail())
                || !email.equalsIgnoreCase(user.getEmail()))
        {
            return error("\u8d26\u53f7\u4e0e\u90ae\u7bb1\u4e0d\u5339\u914d");
        }
        if (UserConstants.USER_DISABLE.equals(user.getStatus()))
        {
            return error("\u8d26\u53f7\u5df2\u505c\u7528");
        }
        String code = SysRegisterService.randomEmailCode();
        mailUtils.sendText(email, "Quanta \u5bc6\u7801\u91cd\u7f6e\u9a8c\u8bc1\u7801",
                "\u60a8\u7684\u5bc6\u7801\u91cd\u7f6e\u9a8c\u8bc1\u7801\u4e3a " + code + "\uff0c5 \u5206\u949f\u5185\u6709\u6548\u3002\u5982\u975e\u672c\u4eba\u64cd\u4f5c\u8bf7\u5ffd\u7565\u3002");
        redisCache.setCacheObject(CacheConstants.RESET_EMAIL_CODE_KEY + username.toLowerCase(),
                code + "|" + email.toLowerCase(), 5, TimeUnit.MINUTES);
        logger.info("password reset email sent, user={}, ip={}", username, IpUtils.getIpAddr());
        return success("\u9a8c\u8bc1\u7801\u5df2\u53d1\u9001\uff0c5 \u5206\u949f\u5185\u6709\u6548");
    }

    @Anonymous
    @PostMapping("/reset")
    @RateLimiter(time = 3600, count = 5, limitType = LimitType.IP, key = "rate_limit:reset_pwd:")
    public AjaxResult reset(@RequestBody PasswordResetBody body)
    {
        String username = StringUtils.trim(body.getUsername());
        String email = StringUtils.trim(body.getEmail());
        String emailCode = StringUtils.trim(body.getEmailCode());
        String password = body.getPassword();
        if (StringUtils.isEmpty(username) || StringUtils.isEmpty(email) || StringUtils.isEmpty(emailCode))
        {
            return error("\u8d26\u53f7\u3001\u90ae\u7bb1\u548c\u9a8c\u8bc1\u7801\u4e0d\u80fd\u4e3a\u7a7a");
        }
        if (StringUtils.isEmpty(password) || password.length() < UserConstants.PASSWORD_MIN_LENGTH
                || password.length() > UserConstants.PASSWORD_MAX_LENGTH)
        {
            return error("\u5bc6\u7801\u957f\u5ea6\u5fc5\u987b\u57285\u523020\u4e2a\u5b57\u7b26\u4e4b\u95f4");
        }
        String verifyKey = CacheConstants.RESET_EMAIL_CODE_KEY + username.toLowerCase();
        String cached = redisCache.getCacheObject(verifyKey);
        if (StringUtils.isEmpty(cached))
        {
            throw new ServiceException("\u90ae\u7bb1\u9a8c\u8bc1\u7801\u5df2\u8fc7\u671f\uff0c\u8bf7\u91cd\u65b0\u83b7\u53d6");
        }
        String[] parts = cached.split("\\|", 2);
        if (parts.length != 2 || !emailCode.equals(parts[0]) || !email.equalsIgnoreCase(parts[1]))
        {
            throw new ServiceException("\u90ae\u7bb1\u9a8c\u8bc1\u7801\u4e0d\u6b63\u786e");
        }
        SysUser user = userService.selectUserByUserName(username);
        if (user == null || StringUtils.isEmpty(user.getEmail())
                || !email.equalsIgnoreCase(user.getEmail()))
        {
            return error("\u8d26\u53f7\u4e0e\u90ae\u7bb1\u4e0d\u5339\u914d");
        }
        redisCache.deleteObject(verifyKey);
        userService.resetUserPwd(user.getUserId(), SecurityUtils.encryptPassword(password));
        return success("\u5bc6\u7801\u91cd\u7f6e\u6210\u529f\uff0c\u8bf7\u4f7f\u7528\u65b0\u5bc6\u7801\u767b\u5f55");
    }
}
