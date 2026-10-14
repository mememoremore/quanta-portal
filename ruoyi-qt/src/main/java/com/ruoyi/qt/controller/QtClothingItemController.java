package com.ruoyi.qt.controller;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.file.FileUploadUtils;
import com.ruoyi.common.utils.file.FileUtils;
import com.ruoyi.common.utils.file.FileValidator;
import com.ruoyi.common.utils.file.MimeTypeUtils;
import com.ruoyi.qt.domain.QtClothingItem;
import com.ruoyi.qt.service.IQtClothingItemService;
import com.ruoyi.qt.util.QtAuthUtils;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.framework.config.ServerConfig;

/**
 * 服装配置Controller
 * 
 * @author ruoyi
 * @date 2026-04-24
 */
@RestController
@RequestMapping("/qt/item")
public class QtClothingItemController extends BaseController
{
    @Autowired
    private IQtClothingItemService qtClothingItemService;
    @Autowired
    private ServerConfig serverConfig;

    /**
     * 查询服装配置列表
     */
    // @PreAuthorize("@ss.hasPermi('qt:item:list')")
    @GetMapping("/list")
    public TableDataInfo list(QtClothingItem qtClothingItem)
    {
        QtAuthUtils.requireQuantaMember();
        startPage();
        List<QtClothingItem> list = qtClothingItemService.selectQtClothingItemList(qtClothingItem);
        fillImageUrl(list);
        return getDataTable(list);
    }

    /**
     * 导出服装配置列表
     */
    @PreAuthorize("@ss.hasPermi('qt:item:export')")
    @Log(title = "服装配置", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, QtClothingItem qtClothingItem)
    {
        List<QtClothingItem> list = qtClothingItemService.selectQtClothingItemList(qtClothingItem);
        ExcelUtil<QtClothingItem> util = new ExcelUtil<QtClothingItem>(QtClothingItem.class);
        util.exportExcel(response, list, "服装配置数据");
    }

    /**
     * 获取服装配置详细信息
     */
    // @PreAuthorize("@ss.hasPermi('qt:item:query')")
    @GetMapping(value = "/{itemId}")
    public AjaxResult getInfo(@PathVariable("itemId") Long itemId)
    {
        QtAuthUtils.requireQuantaMember();
        QtClothingItem item = qtClothingItemService.selectQtClothingItemByItemId(itemId);
        fillImageUrl(item);
        return success(item);
    }

    /**
     * 新增服装配置
     */
    @PreAuthorize("@ss.hasPermi('qt:item:add')")
    @Log(title = "服装配置", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(QtClothingItem qtClothingItem,
            @RequestParam(value = "effectImageFile", required = false) MultipartFile effectImageFile)
    {
        if (effectImageFile != null && !effectImageFile.isEmpty())
        {
            try
            {
                qtClothingItem.setEffectImagePath(uploadImage(effectImageFile));
            }
            catch (Exception e)
            {
                return AjaxResult.error(e.getMessage());
            }
        }
        return toAjax(qtClothingItemService.insertQtClothingItem(qtClothingItem));
    }

    /**
     * 修改服装配置
     */
    @PreAuthorize("@ss.hasPermi('qt:item:edit')")
    @Log(title = "服装配置", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(QtClothingItem qtClothingItem,
            @RequestParam(value = "effectImageFile", required = false) MultipartFile effectImageFile)
    {
        if (effectImageFile != null && !effectImageFile.isEmpty())
        {
            try
            {
                QtClothingItem oldItem = qtClothingItemService.selectQtClothingItemByItemId(qtClothingItem.getItemId());
                qtClothingItem.setEffectImagePath(uploadImage(effectImageFile));
                deleteLocalImageIfExists(oldItem == null ? null : oldItem.getEffectImagePath(), qtClothingItem.getEffectImagePath());
            }
            catch (Exception e)
            {
                return AjaxResult.error(e.getMessage());
            }
        }
        return toAjax(qtClothingItemService.updateQtClothingItem(qtClothingItem));
    }

    /**
     * 删除服装配置
     */
    @PreAuthorize("@ss.hasPermi('qt:item:remove')")
    @Log(title = "服装配置", businessType = BusinessType.DELETE)
	@DeleteMapping("/{itemIds}")
    public AjaxResult remove(@PathVariable Long[] itemIds)
    {
        return toAjax(qtClothingItemService.deleteQtClothingItemByItemIds(itemIds));
    }

    /**
     * 清空服装效果图（逻辑删除）
     */
    @PreAuthorize("@ss.hasPermi('qt:item:edit')")
    @Log(title = "服装配置", businessType = BusinessType.UPDATE)
    @DeleteMapping("/image/{itemId}")
    public AjaxResult removeImage(@PathVariable Long itemId)
    {
        QtClothingItem item = new QtClothingItem();
        item.setItemId(itemId);
        item.setEffectImagePath("");
        item.setUpdateBy(getUsername());
        return toAjax(qtClothingItemService.updateQtClothingItem(item));
    }

    private void fillImageUrl(List<QtClothingItem> list)
    {
        for (QtClothingItem item : list)
        {
            fillImageUrl(item);
        }
    }

    private void fillImageUrl(QtClothingItem item)
    {
        if (item == null || StringUtils.isEmpty(item.getEffectImagePath()))
        {
            return;
        }
        String imagePath = item.getEffectImagePath();
        if (imagePath.startsWith("http://") || imagePath.startsWith("https://"))
        {
            item.setEffectImageUrl(imagePath);
            return;
        }
        if (imagePath.startsWith(RuoYiConfig.getProfile()))
        {
            imagePath = imagePath.substring(RuoYiConfig.getProfile().length());
        }
        if (!imagePath.startsWith("/"))
        {
            imagePath = "/" + imagePath;
        }
        if (!imagePath.startsWith("/profile"))
        {
            imagePath = "/profile" + imagePath;
        }
        item.setEffectImageUrl(serverConfig.getUrl() + imagePath);
    }

    private String uploadImage(MultipartFile effectImageFile) throws Exception
    {
        String uploadPath = RuoYiConfig.getUploadPath() + "/qt/clothing-item";
        return FileUploadUtils.upload(uploadPath, effectImageFile, MimeTypeUtils.IMAGE_EXTENSION, FileValidator.SIZE_IMAGE);
    }

    private void deleteLocalImageIfExists(String oldImagePath, String newImagePath)
    {
        if (StringUtils.isEmpty(oldImagePath) || oldImagePath.startsWith("http://") || oldImagePath.startsWith("https://"))
        {
            return;
        }
        if (oldImagePath.equals(newImagePath))
        {
            return;
        }
        String physicalPath = RuoYiConfig.getProfile() + FileUtils.stripPrefix(oldImagePath);
        FileUtils.deleteFile(physicalPath);
    }
}
