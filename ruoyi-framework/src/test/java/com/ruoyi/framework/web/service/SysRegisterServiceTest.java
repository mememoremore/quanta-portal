package com.ruoyi.framework.web.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.RegisterBody;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.system.service.ISysConfigService;
import com.ruoyi.system.service.ISysUserService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SysRegisterServiceTest
{
    @Mock
    private ISysUserService userService;

    @Mock
    private ISysConfigService configService;

    @Mock
    private RedisCache redisCache;

    @InjectMocks
    private SysRegisterService registerService;

    @BeforeEach
    void disableCaptcha()
    {
        when(configService.selectCaptchaEnabled()).thenReturn(false);
    }

    @Test
    void rejectsStudentNumberOutsideTheElevenDigitSchoolFormat()
    {
        RegisterBody body = registration("2024100319", "freshman@example.com");

        assertEquals("学号格式不正确，请输入11位学号", registerService.register(body));
        verify(userService, never()).registerUser(any());
    }

    @Test
    void rejectsInvalidEmailBeforeCreatingTheUser()
    {
        RegisterBody body = registration("20241003193", "not-an-email");

        assertEquals("邮箱格式不正确", registerService.register(body));
        verify(userService, never()).registerUser(any());
    }

    @Test
    void usesStudentNumberAsUsernameAndPersistsEmailWithoutClassName()
    {
        RegisterBody body = registration("20241003193", "freshman@example.com");
        body.setUsername("client-supplied-name");
        when(userService.checkUserNameUnique(any())).thenReturn(true);
        when(userService.checkStudentNoUnique("20241003193")).thenReturn(true);
        when(userService.checkEmailUnique(any())).thenReturn(true);
        when(userService.registerUser(any())).thenReturn(false);

        String message = registerService.register(body);

        assertTrue(message.startsWith("注册失败"));
        ArgumentCaptor<SysUser> captor = ArgumentCaptor.forClass(SysUser.class);
        verify(userService).registerUser(captor.capture());
        SysUser user = captor.getValue();
        assertEquals("20241003193", user.getUserName());
        assertEquals("20241003193", user.getStudentNo());
        assertEquals("freshman@example.com", user.getEmail());
        assertEquals("0", user.getIsQuantaMember());
    }

    private RegisterBody registration(String studentNo, String email)
    {
        RegisterBody body = new RegisterBody();
        body.setStudentNo(studentNo);
        body.setEmail(email);
        body.setPassword("secret123");
        body.setLoginType("0");
        return body;
    }
}
