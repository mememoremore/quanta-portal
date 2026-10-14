package com.ruoyi.qt.controller;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.file.FileUploadUtils;
import com.ruoyi.common.utils.file.FileUtils;
import com.ruoyi.common.utils.file.FileValidator;
import com.ruoyi.common.utils.file.MimeTypeUtils;
import com.ruoyi.qt.domain.QtMaterial;
import com.ruoyi.qt.service.IQtMaterialService;
import com.ruoyi.qt.util.QtAuthUtils;

/**
 * 学习资料。列表/详情/下载：登录即可（塔员均可）；增删改需要 qt:material:*。
 */
@RestController
@RequestMapping("/qt/materials")
public class QtMaterialController extends BaseController
{
    @Autowired
    private IQtMaterialService qtMaterialService;

    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public TableDataInfo list(QtMaterial query)
    {
        QtAuthUtils.requireQuantaMember();
        startPage();
        List<QtMaterial> list = qtMaterialService.selectMaterialList(query);
        fillDownloadUrl(list);
        return getDataTable(list);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{materialId:\\d+}")
    public AjaxResult getInfo(@PathVariable Long materialId)
    {
        QtAuthUtils.requireQuantaMember();
        QtMaterial material = qtMaterialService.selectMaterialById(materialId);
        if (material == null)
        {
            return error("资料不存在或已删除");
        }
        fillDownloadUrl(java.util.Collections.singletonList(material));
        return success(material);
    }

    @PreAuthorize("@ss.hasPermi('qt:material:add')")
    @Log(title = "学习资料", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(QtMaterial material, @RequestParam("file") MultipartFile file) throws Exception
    {
        if (file == null || file.isEmpty())
        {
            return error("请选择要上传的文件");
        }
        String uploadPath = RuoYiConfig.getUploadPath() + "/qt/materials";
        String stored = FileUploadUtils.upload(uploadPath, file, MimeTypeUtils.DEFAULT_ALLOWED_EXTENSION,
                FileValidator.SIZE_DOCUMENT);
        material.setFileName(file.getOriginalFilename());
        material.setStoredName(FileUtils.getName(stored));
        material.setFilePath(stored);
        material.setFileSize(file.getSize());
        material.setUploaderId(getUserId());
        material.setCreateBy(getUsername());
        qtMaterialService.insertMaterial(material);
        fillDownloadUrl(java.util.Collections.singletonList(material));
        return success(material);
    }

    @PreAuthorize("@ss.hasPermi('qt:material:edit')")
    @Log(title = "学习资料", businessType = BusinessType.UPDATE)
    @PutMapping("/{materialId}")
    public AjaxResult edit(@PathVariable Long materialId, @RequestBody QtMaterial material)
    {
        material.setMaterialId(materialId);
        material.setUpdateBy(getUsername());
        return toAjax(qtMaterialService.updateMaterial(material));
    }

    @PreAuthorize("@ss.hasPermi('qt:material:remove')")
    @Log(title = "学习资料", businessType = BusinessType.DELETE)
    @DeleteMapping("/{materialId}")
    public AjaxResult remove(@PathVariable Long materialId)
    {
        QtMaterial material = qtMaterialService.selectMaterialById(materialId);
        if (material == null)
        {
            return error("资料不存在或已删除");
        }
        int rows = qtMaterialService.deleteMaterialById(materialId);
        try
        {
            String local = RuoYiConfig.getProfile() + FileUtils.stripPrefix(material.getFilePath());
            FileUtils.deleteFile(local);
        }
        catch (Exception ignored)
        {
        }
        return toAjax(rows);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{materialId}/download")
    public void download(@PathVariable Long materialId, HttpServletResponse response) throws Exception
    {
        QtAuthUtils.requireQuantaMember();
        QtMaterial material = qtMaterialService.selectMaterialById(materialId);
        if (material == null)
        {
            throw new ServiceException("资料不存在或已删除");
        }
        String local = RuoYiConfig.getProfile() + FileUtils.stripPrefix(material.getFilePath());
        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        FileUtils.setAttachmentResponseHeader(response, material.getFileName());
        FileUtils.writeBytes(local, response.getOutputStream());
    }

    private void fillDownloadUrl(List<QtMaterial> list)
    {
        if (list == null)
        {
            return;
        }
        for (QtMaterial material : list)
        {
            material.setDownloadUrl("/qt/materials/" + material.getMaterialId() + "/download");
            material.setFilePath(null);
        }
    }
}
