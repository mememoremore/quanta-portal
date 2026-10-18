package com.ruoyi.qt.util;

import java.util.List;
import org.junit.jupiter.api.Test;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.qt.domain.QtMemberProfileUpdateBody;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QtMemberProfileValidatorTest
{
    @Test
    void rejectsOversizedOrControlCommunityNickname()
    {
        QtMemberProfileUpdateBody tooLong = new QtMemberProfileUpdateBody();
        tooLong.setCommunityNickname("名".repeat(21));
        assertEquals(HttpStatus.BAD_REQUEST,
                assertThrows(ServiceException.class, () -> QtMemberProfileValidator.validate(tooLong)).getCode());

        QtMemberProfileUpdateBody control = new QtMemberProfileUpdateBody();
        control.setCommunityNickname("昵称\n换行");
        assertEquals(HttpStatus.BAD_REQUEST,
                assertThrows(ServiceException.class, () -> QtMemberProfileValidator.validate(control)).getCode());

        QtMemberProfileUpdateBody clear = new QtMemberProfileUpdateBody();
        clear.setCommunityNickname("  ");
        assertEquals("", QtMemberProfileValidator.normalize(clear).getCommunityNickname());
    }

    @Test
    void rejectsInvalidPublicUrlAndTooManySkills()
    {
        QtMemberProfileUpdateBody body = new QtMemberProfileUpdateBody();
        body.setGithubUrl("https://user:pass@example.com");
        body.setSkills(List.of("A", "B", "C", "D", "E", "F"));
        ServiceException error = assertThrows(ServiceException.class, () -> QtMemberProfileValidator.validate(body));
        assertEquals(HttpStatus.BAD_REQUEST, error.getCode());
    }

    @Test
    void requiresEmailWhenMarkedPublic()
    {
        QtMemberProfileUpdateBody body = new QtMemberProfileUpdateBody();
        body.setShowContactEmail(true);
        ServiceException error = assertThrows(ServiceException.class, () -> QtMemberProfileValidator.validate(body));
        assertEquals(HttpStatus.BAD_REQUEST, error.getCode());
    }
}
