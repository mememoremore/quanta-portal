package com.ruoyi.framework.oauth;

/**
 * 授权事务和一次性授权码。这些对象不进入接口响应。
 */
final class QuantaOAuthInteraction
{
    String id;

    String clientId;

    String redirectUri;

    String state;

    String nonce;

    String codeChallenge;

    String browserId;

    String csrfToken;

    String status;

    String subjectUserId;

    long expiresAt;

    boolean consumed;

    QuantaOAuthInteraction copy()
    {
        QuantaOAuthInteraction copy = new QuantaOAuthInteraction();
        copy.id = id;
        copy.clientId = clientId;
        copy.redirectUri = redirectUri;
        copy.state = state;
        copy.nonce = nonce;
        copy.codeChallenge = codeChallenge;
        copy.browserId = browserId;
        copy.csrfToken = csrfToken;
        copy.status = status;
        copy.subjectUserId = subjectUserId;
        copy.expiresAt = expiresAt;
        copy.consumed = consumed;
        return copy;
    }
}

final class QuantaOAuthCode
{
    String clientId;

    String redirectUri;

    String userId;

    String nonce;

    String codeChallenge;

    long expiresAt;
}

final class QuantaOAuthResponse
{
    final int status;

    final String contentType;

    final String body;

    final String redirect;

    final String browserCookie;

    private QuantaOAuthResponse(int status, String contentType, String body, String redirect, String browserCookie)
    {
        this.status = status;
        this.contentType = contentType;
        this.body = body;
        this.redirect = redirect;
        this.browserCookie = browserCookie;
    }

    static QuantaOAuthResponse json(int status, String body)
    {
        return new QuantaOAuthResponse(status, "application/json;charset=UTF-8", body, null, null);
    }

    static QuantaOAuthResponse html(int status, String message)
    {
        String safe = message == null ? "" : message.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        String body = "<!DOCTYPE html><html lang=\"zh-CN\"><head><meta charset=\"utf-8\"><title>无法继续登录</title></head><body><h1>无法继续登录</h1><p>"
                + safe + "</p></body></html>";
        return new QuantaOAuthResponse(status, "text/html;charset=UTF-8", body, null, null);
    }

    static QuantaOAuthResponse redirect(int status, String location, String browserCookie)
    {
        return new QuantaOAuthResponse(status, "text/html;charset=UTF-8", "", location, browserCookie);
    }
}
