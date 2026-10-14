package com.ruoyi.web.controller.system;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.annotation.RateLimiter;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.enums.LimitType;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.MailUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.file.FileUploadUtils;
import com.ruoyi.common.utils.file.FileUtils;
import com.ruoyi.common.utils.file.FileValidator;
import com.ruoyi.common.utils.file.MimeTypeUtils;
import com.ruoyi.common.utils.ip.IpUtils;
import com.ruoyi.framework.web.service.SysRegisterService;
import com.ruoyi.framework.web.service.TokenService;
import com.ruoyi.system.service.ISysUserService;

/**
 * 个人信息 业务处理
 * 
 * @author ruoyi
 */
@RestController
@RequestMapping("/system/user/profile")
public class SysProfileController extends BaseController
{
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    @Autowired
    private ISysUserService userService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private RedisCache redisCache;

    @Autowired
    private MailUtils mailUtils;

    /**
     * 个人信息
     */
    @GetMapping
    public AjaxResult profile()
    {
        LoginUser loginUser = getLoginUser();
        SysUser user = loginUser.getUser();
        AjaxResult ajax = AjaxResult.success(user);
        ajax.put("roleGroup", userService.selectUserRoleGroup(loginUser.getUsername()));
        ajax.put("postGroup", userService.selectUserPostGroup(loginUser.getUsername()));
        return ajax;
    }

    /**
     * 修改用户
     */
    @Log(title = "个人信息", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult updateProfile(@RequestBody SysUser user)
    {
        LoginUser loginUser = getLoginUser();
        SysUser currentUser = loginUser.getUser();
        currentUser.setNickName(user.getNickName());
        currentUser.setPhonenumber(user.getPhonenumber());
        currentUser.setSex(user.getSex());
        if (isFreshman(currentUser))
        {
            String requestedEmail = StringUtils.trim(user.getEmail());
            if (StringUtils.isNotEmpty(requestedEmail)
                    && !requestedEmail.equalsIgnoreCase(StringUtils.trim(currentUser.getEmail())))
            {
                return error("新生请通过邮箱验证码入口修改邮箱");
            }
        }
        else
        {
            currentUser.setEmail(user.getEmail());
        }
        if (StringUtils.isNotEmpty(user.getPhonenumber()) && !userService.checkPhoneUnique(currentUser))
        {
            return error("修改用户'" + loginUser.getUsername() + "'失败，手机号码已存在");
        }
        if (!isFreshman(currentUser) && StringUtils.isNotEmpty(user.getEmail()) && !userService.checkEmailUnique(currentUser))
        {
            return error("修改用户'" + loginUser.getUsername() + "'失败，邮箱账号已存在");
        }
        if (userService.updateUserProfile(currentUser) > 0)
        {
            // 更新缓存用户信息
            tokenService.setLoginUser(loginUser);
            return success();
        }
        return error("修改个人信息异常，请联系管理员");
    }

    /**
     * 重置密码
     */
    @Log(title = "个人信息", businessType = BusinessType.UPDATE)
    @PutMapping("/updatePwd")
    public AjaxResult updatePwd(@RequestBody Map<String, String> params)
    {
        String oldPassword = params.get("oldPassword");
        String newPassword = params.get("newPassword");
        LoginUser loginUser = getLoginUser();
        Long userId = loginUser.getUserId();
        SysUser user = userService.selectUserById(userId);
        String password = user.getPassword();
        if (!SecurityUtils.matchesPassword(oldPassword, password))
        {
            return error("修改密码失败，旧密码错误");
        }
        if (SecurityUtils.matchesPassword(newPassword, password))
        {
            return error("新密码不能与旧密码相同");
        }
        newPassword = SecurityUtils.encryptPassword(newPassword);
        if (userService.resetUserPwd(userId, newPassword) > 0)
        {
            // 更新缓存用户密码&密码最后更新时间
            loginUser.getUser().setPwdUpdateDate(DateUtils.getNowDate());
            loginUser.getUser().setPassword(newPassword);
            tokenService.setLoginUser(loginUser);
            return success();
        }
        return error("修改密码异常，请联系管理员");
    }

    /**
     * 向新邮箱发送 6 位验证码。仅新生。
     */
    @PostMapping("/emailCode")
    @RateLimiter(time = 60, count = 3, limitType = LimitType.IP, key = "rate_limit:profile_email:")
    public AjaxResult sendEmailCode(@RequestBody Map<String, String> params)
    {
        LoginUser loginUser = getLoginUser();
        SysUser currentUser = loginUser.getUser();
        String freshmanError = requireFreshman(currentUser);
        if (freshmanError != null)
        {
            return error(freshmanError);
        }

        String email = StringUtils.trim(params.get("email"));
        String emailError = validateNewEmail(currentUser, email);
        if (emailError != null)
        {
            return error(emailError);
        }

        String code = SysRegisterService.randomEmailCode();
        mailUtils.sendText(email, "Quanta 修改邮箱验证码",
                "您的邮箱修改验证码为 " + code + "，5 分钟内有效。如非本人操作请忽略。");
        redisCache.setCacheObject(CacheConstants.PROFILE_EMAIL_CODE_KEY + loginUser.getUserId(),
                code + "|" + email.toLowerCase(), 5, TimeUnit.MINUTES);
        logger.info("profile email sent, userId={}, ip={}", loginUser.getUserId(), IpUtils.getIpAddr());
        return success("验证码已发送，5 分钟内有效");
    }

    /**
     * 新生修改邮箱。塔员和管理员邮箱仍由管理流程维护。
     */
    @Log(title = "新生邮箱", businessType = BusinessType.UPDATE)
    @PutMapping("/updateEmail")
    public AjaxResult updateEmail(@RequestBody Map<String, String> params)
    {
        LoginUser loginUser = getLoginUser();
        SysUser currentUser = loginUser.getUser();
        String freshmanError = requireFreshman(currentUser);
        if (freshmanError != null)
        {
            return error(freshmanError);
        }

        String email = StringUtils.trim(params.get("email"));
        String emailCode = StringUtils.trim(params.get("emailCode"));
        String emailError = validateNewEmail(currentUser, email);
        if (emailError != null)
        {
            return error(emailError);
        }
        if (StringUtils.isEmpty(emailCode))
        {
            return error("邮箱验证码不能为空");
        }
        if (!emailCode.matches("^\\d{6}$"))
        {
            return error("邮箱验证码不正确");
        }
        consumeProfileEmailCode(loginUser.getUserId(), email, emailCode);

        currentUser.setEmail(email);
        if (!userService.checkEmailUnique(currentUser))
        {
            return error("邮箱账号已存在");
        }
        if (userService.updateUserProfile(currentUser) > 0)
        {
            tokenService.setLoginUser(loginUser);
            return success();
        }
        return error("修改邮箱异常，请联系管理员");
    }

    private static boolean isFreshman(SysUser user)
    {
        return user != null && "0".equals(user.getIsQuantaMember());
    }

    private static String requireFreshman(SysUser user)
    {
        return isFreshman(user) ? null : "仅新生可通过该入口修改邮箱";
    }

    private String validateNewEmail(SysUser currentUser, String email)
    {
        if (StringUtils.isEmpty(email))
        {
            return "邮箱不能为空";
        }
        if (email.length() > 50 || !EMAIL_PATTERN.matcher(email).matches())
        {
            return "邮箱格式不正确";
        }
        if (email.equalsIgnoreCase(StringUtils.trim(currentUser.getEmail())))
        {
            return "新邮箱与当前邮箱相同";
        }
        SysUser unique = new SysUser();
        unique.setUserId(currentUser.getUserId());
        unique.setEmail(email);
        if (!userService.checkEmailUnique(unique))
        {
            return "邮箱账号已存在";
        }
        return null;
    }

    private void consumeProfileEmailCode(Long userId, String email, String emailCode)
    {
        String verifyKey = CacheConstants.PROFILE_EMAIL_CODE_KEY + userId;
        String cached = redisCache.getCacheObject(verifyKey);
        if (StringUtils.isEmpty(cached))
        {
            throw new ServiceException("邮箱验证码已过期，请重新获取");
        }
        String[] parts = cached.split("\\|", 2);
        if (parts.length != 2 || !emailCode.equals(parts[0]) || !email.equalsIgnoreCase(parts[1]))
        {
            throw new ServiceException("邮箱验证码不正确");
        }
        redisCache.deleteObject(verifyKey);
    }

    /**
     * 头像上传
     */
    @Log(title = "用户头像", businessType = BusinessType.UPDATE)
    @PostMapping("/avatar")
    public AjaxResult avatar(@RequestParam("avatarfile") MultipartFile file) throws Exception
    {
        if (!file.isEmpty())
        {
            LoginUser loginUser = getLoginUser();
            String avatar = FileUploadUtils.upload(RuoYiConfig.getAvatarPath(), file, MimeTypeUtils.IMAGE_EXTENSION,
                    true, FileValidator.SIZE_IMAGE);
            if (userService.updateUserAvatar(loginUser.getUserId(), avatar))
            {
                String oldAvatar = loginUser.getUser().getAvatar();
                if (StringUtils.isNotEmpty(oldAvatar))
                {
                    FileUtils.deleteFile(RuoYiConfig.getProfile() + FileUtils.stripPrefix(oldAvatar));
                }
                AjaxResult ajax = AjaxResult.success();
                ajax.put("imgUrl", avatar);
                // 更新缓存用户头像
                loginUser.getUser().setAvatar(avatar);
                tokenService.setLoginUser(loginUser);
                return ajax;
            }
        }
        return error("上传图片异常，请联系管理员");
    }
}
