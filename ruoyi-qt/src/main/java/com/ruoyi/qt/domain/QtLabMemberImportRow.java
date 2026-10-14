package com.ruoyi.qt.domain;

/**
 * One normalized lab-member row parsed from an import workbook.
 */
public class QtLabMemberImportRow
{
    private String userName;
    private String nickName;
    private String email;
    private String phonenumber;
    private String sex;
    private String status;
    private String memberNo;
    private String memberDepartment;
    private String memberTitle;
    private String memberCohort;
    private String studentNo;
    private String className;
    private String major;
    private String isQuantaMember;
    private String roleCategory;
    private Long deptId;
    private int sourceRow;

    public String getUserName()
    {
        return userName;
    }

    public void setUserName(String userName)
    {
        this.userName = userName;
    }

    public String getNickName()
    {
        return nickName;
    }

    public void setNickName(String nickName)
    {
        this.nickName = nickName;
    }

    public String getEmail()
    {
        return email;
    }

    public void setEmail(String email)
    {
        this.email = email;
    }

    public String getPhonenumber()
    {
        return phonenumber;
    }

    public void setPhonenumber(String phonenumber)
    {
        this.phonenumber = phonenumber;
    }

    public String getSex()
    {
        return sex;
    }

    public void setSex(String sex)
    {
        this.sex = sex;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
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

    public String getMajor()
    {
        return major;
    }

    public void setMajor(String major)
    {
        this.major = major;
    }

    public String getIsQuantaMember()
    {
        return isQuantaMember;
    }

    public void setIsQuantaMember(String isQuantaMember)
    {
        this.isQuantaMember = isQuantaMember;
    }

    public String getRoleCategory()
    {
        return roleCategory;
    }

    public void setRoleCategory(String roleCategory)
    {
        this.roleCategory = roleCategory;
    }

    public Long getDeptId()
    {
        return deptId;
    }

    public void setDeptId(Long deptId)
    {
        this.deptId = deptId;
    }

    public int getSourceRow()
    {
        return sourceRow;
    }

    public void setSourceRow(int sourceRow)
    {
        this.sourceRow = sourceRow;
    }
}
