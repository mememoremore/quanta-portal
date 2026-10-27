package com.ruoyi.qt.domain;

/**
 * Pages 开放 API 的站主公开资料查询结果。
 *
 * 只承载可公开展示的字段；不包含实名、学号、部门、登录邮箱等敏感信息。
 */
public class QtPagesPublicProfile
{
    private Long userId;

    private String avatar;

    private String communityNickname;

    private String headline;

    private String bio;

    private String skills;

    private String githubUrl;

    private String websiteUrl;

    private String contactEmail;

    private String showContactEmail;

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public String getAvatar()
    {
        return avatar;
    }

    public void setAvatar(String avatar)
    {
        this.avatar = avatar;
    }

    public String getCommunityNickname()
    {
        return communityNickname;
    }

    public void setCommunityNickname(String communityNickname)
    {
        this.communityNickname = communityNickname;
    }

    public String getHeadline()
    {
        return headline;
    }

    public void setHeadline(String headline)
    {
        this.headline = headline;
    }

    public String getBio()
    {
        return bio;
    }

    public void setBio(String bio)
    {
        this.bio = bio;
    }

    public String getSkills()
    {
        return skills;
    }

    public void setSkills(String skills)
    {
        this.skills = skills;
    }

    public String getGithubUrl()
    {
        return githubUrl;
    }

    public void setGithubUrl(String githubUrl)
    {
        this.githubUrl = githubUrl;
    }

    public String getWebsiteUrl()
    {
        return websiteUrl;
    }

    public void setWebsiteUrl(String websiteUrl)
    {
        this.websiteUrl = websiteUrl;
    }

    public String getContactEmail()
    {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail)
    {
        this.contactEmail = contactEmail;
    }

    public String getShowContactEmail()
    {
        return showContactEmail;
    }

    public void setShowContactEmail(String showContactEmail)
    {
        this.showContactEmail = showContactEmail;
    }
}
