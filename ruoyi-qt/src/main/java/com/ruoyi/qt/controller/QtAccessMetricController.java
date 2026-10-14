package com.ruoyi.qt.controller;

import java.util.Date;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.qt.domain.QtAccessMetric;
import com.ruoyi.qt.service.IQtAccessMetricService;

@RestController
@RequestMapping("/qt/metrics")
public class QtAccessMetricController extends BaseController
{
    @Autowired
    private IQtAccessMetricService qtAccessMetricService;

    @PreAuthorize("@ss.hasPermi('qt:metrics:list')")
    @GetMapping("/latest")
    public AjaxResult latest()
    {
        return success(qtAccessMetricService.latest());
    }

    @PreAuthorize("@ss.hasPermi('qt:metrics:list')")
    @GetMapping("/list")
    public AjaxResult list(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date beginTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date endTime)
    {
        List<QtAccessMetric> rows = qtAccessMetricService.selectGlobalList(beginTime, endTime);
        return success(rows);
    }

    @PreAuthorize("@ss.hasPermi('qt:metrics:list')")
    @GetMapping("/uris")
    public AjaxResult uris(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date beginTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date endTime)
    {
        List<QtAccessMetric> rows = qtAccessMetricService.selectUriAgg(beginTime, endTime);
        return success(rows);
    }
}
