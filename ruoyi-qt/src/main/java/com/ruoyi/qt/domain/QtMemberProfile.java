package com.ruoyi.qt.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 塔员云端个人档案 qt_member_profile
 */
public class QtMemberProfile extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long userId;
    private String communityNickname;
    private String headline;
    private String bio;
    /** JSON 数组文本 */
    private String skills;
    private String githubUrl;
    private String websiteUrl;
    private String contactEmail;
    private Boolean showContactEmail;

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
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

    public Boolean getShowContactEmail()
    {
        return showContactEmail;
    }

    public void setShowContactEmail(Boolean showContactEmail)
    {
        this.showContactEmail = showContactEmail;
    }

    @Override
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public Date getUpdateTime()
    {
        return super.getUpdateTime();
    }
}
