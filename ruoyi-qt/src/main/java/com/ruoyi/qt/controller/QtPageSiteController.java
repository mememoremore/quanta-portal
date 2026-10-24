package com.ruoyi.qt.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.qt.domain.QtPageDeployment;
import com.ruoyi.qt.domain.QtPageSiteCreateBody;
import com.ruoyi.qt.domain.QtPageSiteSaveResult;
import com.ruoyi.qt.domain.QtPageSiteUpdateBody;
import com.ruoyi.qt.service.IQtPageDeployService;
import com.ruoyi.qt.service.IQtPageSiteService;

/**
 * 成员静态站点（Quanta Pages）本人接口。
 *
 * 全部要求有效塔员身份，且只能操作本人站点；错误由 QtPagesExceptionHandler
 * 映射为真实 HTTP 状态 + AjaxResult.code。未登录仍由 Spring Security 拦截。
 */
@RestController
@RequestMapping("/qt/pages/sites")
public class QtPageSiteController extends BaseController
{
    @Autowired
    private IQtPageSiteService siteService;

    @Autowired
    private IQtPageDeployService deployService;

    /**
     * 创建站点并入队首次部署。
     */
    @PostMapping
    public AjaxResult create(@RequestBody QtPageSiteCreateBody body)
    {
        QtPageSiteSaveResult result = siteService.create(body);
        AjaxResult ajax = AjaxResult.success();
        ajax.put("data", result);
        return ajax;
    }

    /**
     * 本人站点；尚未创建时 data 为 null。
     */
    @GetMapping("/me")
    public AjaxResult mySite()
    {
        return AjaxResult.success(siteService.getOwnSite());
    }

    /**
     * 更新仓库/分支/发布目录；旧在线版本继续提供服务。
     */
    @PutMapping("/{siteId:\\d+}")
    public AjaxResult update(@PathVariable("siteId") Long siteId, @RequestBody QtPageSiteUpdateBody body)
    {
        QtPageSiteSaveResult result = siteService.update(siteId, body);
        AjaxResult ajax = AjaxResult.success();
        ajax.put("data", result);
        return ajax;
    }

    /**
     * 部署分支当前最新提交；请求快速返回，归档处理全部在后台。
     */
    @PostMapping("/{siteId:\\d+}/deploy")
    public AjaxResult deploy(@PathVariable("siteId") Long siteId)
    {
        QtPageDeployment deployment = deployService.requestDeploy(siteId, QtPageDeployment.TRIGGER_MANUAL);
        return AjaxResult.success(deployment);
    }
}
