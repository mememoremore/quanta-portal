package com.ruoyi.qt.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.RateLimiter;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.LimitType;
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
 * 手动入口按 USER 维度限流（每账号每 60 秒 10 次）。
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
    @RateLimiter(time = 60, count = 10, limitType = LimitType.USER, key = "qt_pages")
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
    @RateLimiter(time = 60, count = 10, limitType = LimitType.USER, key = "qt_pages")
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
    @RateLimiter(time = 60, count = 10, limitType = LimitType.USER, key = "qt_pages")
    @PostMapping("/{siteId:\\d+}/deploy")
    public AjaxResult deploy(@PathVariable("siteId") Long siteId)
    {
        QtPageDeployment deployment = deployService.requestDeploy(siteId, QtPageDeployment.TRIGGER_MANUAL);
        return AjaxResult.success(deployment);
    }

    /**
     * 部署历史列表；分页默认 20、最大 100。
     */
    @GetMapping("/{siteId:\\d+}/deployments")
    public TableDataInfo deployments(@PathVariable("siteId") Long siteId,
            @RequestParam(value = "pageNum", required = false) Integer pageNum,
            @RequestParam(value = "pageSize", required = false) Integer pageSize)
    {
        List<QtPageDeployment> list = deployService.listOwnedDeployments(siteId, pageNum, pageSize);
        return getDataTable(list);
    }

    /**
     * 部署详情（含阶段日志）；仅本人记录。
     */
    @GetMapping("/{siteId:\\d+}/deployments/{deploymentId:\\d+}")
    public AjaxResult deployment(@PathVariable("siteId") Long siteId, @PathVariable("deploymentId") Long deploymentId)
    {
        return AjaxResult.success(deployService.getOwnedDeployment(siteId, deploymentId));
    }

    /**
     * 手动回滚到本站成功且实体仍存在的版本。
     */
    @RateLimiter(time = 60, count = 10, limitType = LimitType.USER, key = "qt_pages")
    @PostMapping("/{siteId:\\d+}/rollback/{deploymentId:\\d+}")
    public AjaxResult rollback(@PathVariable("siteId") Long siteId, @PathVariable("deploymentId") Long deploymentId)
    {
        QtPageDeployment deployment = deployService.requestRollback(siteId, deploymentId);
        return AjaxResult.success(deployment);
    }
}
