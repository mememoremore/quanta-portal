package com.ruoyi.common.core.domain.model;

import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.utils.StringUtils;

/**
 * 登录结果
 *
 * @author ruoyi
 */
public class LoginResult
{
    /**
     * 令牌
     */
    private String token;

    /**
     * 是否塔员(0新生 1塔员)
     */
    private String isQuantaMember;

    /**
     * 成员编号
     */
    private String memberNo;

    /**
     * 部门(BACKEND/FRONTEND...)
     */
    private String memberDepartment;

    /**
     * 职称
     */
    private String memberTitle;

    /**
     * 届次(例如 20th)
     */
    private String memberCohort;

    /**
     * 学号
     */
    private String studentNo;

    /**
     * 班级
     */
    private String className;

    public LoginResult()
    {
    }

    public LoginResult(String token, String isQuantaMember)
    {
        this.token = token;
        this.isQuantaMember = isQuantaMember;
    }

    public LoginResult(String token, SysUser user)
    {
        this.token = token;
        if (user != null)
        {
            String member = user.getIsQuantaMember();
            this.isQuantaMember = StringUtils.isNotEmpty(member) ? member : "0";
            this.memberNo = user.getMemberNo();
            this.memberDepartment = user.getMemberDepartment();
            this.memberTitle = user.getMemberTitle();
            this.memberCohort = user.getMemberCohort();
            this.studentNo = user.getStudentNo();
            this.className = user.getClassName();
        }
    }

    public String getToken()
    {
        return token;
    }

    public void setToken(String token)
    {
        this.token = token;
    }

    public String getIsQuantaMember()
    {
        return isQuantaMember;
    }

    public void setIsQuantaMember(String isQuantaMember)
    {
        this.isQuantaMember = isQuantaMember;
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

    public String getStudentNo()
    {
        return studentNo;
    }

    public void setStudentNo(String studentNo)
    {
        this.studentNo = studentNo;
    }

    public String getClassName()
    {
        return className;
    }

    public void setClassName(String className)
    {
        this.className = className;
    }
}
