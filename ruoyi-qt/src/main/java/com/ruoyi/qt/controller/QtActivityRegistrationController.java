package com.ruoyi.qt.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.github.pagehelper.PageInfo;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.qt.domain.QtActivity;
import com.ruoyi.qt.domain.QtActivitySignup;
import com.ruoyi.qt.service.IQtActivityService;
import com.ruoyi.qt.service.IQtActivitySignupService;

@RestController
@RequestMapping("/qt/activity")
public class QtActivityRegistrationController extends BaseController
{
    @Autowired
    private IQtActivityService qtActivityService;

    @Autowired
    private IQtActivitySignupService qtActivitySignupService;

    @PreAuthorize("@ss.hasPermi('qt:activity:registrations')")
    @GetMapping("/lecture/registrations")
    public AjaxResult lecture(@RequestParam(value = "activityId", required = false) Long activityId)
    {
        return registrations("LECTURE", activityId);
    }

    @PreAuthorize("@ss.hasPermi('qt:activity:registrations')")
    @GetMapping("/sharing/registrations")
    public AjaxResult sharing(@RequestParam(value = "activityId", required = false) Long activityId)
    {
        return registrations("SHARING", activityId);
    }

    private AjaxResult registrations(String activityType, Long activityId)
    {
        QtActivity activity = resolveActivity(activityType, activityId);
        if (activity == null)
        {
            AjaxResult ajax = success();
            ajax.put("total", 0);
            ajax.put("quota", 0);
            ajax.put("rows", java.util.Collections.emptyList());
            ajax.put("activityId", null);
            ajax.put("activityTitle", null);
            return ajax;
        }
        QtActivitySignup query = new QtActivitySignup();
        query.setActivityId(activity.getActivityId());
        startPage();
        List<QtActivitySignup> rows = qtActivitySignupService.selectQtActivitySignupDetailList(query);
        AjaxResult ajax = success();
        ajax.put("total", new PageInfo<QtActivitySignup>(rows).getTotal());
        ajax.put("quota", activity.getCapacity() == null ? 0 : activity.getCapacity());
        ajax.put("rows", rows);
        ajax.put("activityId", activity.getActivityId());
        ajax.put("activityTitle", activity.getTitle());
        ajax.put("activityType", activity.getActivityType());
        return ajax;
    }

    private QtActivity resolveActivity(String activityType, Long activityId)
    {
        if (activityId != null)
        {
            QtActivity activity = qtActivityService.selectQtActivityByActivityId(activityId);
            if (activity != null && (StringUtils.isEmpty(activity.getActivityType())
                    || activityType.equals(activity.getActivityType())))
            {
                return activity;
            }
            return activity;
        }
        QtActivity query = new QtActivity();
        query.setActivityType(activityType);
        query.setStatus("PUBLISHED");
        List<QtActivity> list = qtActivityService.selectQtActivityList(query);
        if (list == null || list.isEmpty())
        {
            return null;
        }
        QtActivity latest = list.get(0);
        for (QtActivity item : list)
        {
            if (item.getActivityStart() != null && (latest.getActivityStart() == null
                    || item.getActivityStart().after(latest.getActivityStart())))
            {
                latest = item;
            }
        }
        return latest;
    }
}
