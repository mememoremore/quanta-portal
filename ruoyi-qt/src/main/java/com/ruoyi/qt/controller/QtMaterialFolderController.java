package com.ruoyi.qt.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
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
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.qt.domain.QtMaterialFolder;
import com.ruoyi.qt.service.IQtMaterialFolderService;
import com.ruoyi.qt.util.QtAuthUtils;

@RestController
@RequestMapping("/qt/materials/folders")
public class QtMaterialFolderController extends BaseController
{
    @Autowired
    private IQtMaterialFolderService qtMaterialFolderService;

    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public AjaxResult list(@RequestParam String category, @RequestParam(defaultValue = "0") Long parentId)
    {
        QtAuthUtils.requireQuantaMember();
        List<QtMaterialFolder> folders = qtMaterialFolderService.listFolders(category, parentId);
        return success(folders);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{folderId:\\d+}")
    public AjaxResult detail(@PathVariable Long folderId)
    {
        QtAuthUtils.requireQuantaMember();
        return success(qtMaterialFolderService.getFolderDetail(folderId));
    }

    @PreAuthorize("@ss.hasPermi('qt:material:add')")
    @Log(title = "学习资料目录", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody QtMaterialFolder folder)
    {
        folder.setCreateBy(getUsername());
        return success(qtMaterialFolderService.createFolder(folder));
    }

    @PreAuthorize("@ss.hasPermi('qt:material:add')")
    @Log(title = "学习资料目录", businessType = BusinessType.UPDATE)
    @PutMapping("/{folderId}")
    public AjaxResult edit(@PathVariable Long folderId, @RequestBody QtMaterialFolder folder)
    {
        folder.setUpdateBy(getUsername());
        return success(qtMaterialFolderService.updateFolder(folderId, folder));
    }

    @PreAuthorize("@ss.hasPermi('qt:material:add')")
    @Log(title = "学习资料目录", businessType = BusinessType.DELETE)
    @DeleteMapping("/{folderId}")
    public AjaxResult remove(@PathVariable Long folderId)
    {
        qtMaterialFolderService.deleteFolder(folderId);
        return success();
    }
}
