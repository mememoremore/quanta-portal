package com.ruoyi.qt.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 面试投递主表对象 qt_interview_application
 */
public class QtInterviewApplication extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long applicationId;
    private Long userId;
    @Excel(name = "姓名")
    private String realName;
    @Excel(name = "性别", readConverterExp = "0=男,1=女,2=未知")
    private String gender;
    @Excel(name = "班级")
    private String className;
    @Excel(name = "第一志愿")
    private String firstChoice;
    @Excel(name = "第二志愿")
    private String secondChoice;
    private String photoUrl;
    private String resumeUrl;
    private String resumeFileName;
    @Excel(name = "投递状态")
    private String applyStatus;
    private String photoAccessUrl;
    private String resumeAccessUrl;
    @Excel(name = "录用部门")
    private String offeredDepartment;
    @Excel(name = "入职确认")
    private String joinStatus;
    @Excel(name = "最终状态")
    private String finalStatus;
    private String noticeStatus;
    @Excel(name = "学号")
    private String studentNo;
    @Excel(name = "电话")
    private String phonenumber;
    @Excel(name = "邮箱")
    private String email;
    @Excel(name = "专业")
    private String major;
    @Excel(name = "昵称")
    private String nickName;
    @Excel(name = "第一志愿轮次结果")
    private String firstChoiceStatus;
    @Excel(name = "第二志愿轮次结果")
    private String secondChoiceStatus;
    private String firstChoiceFirstRoundStatus;
    private String firstChoiceSecondRoundStatus;
    private String secondChoiceFirstRoundStatus;
    private String secondChoiceSecondRoundStatus;
    private BigDecimal firstChoiceSecondRoundScore;
    private BigDecimal secondChoiceSecondRoundScore;
    private String firstChoiceSecondRoundScoreUpdateBy;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date firstChoiceSecondRoundScoreUpdateTime;
    private String secondChoiceSecondRoundScoreUpdateBy;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date secondChoiceSecondRoundScoreUpdateTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date firstRoundInterviewTime;
    /** 查询用：数据范围部门 */
    private String scopedDepartment;
    /** 查询用：二面排序部门，空则按已晋级志愿最高分 */
    private String sortDepartment;
    /** 查询用：轮次 */
    private Long roundId;
    /** 查询用：志愿部门筛选 */
    private String department;
    /** 查询用：轮次结果状态 */
    private String resultStatus;

    public Long getApplicationId()
    {
        return applicationId;
    }

    public void setApplicationId(Long applicationId)
    {
        this.applicationId = applicationId;
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public String getRealName()
    {
        return realName;
    }

    public void setRealName(String realName)
    {
        this.realName = realName;
    }

    public String getGender()
    {
        return gender;
    }

    public void setGender(String gender)
    {
        this.gender = gender;
    }

    public String getClassName()
    {
        return className;
    }

    public void setClassName(String className)
    {
        this.className = className;
    }

    public String getFirstChoice()
    {
        return firstChoice;
    }

    public void setFirstChoice(String firstChoice)
    {
        this.firstChoice = firstChoice;
    }

    public String getSecondChoice()
    {
        return secondChoice;
    }

    public void setSecondChoice(String secondChoice)
    {
        this.secondChoice = secondChoice;
    }

    public String getPhotoUrl()
    {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl)
    {
        this.photoUrl = photoUrl;
    }

    public String getResumeUrl()
    {
        return resumeUrl;
    }

    public void setResumeUrl(String resumeUrl)
    {
        this.resumeUrl = resumeUrl;
    }

    public String getResumeFileName()
    {
        return resumeFileName;
    }

    public void setResumeFileName(String resumeFileName)
    {
        this.resumeFileName = resumeFileName;
    }

    public String getApplyStatus()
    {
        return applyStatus;
    }

    public void setApplyStatus(String applyStatus)
    {
        this.applyStatus = applyStatus;
    }

    public String getPhotoAccessUrl()
    {
        return photoAccessUrl;
    }

    public void setPhotoAccessUrl(String photoAccessUrl)
    {
        this.photoAccessUrl = photoAccessUrl;
    }

    public String getResumeAccessUrl()
    {
        return resumeAccessUrl;
    }

    public void setResumeAccessUrl(String resumeAccessUrl)
    {
        this.resumeAccessUrl = resumeAccessUrl;
    }

    public String getOfferedDepartment()
    {
        return offeredDepartment;
    }

    public void setOfferedDepartment(String offeredDepartment)
    {
        this.offeredDepartment = offeredDepartment;
    }

    public String getJoinStatus()
    {
        return joinStatus;
    }

    public void setJoinStatus(String joinStatus)
    {
        this.joinStatus = joinStatus;
    }

    public String getFinalStatus()
    {
        return finalStatus;
    }

    public void setFinalStatus(String finalStatus)
    {
        this.finalStatus = finalStatus;
    }

    public String getNoticeStatus()
    {
        return noticeStatus;
    }

    public void setNoticeStatus(String noticeStatus)
    {
        this.noticeStatus = noticeStatus;
    }

    public String getStudentNo()
    {
        return studentNo;
    }

    public void setStudentNo(String studentNo)
    {
        this.studentNo = studentNo;
    }

    public String getPhonenumber()
    {
        return phonenumber;
    }

    public void setPhonenumber(String phonenumber)
    {
        this.phonenumber = phonenumber;
    }

    public String getEmail()
    {
        return email;
    }

    public void setEmail(String email)
    {
        this.email = email;
    }

    public String getMajor()
    {
        return major;
    }

    public void setMajor(String major)
    {
        this.major = major;
    }

    public String getNickName()
    {
        return nickName;
    }

    public void setNickName(String nickName)
    {
        this.nickName = nickName;
    }

    public String getFirstChoiceStatus()
    {
        return firstChoiceStatus;
    }

    public void setFirstChoiceStatus(String firstChoiceStatus)
    {
        this.firstChoiceStatus = firstChoiceStatus;
    }

    public String getSecondChoiceStatus()
    {
        return secondChoiceStatus;
    }

    public void setSecondChoiceStatus(String secondChoiceStatus)
    {
        this.secondChoiceStatus = secondChoiceStatus;
    }

    public String getFirstChoiceFirstRoundStatus()
    {
        return firstChoiceFirstRoundStatus;
    }

    public void setFirstChoiceFirstRoundStatus(String firstChoiceFirstRoundStatus)
    {
        this.firstChoiceFirstRoundStatus = firstChoiceFirstRoundStatus;
    }

    public String getFirstChoiceSecondRoundStatus()
    {
        return firstChoiceSecondRoundStatus;
    }

    public void setFirstChoiceSecondRoundStatus(String firstChoiceSecondRoundStatus)
    {
        this.firstChoiceSecondRoundStatus = firstChoiceSecondRoundStatus;
    }

    public String getSecondChoiceFirstRoundStatus()
    {
        return secondChoiceFirstRoundStatus;
    }

    public void setSecondChoiceFirstRoundStatus(String secondChoiceFirstRoundStatus)
    {
        this.secondChoiceFirstRoundStatus = secondChoiceFirstRoundStatus;
    }

    public String getSecondChoiceSecondRoundStatus()
    {
        return secondChoiceSecondRoundStatus;
    }

    public void setSecondChoiceSecondRoundStatus(String secondChoiceSecondRoundStatus)
    {
        this.secondChoiceSecondRoundStatus = secondChoiceSecondRoundStatus;
    }

    public BigDecimal getFirstChoiceSecondRoundScore()
    {
        return firstChoiceSecondRoundScore;
    }

    public void setFirstChoiceSecondRoundScore(BigDecimal firstChoiceSecondRoundScore)
    {
        this.firstChoiceSecondRoundScore = firstChoiceSecondRoundScore;
    }

    public BigDecimal getSecondChoiceSecondRoundScore()
    {
        return secondChoiceSecondRoundScore;
    }

    public void setSecondChoiceSecondRoundScore(BigDecimal secondChoiceSecondRoundScore)
    {
        this.secondChoiceSecondRoundScore = secondChoiceSecondRoundScore;
    }

    public String getFirstChoiceSecondRoundScoreUpdateBy()
    {
        return firstChoiceSecondRoundScoreUpdateBy;
    }

    public void setFirstChoiceSecondRoundScoreUpdateBy(String firstChoiceSecondRoundScoreUpdateBy)
    {
        this.firstChoiceSecondRoundScoreUpdateBy = firstChoiceSecondRoundScoreUpdateBy;
    }

    public Date getFirstChoiceSecondRoundScoreUpdateTime()
    {
        return firstChoiceSecondRoundScoreUpdateTime;
    }

    public void setFirstChoiceSecondRoundScoreUpdateTime(Date firstChoiceSecondRoundScoreUpdateTime)
    {
        this.firstChoiceSecondRoundScoreUpdateTime = firstChoiceSecondRoundScoreUpdateTime;
    }

    public String getSecondChoiceSecondRoundScoreUpdateBy()
    {
        return secondChoiceSecondRoundScoreUpdateBy;
    }

    public void setSecondChoiceSecondRoundScoreUpdateBy(String secondChoiceSecondRoundScoreUpdateBy)
    {
        this.secondChoiceSecondRoundScoreUpdateBy = secondChoiceSecondRoundScoreUpdateBy;
    }

    public Date getSecondChoiceSecondRoundScoreUpdateTime()
    {
        return secondChoiceSecondRoundScoreUpdateTime;
    }

    public void setSecondChoiceSecondRoundScoreUpdateTime(Date secondChoiceSecondRoundScoreUpdateTime)
    {
        this.secondChoiceSecondRoundScoreUpdateTime = secondChoiceSecondRoundScoreUpdateTime;
    }

    public Date getFirstRoundInterviewTime()
    {
        return firstRoundInterviewTime;
    }

    public void setFirstRoundInterviewTime(Date firstRoundInterviewTime)
    {
        this.firstRoundInterviewTime = firstRoundInterviewTime;
    }

    public String getSortDepartment()
    {
        return sortDepartment;
    }

    public void setSortDepartment(String sortDepartment)
    {
        this.sortDepartment = sortDepartment;
    }

    public String getScopedDepartment()
    {
        return scopedDepartment;
    }

    public void setScopedDepartment(String scopedDepartment)
    {
        this.scopedDepartment = scopedDepartment;
    }

    public Long getRoundId()
    {
        return roundId;
    }

    public void setRoundId(Long roundId)
    {
        this.roundId = roundId;
    }

    public String getDepartment()
    {
        return department;
    }

    public void setDepartment(String department)
    {
        this.department = department;
    }

    public String getResultStatus()
    {
        return resultStatus;
    }

    public void setResultStatus(String resultStatus)
    {
        this.resultStatus = resultStatus;
    }
}
