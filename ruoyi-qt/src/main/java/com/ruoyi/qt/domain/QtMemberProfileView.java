package com.ruoyi.qt.domain;

import java.util.List;

/**
 * 成员档案 API 响应（本人/公开由服务端裁剪字段）
 */
public class QtMemberProfileView
{
    private Long userId;
    private String nickName;
    private String realName;
    private String communityNickname;
    private String avatar;
    private String memberNo;
    private String memberDepartment;
    private String memberTitle;
    private String memberCohort;
    private String headline;
    private String bio;
    private List<String> skills;
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

    public String getNickName()
    {
        return nickName;
    }

    public void setNickName(String nickName)
    {
        this.nickName = nickName;
    }

    public String getRealName()
    {
        return realName;
    }

    public void setRealName(String realName)
    {
        this.realName = realName;
    }

    public String getCommunityNickname()
    {
        return communityNickname;
    }

    public void setCommunityNickname(String communityNickname)
    {
        this.communityNickname = communityNickname;
    }

    public String getAvatar()
    {
        return avatar;
    }

    public void setAvatar(String avatar)
    {
        this.avatar = avatar;
    }

    public String getMemberNo()
    {
        return memberNo;
    }

    public void setMemberNo(String memberNo)
    {
        this.memberNo = memberNo;
    }

    public String getMemberDepartment()
    {
        return memberDepartment;
    }

    public void setMemberDepartment(String memberDepartment)
    {
        this.memberDepartment = memberDepartment;
    }

    public String getMemberTitle()
    {
        return memberTitle;
    }

    public void setMemberTitle(String memberTitle)
    {
        this.memberTitle = memberTitle;
    }

    public String getMemberCohort()
    {
        return memberCohort;
    }

    public void setMemberCohort(String memberCohort)
    {
        this.memberCohort = memberCohort;
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
