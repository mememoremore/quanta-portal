package com.ruoyi.qt.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.qt.service.IQtDashboardService;

@RestController
@RequestMapping("/dashboard")
public class QtDashboardController extends BaseController
{
    @Autowired
    private IQtDashboardService qtDashboardService;

    @PreAuthorize("@ss.hasPermi('qt:dashboard:stats')")
    @GetMapping("/stats")
    public AjaxResult stats()
    {
        return success(qtDashboardService.selectStats());
    }
}
