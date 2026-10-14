package com.ruoyi.qt.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 实验室图书对象 qt_book
 * 
 * @author ruoyi
 * @date 2026-04-24
 */
public class QtBook extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 图书ID */
    private Long bookId;

    /** ISBN */
    @Excel(name = "ISBN")
    private String isbn;

    /** 书名 */
    @Excel(name = "书名")
    private String bookName;

    /** 作者 */
    @Excel(name = "作者")
    private String author;

    /** 出版社 */
    @Excel(name = "出版社")
    private String publisher;

    /** 出版日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "出版日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date publishDate;

    /** 总库存 */
    @Excel(name = "总库存")
    private Long totalCount;

    /** 可借数量 */
    @Excel(name = "可借数量")
    private Long availableCount;

    /** 存放位置 */
    @Excel(name = "存放位置")
    private String locationDesc;

    /** 状态(0可借 1停借) */
    @Excel(name = "状态(0可借 1停借)")
    private String status;

    /** 图书类型 */
    @Excel(name = "图书类型")
    private String bookType;

    public void setBookId(Long bookId) 
    {
        this.bookId = bookId;
    }

    public Long getBookId() 
    {
        return bookId;
    }

    public void setIsbn(String isbn) 
    {
        this.isbn = isbn;
    }

    public String getIsbn() 
    {
        return isbn;
    }

    public void setBookName(String bookName) 
    {
        this.bookName = bookName;
    }

    public String getBookName() 
    {
        return bookName;
    }

    public void setAuthor(String author) 
    {
        this.author = author;
    }

    public String getAuthor() 
    {
        return author;
    }

    public void setPublisher(String publisher) 
    {
        this.publisher = publisher;
    }

    public String getPublisher() 
    {
        return publisher;
    }

    public void setPublishDate(Date publishDate) 
    {
        this.publishDate = publishDate;
    }

    public Date getPublishDate() 
    {
        return publishDate;
    }

    public void setTotalCount(Long totalCount) 
    {
        this.totalCount = totalCount;
    }

    public Long getTotalCount() 
    {
        return totalCount;
    }

    public void setAvailableCount(Long availableCount) 
    {
        this.availableCount = availableCount;
    }

    public Long getAvailableCount() 
    {
        return availableCount;
    }

    public void setLocationDesc(String locationDesc) 
    {
        this.locationDesc = locationDesc;
    }

    public String getLocationDesc() 
    {
        return locationDesc;
    }

    public void setStatus(String status) 
    {
        this.status = status;
    }

    public String getStatus()
    {
        return status;
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
            .append("bookId", getBookId())
            .append("isbn", getIsbn())
            .append("bookName", getBookName())
            .append("author", getAuthor())
            .append("publisher", getPublisher())
            .append("publishDate", getPublishDate())
            .append("totalCount", getTotalCount())
            .append("availableCount", getAvailableCount())
            .append("locationDesc", getLocationDesc())
            .append("status", getStatus())
            .append("bookType", getBookType())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
