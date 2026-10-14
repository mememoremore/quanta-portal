package com.ruoyi.qt.util;

import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.framework.config.ServerConfig;
import com.ruoyi.framework.security.ProfileAccessSigner;
import com.ruoyi.qt.domain.QtInterviewApplication;

/**
 * Sign photo and PDF resume URLs for interview applications.
 */
public final class QtInterviewAccessUrls
{
    private QtInterviewAccessUrls()
    {
    }

    public static void fill(QtInterviewApplication application, ProfileAccessSigner signer, ServerConfig serverConfig)
    {
        if (application == null)
        {
            return;
        }
        application.setPhotoAccessUrl(sign(application.getPhotoUrl(), signer, serverConfig));
        application.setResumeAccessUrl(sign(application.getResumeUrl(), signer, serverConfig));
    }

    public static void clearSensitiveFiles(QtInterviewApplication application)
    {
        if (application == null)
        {
            return;
        }
        application.setPhonenumber(null);
        application.setPhotoUrl(null);
        application.setPhotoAccessUrl(null);
        application.setResumeUrl(null);
        application.setResumeAccessUrl(null);
        application.setResumeFileName(null);
    }

    private static String sign(String path, ProfileAccessSigner signer, ServerConfig serverConfig)
    {
        if (StringUtils.isEmpty(path) || signer == null)
        {
            return path;
        }
        if (path.startsWith("http://") || path.startsWith("https://"))
        {
            return signer.signUrl(path);
        }
        String base = serverConfig == null ? "" : serverConfig.getUrl();
        return signer.signUrl(base + path);
    }
}
