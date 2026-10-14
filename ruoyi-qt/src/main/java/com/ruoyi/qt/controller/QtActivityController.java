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
import com.ruoyi.qt.domain.QtActivity;
import com.ruoyi.qt.service.IQtActivityService;
import com.ruoyi.qt.util.QtAuthUtils;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 实验室活动Controller
 * 
 * @author ruoyi
 * @date 2026-04-24
 */
@RestController
@RequestMapping("/qt/activity")
public class QtActivityController extends BaseController
{
    @Autowired
    private IQtActivityService qtActivityService;

    @Autowired
    private QtQueryCache qtQueryCache;

    /**
     * 查询实验室活动列表
     */
    @GetMapping("/list")
    public TableDataInfo list(QtActivity qtActivity)
    {
        if (!QtAuthUtils.hasAdminList(QtAuthUtils.PERM_ACTIVITY_LIST))
        {
            qtActivity.setStatus("PUBLISHED");
        }
        // 非管理员被强制 status=PUBLISHED，指纹含 status，因此不会串到管理员的未发布数据
        return qtQueryCache.loadPage(CacheConstants.CACHE_QT_ACTIVITY_LIST, QtCacheKeys.list(qtActivity),
                CacheConstants.TTL_QT_ACTIVITY_LIST, QtActivity.class, () -> {
                    startPage();
                    List<QtActivity> list = qtActivityService.selectQtActivityList(qtActivity);
                    return getDataTable(list);
                });
    }

    /**
     * 导出实验室活动列表
     */
    @PreAuthorize("@ss.hasPermi('qt:activity:export')")
    @Log(title = "实验室活动", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, QtActivity qtActivity)
    {
        List<QtActivity> list = qtActivityService.selectQtActivityList(qtActivity);
        ExcelUtil<QtActivity> util = new ExcelUtil<QtActivity>(QtActivity.class);
        util.exportExcel(response, list, "实验室活动数据");
    }

    /**
     * 获取实验室活动详细信息
     */
    @GetMapping(value = "/{activityId}")
    public AjaxResult getInfo(@PathVariable("activityId") Long activityId)
    {
        QtActivity activity = qtActivityService.selectQtActivityByActivityId(activityId);
        if (activity != null && !QtAuthUtils.hasAdminList(QtAuthUtils.PERM_ACTIVITY_LIST)
                && !"PUBLISHED".equals(activity.getStatus()))
        {
            return error("活动不存在或未发布");
        }
        return success(activity);
    }

    /**
     * 新增实验室活动
     */
    @PreAuthorize("@ss.hasPermi('qt:activity:add')")
    @Log(title = "实验室活动", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody QtActivity qtActivity)
    {
        if (qtActivity.getCreatorUserId() == null)
        {
            qtActivity.setCreatorUserId(getUserId());
        }
        if (qtActivity.getCreateBy() == null || qtActivity.getCreateBy().isEmpty())
        {
            qtActivity.setCreateBy(getUsername());
        }
        return toAjax(qtActivityService.insertQtActivity(qtActivity));
    }

    /**
     * 修改实验室活动
     */
    @PreAuthorize("@ss.hasPermi('qt:activity:edit')")
    @Log(title = "实验室活动", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody QtActivity qtActivity)
    {
        return toAjax(qtActivityService.updateQtActivity(qtActivity));
    }

    /**
     * 删除实验室活动
     */
    @PreAuthorize("@ss.hasPermi('qt:activity:remove')")
    @Log(title = "实验室活动", businessType = BusinessType.DELETE)
	@DeleteMapping("/{activityIds}")
    public AjaxResult remove(@PathVariable Long[] activityIds)
    {
        return toAjax(qtActivityService.deleteQtActivityByActivityIds(activityIds));
    }
}
