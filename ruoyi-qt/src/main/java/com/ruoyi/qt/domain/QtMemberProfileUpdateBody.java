package com.ruoyi.qt.domain;

import java.util.List;

/**
 * 本人可编辑的云端档案字段
 */
public class QtMemberProfileUpdateBody
{
    private String communityNickname;
    private String headline;
    private String bio;
    private List<String> skills;
    private String githubUrl;
    private String websiteUrl;
    private String contactEmail;
    private Boolean showContactEmail;

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

    public List<String> getSkills()
    {
        return skills;
    }

    public void setSkills(List<String> skills)
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

    public Boolean getShowContactEmail()
    {
        return showContactEmail;
    }

    public void setShowContactEmail(Boolean showContactEmail)
    {
        this.showContactEmail = showContactEmail;
    }
}
