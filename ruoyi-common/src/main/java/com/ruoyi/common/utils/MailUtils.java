package com.ruoyi.common.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import com.ruoyi.common.exception.ServiceException;
import jakarta.mail.internet.MimeMessage;

/**
 * SMTP sender for Aliyun DirectMail. Credentials come from env only.
 */
@Component
public class MailUtils
{
    private static final Logger log = LoggerFactory.getLogger(MailUtils.class);
    public static final String INLINE_QR_CONTENT_ID = "qrcode";

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String from;

    public void sendText(String to, String subject, String content)
    {
        sendText(to, subject, content, null, null, null);
    }

    public void sendText(String to, String subject, String content, String attachmentName, byte[] attachment,
            String contentType)
    {
        send(to, subject, content, null, attachmentName, attachment, contentType);
    }

    public void sendHtml(String to, String subject, String plain, String html, String attachmentName, byte[] attachment,
            String contentType)
    {
        send(to, subject, plain, html, attachmentName, attachment, contentType);
    }

    private void send(String to, String subject, String plain, String html, String attachmentName, byte[] attachment,
            String contentType)
    {
        if (mailSender == null || StringUtils.isEmpty(from))
        {
            throw new ServiceException("\u90ae\u4ef6\u670d\u52a1\u672a\u914d\u7f6e");
        }
        try
        {
            boolean hasAttachment = attachment != null && attachment.length > 0;
            boolean htmlMail = StringUtils.isNotEmpty(html);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, htmlMail || hasAttachment, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            if (htmlMail)
            {
                String text = StringUtils.isNotEmpty(plain) ? plain : html;
                helper.setText(text, html);
            }
            else
            {
                helper.setText(plain, false);
            }
            if (hasAttachment)
            {
                String name = StringUtils.isEmpty(attachmentName) ? "qrcode.png" : attachmentName;
                String mime = imageContentType(name, contentType);
                if (htmlMail)
                {
                    helper.addInline(INLINE_QR_CONTENT_ID, namedResource(attachment, name), mime);
                }
                helper.addAttachment(name, namedResource(attachment, name));
            }
            mailSender.send(message);
        }
        catch (ServiceException ex)
        {
            throw ex;
        }
        catch (Exception ex)
        {
            log.error("send mail failed, to={}", to, ex);
            throw new ServiceException("\u90ae\u4ef6\u53d1\u9001\u5931\u8d25\uff0c\u8bf7\u7a0d\u540e\u91cd\u8bd5");
        }
    }

    private static ByteArrayResource namedResource(byte[] bytes, String filename)
    {
        return new ByteArrayResource(bytes)
        {
            @Override
            public String getFilename()
            {
                return filename;
            }
        };
    }

    private static String imageContentType(String filename, String contentType)
    {
        if (StringUtils.isNotEmpty(contentType) && contentType.startsWith("image/"))
        {
            return contentType;
        }
        String lower = filename == null ? "" : filename.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg"))
        {
            return "image/jpeg";
        }
        return "image/png";
    }
}
