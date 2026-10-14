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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.qt.cache.QtCacheKeys;
import com.ruoyi.qt.cache.QtQueryCache;
import com.ruoyi.qt.domain.QtWorkstation;
import com.ruoyi.qt.service.IQtWorkstationService;
import com.ruoyi.qt.util.QtAuthUtils;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 实验室工位Controller
 * 
 * @author ruoyi
 * @date 2026-04-24
 */
@RestController
@RequestMapping("/qt/workstation")
public class QtWorkstationController extends BaseController
{
    @Autowired
    private IQtWorkstationService qtWorkstationService;

    @Autowired
    private QtQueryCache qtQueryCache;

    /**
     * 查询实验室工位列表
     */
    // @PreAuthorize("@ss.hasPermi('qt:workstation:list')")
    @GetMapping("/list")
    public TableDataInfo list(QtWorkstation qtWorkstation)
    {
        QtAuthUtils.requireQuantaMember();
        return qtQueryCache.loadPage(CacheConstants.CACHE_QT_WORKSTATION_LIST, QtCacheKeys.list(qtWorkstation),
                CacheConstants.TTL_QT_WORKSTATION_LIST, QtWorkstation.class, () -> {
                    startPage();
                    List<QtWorkstation> list = qtWorkstationService.selectQtWorkstationList(qtWorkstation);
                    return getDataTable(list);
                });
    }

    /**
     * 导出实验室工位列表
     */
    @PreAuthorize("@ss.hasPermi('qt:workstation:export')")
    @Log(title = "实验室工位", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, QtWorkstation qtWorkstation)
    {
        List<QtWorkstation> list = qtWorkstationService.selectQtWorkstationList(qtWorkstation);
        ExcelUtil<QtWorkstation> util = new ExcelUtil<QtWorkstation>(QtWorkstation.class);
        util.exportExcel(response, list, "实验室工位数据");
    }

    /**
     * 获取实验室工位详细信息
     */
    // @PreAuthorize("@ss.hasPermi('qt:workstation:query')")
    @GetMapping(value = "/{workstationId}")
    public AjaxResult getInfo(@PathVariable("workstationId") Long workstationId)
    {
        QtAuthUtils.requireQuantaMember();
        return success(qtWorkstationService.selectQtWorkstationByWorkstationId(workstationId));
    }

    /**
     * 新增实验室工位
     */
    @PreAuthorize("@ss.hasPermi('qt:workstation:add')")
    @Log(title = "实验室工位", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody QtWorkstation qtWorkstation)
    {
        return toAjax(qtWorkstationService.insertQtWorkstation(qtWorkstation));
    }

    /**
     * 修改实验室工位
     */
    @PreAuthorize("@ss.hasPermi('qt:workstation:edit')")
    @Log(title = "实验室工位", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody QtWorkstation qtWorkstation)
    {
        return toAjax(qtWorkstationService.updateQtWorkstation(qtWorkstation));
    }

    /**
     * 删除实验室工位
     */
    @PreAuthorize("@ss.hasPermi('qt:workstation:remove')")
    @Log(title = "实验室工位", businessType = BusinessType.DELETE)
	@DeleteMapping("/{workstationIds}")
    public AjaxResult remove(@PathVariable Long[] workstationIds)
    {
        return toAjax(qtWorkstationService.deleteQtWorkstationByWorkstationIds(workstationIds));
    }
}
