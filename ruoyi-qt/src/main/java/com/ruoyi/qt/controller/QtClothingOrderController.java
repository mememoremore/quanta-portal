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
import org.springframework.validation.annotation.Validated;
import com.ruoyi.qt.validation.Create;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.annotation.RepeatSubmit;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.qt.domain.QtClothingOrder;
import com.ruoyi.qt.service.IQtClothingOrderService;
import com.ruoyi.qt.util.QtAuthUtils;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.framework.config.ServerConfig;
import com.ruoyi.framework.security.ProfileAccessSigner;
import com.ruoyi.common.utils.StringUtils;

/**
 * 实验室服装订单Controller
 */
@RestController
@RequestMapping("/qt/order")
public class QtClothingOrderController extends BaseController
{
    @Autowired
    private IQtClothingOrderService qtClothingOrderService;

    @Autowired
    private ServerConfig serverConfig;

    @Autowired
    private ProfileAccessSigner profileAccessSigner;

    @GetMapping("/list")
    public TableDataInfo list(QtClothingOrder qtClothingOrder)
    {
        QtAuthUtils.requireQuantaMember();
        QtAuthUtils.restrictToSelfIfNoAdmin(QtAuthUtils.PERM_ORDER_LIST, qtClothingOrder::setUserId);
        startPage();
        List<QtClothingOrder> list = qtClothingOrderService.selectQtClothingOrderList(qtClothingOrder);
        fillOrderAliases(list);
        return getDataTable(list);
    }

    @GetMapping("/detailList")
    public TableDataInfo detailList(QtClothingOrder qtClothingOrder)
    {
        QtAuthUtils.requireQuantaMember();
        QtAuthUtils.restrictToSelfIfNoAdmin(QtAuthUtils.PERM_ORDER_LIST, qtClothingOrder::setUserId);
        startPage();
        List<QtClothingOrder> list = qtClothingOrderService.selectQtClothingOrderDetailList(qtClothingOrder);
        fillOrderAliases(list);
        return getDataTable(list);
    }

    /**
     * 塔员「我的服务」：只查当前登录用户的塔服订单。
     */
    @GetMapping("/myDetailList")
    public TableDataInfo myDetailList(QtClothingOrder qtClothingOrder)
    {
        QtAuthUtils.requireQuantaMember();
        QtAuthUtils.restrictToSelf(qtClothingOrder::setUserId);
        startPage();
        List<QtClothingOrder> list = qtClothingOrderService.selectQtClothingOrderDetailList(qtClothingOrder);
        fillOrderAliases(list);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('qt:order:export')")
    @Log(title = "实验室服装订单", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, QtClothingOrder qtClothingOrder)
    {
        QtAuthUtils.restrictToSelfIfNoAdmin(QtAuthUtils.PERM_ORDER_LIST, qtClothingOrder::setUserId);
        List<QtClothingOrder> list = qtClothingOrderService.selectQtClothingOrderList(qtClothingOrder);
        ExcelUtil<QtClothingOrder> util = new ExcelUtil<QtClothingOrder>(QtClothingOrder.class);
        util.exportExcel(response, list, "实验室服装订单数据");
    }

    @GetMapping(value = "/{orderId}")
    public AjaxResult getInfo(@PathVariable("orderId") Long orderId)
    {
        QtAuthUtils.requireQuantaMember();
        QtClothingOrder order = qtClothingOrderService.selectQtClothingOrderByOrderId(orderId);
        if (order != null)
        {
            QtAuthUtils.assertOwnerOrAdmin(order.getUserId(), QtAuthUtils.PERM_ORDER_LIST);
            fillOrderAliases(java.util.Collections.singletonList(order));
        }
        return success(order);
    }

    @Log(title = "实验室服装订单", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @PostMapping
    public AjaxResult add(@Validated(Create.class) @RequestBody QtClothingOrder qtClothingOrder)
    {
        QtAuthUtils.requireQuantaMember();
        if (qtClothingOrder.getUserId() == null || !QtAuthUtils.hasAdminList(QtAuthUtils.PERM_ORDER_LIST))
        {
            qtClothingOrder.setUserId(getUserId());
        }
        qtClothingOrder.setCreateBy(getUsername());
        return toAjax(qtClothingOrderService.insertQtClothingOrder(qtClothingOrder));
    }

    @PreAuthorize("@ss.hasPermi('qt:order:edit')")
    @Log(title = "实验室服装订单", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody QtClothingOrder qtClothingOrder)
    {
        return toAjax(qtClothingOrderService.updateQtClothingOrder(qtClothingOrder));
    }

    @PreAuthorize("@ss.hasPermi('qt:order:approve')")
    @Log(title = "塔服确认收款", businessType = BusinessType.UPDATE)
    @PutMapping("/{orderId}/approve")
    public AjaxResult approve(@PathVariable Long orderId)
    {
        return toAjax(qtClothingOrderService.approveOrder(orderId, getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('qt:order:remove')")
    @Log(title = "实验室服装订单", businessType = BusinessType.DELETE)
    @DeleteMapping("/{orderIds}")
    public AjaxResult remove(@PathVariable Long[] orderIds)
    {
        return toAjax(qtClothingOrderService.deleteQtClothingOrderByOrderIds(orderIds));
    }

    private void fillOrderAliases(List<QtClothingOrder> list)
    {
        if (list == null)
        {
            return;
        }
        for (QtClothingOrder order : list)
        {
            order.setOrderTime(order.getCreateTime());
            if (StringUtils.isNotEmpty(order.getPaymentProofPath()))
            {
                String path = order.getPaymentProofPath();
                if (path.startsWith("http://") || path.startsWith("https://"))
                {
                    order.setPaymentProofUrl(profileAccessSigner.signUrl(path));
                }
                else
                {
                    order.setPaymentProofUrl(profileAccessSigner.signUrl(serverConfig.getUrl() + path));
                }
            }
            order.setPaymentProofPath(null);
        }
    }
}
