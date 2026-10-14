package com.ruoyi.common.core.domain.model;

/**
 * Email password-reset request body.
 */
public class PasswordResetBody
{
    private String username;

    private String email;

    private String emailCode;

    private String password;

    public String getUsername()
    {
        return username;
    }

    public void setUsername(String username)
    {
        this.username = username;
    }

    public String getEmail()
    {
        return email;
    }

    public void setEmail(String email)
    {
        this.email = email;
    }

    public String getEmailCode()
    {
        return emailCode;
    }

    public void setEmailCode(String emailCode)
    {
        this.emailCode = emailCode;
    }

    public String getPassword()
    {
        return password;
    }

    public void setPassword(String password)
    {
        this.password = password;
    }
}
