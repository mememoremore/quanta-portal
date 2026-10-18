package com.ruoyi.qt.domain;

/**
 * CEO 成员维护请求。真实姓名沿用前端契约字段 nickName，服务端写入 real_name。
 */
public class QtMemberManageBody
{
    private String mode;
    private String version;
    private String memberDepartment;
    private String memberTitle;
    private String memberCohort;
    private String roleCategory;
    private String nickName;
    private String userName;
    private String email;

    public String getMode()
    {
        return mode;
    }

    public void setMode(String mode)
    {
        this.mode = mode;
    }

    public String getVersion()
    {
        return version;
    }

    public void setVersion(String version)
    {
        this.version = version;
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

    public String getRoleCategory()
    {
        return roleCategory;
    }

    public void setRoleCategory(String roleCategory)
    {
        this.roleCategory = roleCategory;
    }

    public String getNickName()
    {
        return nickName;
    }

    public void setNickName(String nickName)
    {
        this.nickName = nickName;
    }

    public String getUserName()
    {
        return userName;
    }

    public void setUserName(String userName)
    {
        this.userName = userName;
    }

    public String getEmail()
    {
        return email;
    }

    public void setEmail(String email)
    {
        this.email = email;
    }
}
