package com.ruoyi.qt.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;
import jakarta.validation.constraints.NotNull;
import com.ruoyi.qt.validation.Create;

/**
 * 图书借阅记录对象 qt_book_borrow
 * 
 * @author ruoyi
 * @date 2026-04-24
 */
public class QtBookBorrow extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 借阅ID */
    private Long borrowId;

    /** 图书ID */
    @NotNull(groups = Create.class, message = "图书ID不能为空")
    @Excel(name = "图书ID")
    private Long bookId;

    /** 用户ID */
    @Excel(name = "用户ID")
    private Long userId;

    /** 借出时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "借出时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date borrowTime;

    /** 应还时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "应还时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date dueTime;

    /** 归还时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "归还时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date returnTime;

    /** 状态(BORROWED/RETURNED/OVERDUE) */
    @Excel(name = "状态(BORROWED/RETURNED/OVERDUE)")
    private String status;

    /** 图书名称 */
    @Excel(name = "图书名称")
    private String bookName;

    /** ISBN */
    @Excel(name = "ISBN")
    private String isbn;

    /** 作者 */
    @Excel(name = "作者")
    private String author;

    /** 用户账号 */
    @Excel(name = "用户账号")
    private String userName;

    /** 用户昵称 */
    @Excel(name = "用户昵称")
    private String nickName;

    /** 成员编号 */
    @Excel(name = "成员编号")
    private String memberNo;

    /** 成员部门 */
    @Excel(name = "成员部门")
    private String memberDepartment;

    /** 成员职称 */
    @Excel(name = "成员职称")
    private String memberTitle;

    /** 成员届次 */
    @Excel(name = "成员届次")
    private String memberCohort;

    /** 图书类型 */
    @Excel(name = "图书类型")
    private String bookType;

    public void setBorrowId(Long borrowId) 
    {
        this.borrowId = borrowId;
    }

    public Long getBorrowId() 
    {
        return borrowId;
    }

    public void setBookId(Long bookId) 
    {
        this.bookId = bookId;
    }

    public Long getBookId() 
    {
        return bookId;
    }

    public void setUserId(Long userId) 
    {
        this.userId = userId;
    }

    public Long getUserId() 
    {
        return userId;
    }

    public void setBorrowTime(Date borrowTime) 
    {
        this.borrowTime = borrowTime;
    }

    public Date getBorrowTime() 
    {
        return borrowTime;
    }

    public void setDueTime(Date dueTime) 
    {
        this.dueTime = dueTime;
    }

    public Date getDueTime() 
    {
        return dueTime;
    }

    public void setReturnTime(Date returnTime) 
    {
        this.returnTime = returnTime;
    }

    public Date getReturnTime() 
    {
        return returnTime;
    }

    public void setStatus(String status) 
    {
        this.status = status;
    }

    public String getStatus() 
    {
        return status;
    }

    public String getBookName()
    {
        return bookName;
    }

    public void setBookName(String bookName)
    {
        this.bookName = bookName;
    }

    public String getIsbn()
    {
        return isbn;
    }

    public void setIsbn(String isbn)
    {
        this.isbn = isbn;
    }

    public String getAuthor()
    {
        return author;
    }

    public void setAuthor(String author)
    {
        this.author = author;
    }

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

    public String getBookType()
    {
        return bookType;
    }

    public void setBookType(String bookType)
    {
        this.bookType = bookType;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("borrowId", getBorrowId())
            .append("bookId", getBookId())
            .append("userId", getUserId())
            .append("borrowTime", getBorrowTime())
            .append("dueTime", getDueTime())
            .append("returnTime", getReturnTime())
            .append("status", getStatus())
            .append("bookName", getBookName())
            .append("isbn", getIsbn())
            .append("author", getAuthor())
            .append("userName", getUserName())
            .append("nickName", getNickName())
            .append("memberNo", getMemberNo())
            .append("memberDepartment", getMemberDepartment())
            .append("memberTitle", getMemberTitle())
            .append("memberCohort", getMemberCohort())
            .append("remark", getRemark())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
