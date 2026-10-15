package com.ruoyi.framework.web.service;

import java.io.Serializable;

/**
 * 服务端保存的 Refresh 会话。Redis 只存哈希，不存 Cookie 原文。
 */
public class RefreshSession implements Serializable
{
    private static final long serialVersionUID = 1L;

    private Long userId;

    private String username;

    private String accessUuid;

    private String familyId;

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public String getUsername()
    {
        return username;
    }

    public void setUsername(String username)
    {
        this.username = username;
    }

    public String getAccessUuid()
    {
        return accessUuid;
    }

    public void setAccessUuid(String accessUuid)
    {
        this.accessUuid = accessUuid;
    }

    public String getFamilyId()
    {
        return familyId;
    }

    public void setFamilyId(String familyId)
    {
        this.familyId = familyId;
    }
}
