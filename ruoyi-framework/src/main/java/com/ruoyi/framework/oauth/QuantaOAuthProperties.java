package com.ruoyi.framework.oauth;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * OAuth 客户端密钥和签名私钥只来自运行环境，不写入仓库。
 */
@Component
@ConfigurationProperties(prefix = "quanta.oauth")
public class QuantaOAuthProperties
{
    private boolean enabled;

    private String issuer = "https://www.quantacenter.com";

    private String portalOrigin = "https://www.quantacenter.com";

    private String clientId = "quanta-challenge";

    private String clientSecret = "";

    private String redirectUri = "https://challenge.quantacenter.com/api/auth/quanta/callback";

    private String privateKeyPem = "";

    private String keyId = "quanta-oidc-1";

    private String clientName = "Quanta Challenge";

    @PostConstruct
    void failClosedWhenEnabledButIncomplete()
    {
        if (enabled && !ready())
        {
            throw new IllegalStateException("Quanta OAuth 已开启，但 issuer、客户端或签名私钥配置不完整");
        }
    }

    public boolean ready()
    {
        return enabled && "quanta-challenge".equals(clientId) && clientSecret != null && clientSecret.length() >= 32
                && httpsOrigin(issuer) && httpsOrigin(portalOrigin) && httpsAbsolute(redirectUri)
                && privateKeyPem != null && privateKeyPem.contains("BEGIN PRIVATE KEY")
                && keyId != null && !keyId.isBlank();
    }

    public String clientOrigin()
    {
        java.net.URI uri = java.net.URI.create(redirectUri);
        String origin = uri.getScheme() + "://" + uri.getHost();
        if (uri.getPort() > 0)
        {
            origin += ":" + uri.getPort();
        }
        return origin;
    }

    private static boolean httpsOrigin(String value)
    {
        return httpsAbsolute(value) && !value.substring("https://".length()).contains("/");
    }

    private static boolean httpsAbsolute(String value)
    {
        return value != null && value.startsWith("https://") && !value.endsWith("/");
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    public String getIssuer()
    {
        return issuer;
    }

    public void setIssuer(String issuer)
    {
        this.issuer = issuer;
    }

    public String getPortalOrigin()
    {
        return portalOrigin;
    }

    public void setPortalOrigin(String portalOrigin)
    {
        this.portalOrigin = portalOrigin;
    }

    public String getClientId()
    {
        return clientId;
    }

    public void setClientId(String clientId)
    {
        this.clientId = clientId;
    }

    public String getClientSecret()
    {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret)
    {
        this.clientSecret = clientSecret;
    }

    public String getRedirectUri()
    {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri)
    {
        this.redirectUri = redirectUri;
    }

    public String getPrivateKeyPem()
    {
        return privateKeyPem;
    }

    public void setPrivateKeyPem(String privateKeyPem)
    {
        this.privateKeyPem = privateKeyPem;
    }

    public String getKeyId()
    {
        return keyId;
    }

    public void setKeyId(String keyId)
    {
        this.keyId = keyId;
    }

    public String getClientName()
    {
        return clientName;
    }

    public void setClientName(String clientName)
    {
        this.clientName = clientName;
    }
}
