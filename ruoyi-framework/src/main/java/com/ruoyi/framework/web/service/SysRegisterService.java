package com.ruoyi.framework.web.service;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.RegisterBody;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.exception.user.CaptchaException;
import com.ruoyi.common.exception.user.CaptchaExpireException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.MailUtils;
import com.ruoyi.common.utils.MessageUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.ip.IpUtils;
import com.ruoyi.framework.manager.AsyncManager;
import com.ruoyi.framework.manager.factory.AsyncFactory;
import com.ruoyi.system.service.ISysConfigService;
import com.ruoyi.system.service.ISysUserService;

/**
 * 注册校验方法
 *
 * @author ruoyi
 */
@Component
public class SysRegisterService
{
    private static final Logger log = LoggerFactory.getLogger(SysRegisterService.class);

    private static final Pattern STUDENT_NO_PATTERN = Pattern.compile("^20\\d{9}$");

    public static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    @Autowired
    private ISysUserService userService;

    @Autowired
    private ISysConfigService configService;

    @Autowired
    private RedisCache redisCache;

    @Autowired
    private MailUtils mailUtils;

    public static String randomEmailCode()
    {
        return String.format("%06d", ThreadLocalRandom.current().nextInt(1000000));
    }

    /**
     * 注册（仅允许新生；塔员需后台开通）
     */
    public String register(RegisterBody registerBody)
    {
        String msg = "";
        String password = registerBody.getPassword();
        String loginType = registerBody.getLoginType();
        String studentNo = StringUtils.trim(registerBody.getStudentNo());
        String username = studentNo;
        String email = StringUtils.trim(registerBody.getEmail());
        String nickName = StringUtils.trim(registerBody.getNickName());

        SysUser sysUser = new SysUser();
        sysUser.setUserName(username);
        sysUser.setEmail(email);

        // 验证码开关
        boolean captchaEnabled = configService.selectCaptchaEnabled();
        if (captchaEnabled)
        {
            validateCaptcha(username, registerBody.getCode(), registerBody.getUuid());
        }

        if (StringUtils.isNotEmpty(loginType)
                && ("1".equals(loginType) || "member".equalsIgnoreCase(loginType)))
        {
            msg = "塔员账号需由管理员开通，不支持自助注册";
        }
        else if (StringUtils.isEmpty(studentNo))
        {
            msg = "学号不能为空";
        }
        else if (!STUDENT_NO_PATTERN.matcher(studentNo).matches())
        {
            msg = "学号格式不正确，请输入11位学号";
        }
        else if (StringUtils.isEmpty(email))
        {
            msg = "邮箱不能为空";
        }
        else if (!EMAIL_PATTERN.matcher(email).matches())
        {
            msg = "邮箱格式不正确";
        }
        else if (StringUtils.isEmpty(registerBody.getEmailCode()))
        {
            msg = "邮箱验证码不能为空";
        }
        else if (StringUtils.isEmpty(password))
        {
            msg = "用户密码不能为空";
        }
        else if (username.length() < UserConstants.USERNAME_MIN_LENGTH
                || username.length() > UserConstants.USERNAME_MAX_LENGTH)
        {
            msg = "账户长度必须在2到20个字符之间";
        }
        else if (password.length() < UserConstants.PASSWORD_MIN_LENGTH
                || password.length() > UserConstants.PASSWORD_MAX_LENGTH)
        {
            msg = "密码长度必须在5到20个字符之间";
        }
        else if (!userService.checkUserNameUnique(sysUser))
        {
            msg = "保存用户'" + username + "'失败，注册账号已存在";
        }
        else if (!userService.checkStudentNoUnique(studentNo))
        {
            msg = "学号已存在，请勿重复注册";
        }
        else if (!userService.checkEmailUnique(sysUser))
        {
            msg = "邮箱账号已存在";
        }
        else
        {
            consumeRegisterEmailCode(email, studentNo, registerBody.getEmailCode());
            sysUser.setNickName(StringUtils.isNotEmpty(nickName) ? nickName : username);
            sysUser.setStudentNo(studentNo);
            sysUser.setEmail(email);
            // 自助注册固定为新生
            sysUser.setIsQuantaMember("0");
            sysUser.setPwdUpdateDate(DateUtils.getNowDate());
            sysUser.setPassword(SecurityUtils.encryptPassword(password));
            sysUser.setCreateBy("register");
            boolean regFlag = userService.registerUser(sysUser);
            if (!regFlag)
            {
                msg = "注册失败,请联系系统管理人员";
            }
            else
            {
                AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.REGISTER, MessageUtils.message("user.register.success")));
            }
        }
        return msg;
    }

    /**
     * 向填写邮箱发送 6 位注册验证码，与学号绑定，Redis 5 分钟。
     */
    public void sendRegisterEmailCode(RegisterBody registerBody)
    {
        String email = StringUtils.trim(registerBody.getEmail());
        String studentNo = StringUtils.trim(registerBody.getStudentNo());
        if (StringUtils.isEmpty(studentNo) || !STUDENT_NO_PATTERN.matcher(studentNo).matches())
        {
            throw new ServiceException("学号格式不正确，请输入11位学号");
        }
        if (StringUtils.isEmpty(email) || !EMAIL_PATTERN.matcher(email).matches())
        {
            throw new ServiceException("邮箱格式不正确");
        }
        SysUser unique = new SysUser();
        unique.setUserName(studentNo);
        unique.setEmail(email);
        if (!userService.checkUserNameUnique(unique) || !userService.checkStudentNoUnique(studentNo))
        {
            throw new ServiceException("学号已存在，请勿重复注册");
        }
        if (!userService.checkEmailUnique(unique))
        {
            throw new ServiceException("邮箱账号已存在");
        }
        String code = randomEmailCode();
        mailUtils.sendText(email, "Quanta 注册验证码",
                "您的注册验证码为 " + code + "，5 分钟内有效。如非本人操作请忽略。");
        redisCache.setCacheObject(CacheConstants.REGISTER_EMAIL_CODE_KEY + email.toLowerCase(),
                code + "|" + studentNo, 5, TimeUnit.MINUTES);
        log.info("register email sent, studentNo={}, ip={}", studentNo, IpUtils.getIpAddr());
    }

    private void consumeRegisterEmailCode(String email, String studentNo, String emailCode)
    {
        String verifyKey = CacheConstants.REGISTER_EMAIL_CODE_KEY + email.toLowerCase();
        String cached = redisCache.getCacheObject(verifyKey);
        if (StringUtils.isEmpty(cached))
        {
            throw new ServiceException("邮箱验证码已过期，请重新获取");
        }
        String[] parts = cached.split("\\|", 2);
        if (parts.length != 2 || !StringUtils.trim(emailCode).equals(parts[0])
                || !studentNo.equals(parts[1]))
        {
            throw new ServiceException("邮箱验证码不正确");
        }
        redisCache.deleteObject(verifyKey);
    }

    /**
     * 校验验证码
     */
    public void validateCaptcha(String username, String code, String uuid)
    {
        String verifyKey = CacheConstants.CAPTCHA_CODE_KEY + StringUtils.nvl(uuid, "");
        String captcha = redisCache.getCacheObject(verifyKey);
        redisCache.deleteObject(verifyKey);
        if (captcha == null)
        {
            throw new CaptchaExpireException();
        }
        if (!code.equalsIgnoreCase(captcha))
        {
            throw new CaptchaException();
        }
    }
}
