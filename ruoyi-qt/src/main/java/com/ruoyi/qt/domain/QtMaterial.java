package com.ruoyi.qt.domain;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 学习资料 qt_material
 */
public class QtMaterial extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 四个标准分类：新上传资料仅接受这些值，categoryScope=STANDARD 时仅查询这些值 */
    public static final Set<String> STANDARD_CATEGORIES = Collections
            .unmodifiableSet(new HashSet<>(Arrays.asList("FRONTEND", "BACKEND", "PRODUCT", "DESIGN")));

    private Long materialId;
    private String fileName;
    private String storedName;
    private String filePath;
    private Long fileSize;
    private String category;
    /** ALL / MEMBER / DEPT */
    private String visibility;
    /** 查询参数：STANDARD 表示仅查四个标准分类（非表字段） */
    private String categoryScope;
    private Long uploaderId;
    private String uploaderName;
    private String uploaderDepartment;
    private String downloadUrl;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date uploadTime;

    public Long getMaterialId()
    {
        return materialId;
    }

    public void setMaterialId(Long materialId)
    {
        this.materialId = materialId;
    }

    public String getFileName()
    {
        return fileName;
    }

    public void setFileName(String fileName)
    {
        this.fileName = fileName;
    }

    public String getStoredName()
    {
        return storedName;
    }

    public void setStoredName(String storedName)
    {
        this.storedName = storedName;
    }

    public String getFilePath()
    {
        return filePath;
    }

    public void setFilePath(String filePath)
    {
        this.filePath = filePath;
    }

    public Long getFileSize()
    {
        return fileSize;
    }

    public void setFileSize(Long fileSize)
    {
        this.fileSize = fileSize;
    }

    public String getCategory()
    {
        return category;
    }

    public void setCategory(String category)
    {
        this.category = category;
    }

    public static boolean isStandardCategory(String category)
    {
        return category != null && STANDARD_CATEGORIES.contains(category);
    }

    public String getCategoryScope()
    {
        return categoryScope;
    }

    public void setCategoryScope(String categoryScope)
    {
        this.categoryScope = categoryScope;
    }

    public String getVisibility()
    {
        return visibility;
    }

    public void setVisibility(String visibility)
    {
        this.visibility = visibility;
    }

    public Long getUploaderId()
    {
        return uploaderId;
    }

    public void setUploaderId(Long uploaderId)
    {
        this.uploaderId = uploaderId;
    }

    public String getUploaderName()
    {
        return uploaderName;
    }

    public void setUploaderName(String uploaderName)
    {
        this.uploaderName = uploaderName;
    }

    public String getUploaderDepartment()
    {
        return uploaderDepartment;
    }

    public void setUploaderDepartment(String uploaderDepartment)
    {
        this.uploaderDepartment = uploaderDepartment;
    }

    public String getDownloadUrl()
    {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl)
    {
        this.downloadUrl = downloadUrl;
    }

    public Date getUploadTime()
    {
        return uploadTime != null ? uploadTime : getCreateTime();
    }

    public void setUploadTime(Date uploadTime)
    {
        this.uploadTime = uploadTime;
        setCreateTime(uploadTime);
    }
}
