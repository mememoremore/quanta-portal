package com.ruoyi.qt.domain;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 面试投递扩展信息对象 qt_interview_profile
 */
public class QtInterviewProfile extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long profileId;
    private Long userId;
    private Long applicationId;
    private String selfIntro;
    private String codingExperience;
    private String codingExperienceDesc;
    private String quantaUnderstanding;

    public Long getProfileId()
    {
        return profileId;
    }

    public void setProfileId(Long profileId)
    {
        this.profileId = profileId;
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public Long getApplicationId()
    {
        return applicationId;
    }

    public void setApplicationId(Long applicationId)
    {
        this.applicationId = applicationId;
    }

    public String getSelfIntro()
    {
        return selfIntro;
    }

    public void setSelfIntro(String selfIntro)
    {
        this.selfIntro = selfIntro;
    }

    public String getCodingExperience()
    {
        return codingExperience;
    }

    public void setCodingExperience(String codingExperience)
    {
        this.codingExperience = codingExperience;
    }

    public String getCodingExperienceDesc()
    {
        return codingExperienceDesc;
    }

    public void setCodingExperienceDesc(String codingExperienceDesc)
    {
        this.codingExperienceDesc = codingExperienceDesc;
    }

    public String getQuantaUnderstanding()
    {
        return quantaUnderstanding;
    }

    public void setQuantaUnderstanding(String quantaUnderstanding)
    {
        this.quantaUnderstanding = quantaUnderstanding;
    }
}
