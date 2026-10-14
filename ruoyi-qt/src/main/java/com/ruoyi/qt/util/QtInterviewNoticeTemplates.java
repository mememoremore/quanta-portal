package com.ruoyi.qt.util;

import org.springframework.web.util.HtmlUtils;
import com.ruoyi.common.utils.StringUtils;

/**
 * Fixed HTML/plain templates for one recruitment result mail, keyed by offered department.
 */
public final class QtInterviewNoticeTemplates
{
    public static final String QR_CONTENT_ID = "qrcode";

    private QtInterviewNoticeTemplates()
    {
    }

    public static NoticeMail render(String realName, String offeredDepartment, boolean pass, boolean preview)
    {
        if (!pass)
        {
            return reject(realName);
        }
        return offer(realName, offeredDepartment, preview);
    }

    public static NoticeMail offer(String realName, String department, boolean preview)
    {
        String code = department == null ? "" : department.trim().toUpperCase();
        switch (code)
        {
            case "BACKEND":
                return offerMail(realName, "\u540e\u7aef\u90e8", preview);
            case "PRODUCT":
                return offerMail(realName, "\u4ea7\u54c1\u90e8", preview);
            case "DESIGN":
                return offerMail(realName, "\u8bbe\u8ba1\u90e8", preview);
            case "FRONTEND":
                return offerMail(realName, "\u524d\u7aef\u90e8", preview);
            default:
                String label = StringUtils.isEmpty(code) ? "Quanta" : code;
                return offerMail(realName, label, preview);
        }
    }

    public static NoticeMail reject(String realName)
    {
        String plainName = plainName(realName);
        String greeting = "\u4f60\u597d\uff0c" + plainName + "\u3002";
        String body = "\u611f\u8c22\u4f60\u53c2\u52a0 Quanta \u62db\u65b0\u9762\u8bd5\u3002\u5f88\u9057\u61be\u672c\u6b21\u672a\u80fd\u5f55\u7528\uff0c\u795d\u4f60\u5b66\u4e60\u987a\u5229\u3002";
        return new NoticeMail("Quanta \u62db\u65b0\u7ed3\u679c\u901a\u77e5", greeting + "\n\n" + body,
                "<p>" + htmlGreeting(plainName) + "</p><p>" + body + "</p>");
    }

    private static NoticeMail offerMail(String realName, String deptLabel, boolean preview)
    {
        String plainName = plainName(realName);
        String greeting = "\u4f60\u597d\uff0c" + plainName + "\u3002";
        String intro = "\u4f60\u5df2\u901a\u8fc7 Quanta " + deptLabel + "\u9762\u8bd5\uff0c\u6b63\u5f0f\u52a0\u5165" + deptLabel
                + "\u3002";
        String scanHint = "\u8bf7\u626b\u63cf\u4e0b\u65b9\u4e8c\u7ef4\u7801\u52a0\u5165\u90e8\u95e8\u7fa4\uff0c\u540e\u7eed\u5b89\u6392\u5728\u7fa4\u5185\u901a\u77e5\u3002";
        String fallback = "\u82e5\u56fe\u7247\u65e0\u6cd5\u663e\u793a\uff0c\u8bf7\u6253\u5f00\u9644\u4ef6\u4e2d\u7684\u4e8c\u7ef4\u7801\u3002";
        String qrBlock = preview
                ? "<p><em>\u53d1\u9001\u65f6\u5c06\u5728\u6b64\u5904\u663e\u793a\u7fa4\u4e8c\u7ef4\u7801</em></p>"
                : "<p><img src=\"cid:" + QR_CONTENT_ID
                        + "\" alt=\"\u90e8\u95e8\u7fa4\u4e8c\u7ef4\u7801\" style=\"max-width:240px;height:auto;\" /></p>"
                        + "<p>" + fallback + "</p>";
        String html = "<p>" + htmlGreeting(plainName) + "</p><p>" + intro + "</p><p>" + scanHint + "</p>" + qrBlock;
        String plain = greeting + "\n\n" + intro + "\n\n" + scanHint + "\n\n"
                + (preview ? "\u53d1\u9001\u65f6\u5c06\u5728\u6b64\u5904\u663e\u793a\u7fa4\u4e8c\u7ef4\u7801"
                        : fallback);
        return new NoticeMail("Quanta " + deptLabel + "\u5f55\u7528\u901a\u77e5", plain, html);
    }

    private static String plainName(String realName)
    {
        return StringUtils.isEmpty(realName) ? "" : realName;
    }

    private static String htmlGreeting(String plainName)
    {
        return "\u4f60\u597d\uff0c" + HtmlUtils.htmlEscape(plainName, "UTF-8") + "\u3002";
    }

    public static final class NoticeMail
    {
        public final String subject;
        public final String plain;
        public final String html;

        private NoticeMail(String subject, String plain, String html)
        {
            this.subject = subject;
            this.plain = plain;
            this.html = html;
        }
    }
}
