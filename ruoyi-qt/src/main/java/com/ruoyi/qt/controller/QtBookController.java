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
import com.ruoyi.qt.domain.QtBook;
import com.ruoyi.qt.service.IQtBookService;
import com.ruoyi.qt.util.QtAuthUtils;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 实验室图书Controller
 * 
 * @author ruoyi
 * @date 2026-04-24
 */
@RestController
@RequestMapping("/qt/book")
public class QtBookController extends BaseController
{
    @Autowired
    private IQtBookService qtBookService;

    @Autowired
    private QtQueryCache qtQueryCache;

    /**
     * 查询实验室图书列表
     */
    // @PreAuthorize("@ss.hasPermi('qt:book:list')")
    @GetMapping("/list")
    public TableDataInfo list(QtBook qtBook)
    {
        QtAuthUtils.requireQuantaMember();
        // 小程序图书页一次拉全表本地分页，是站内最热的读；缓存整份已组装好的响应以保住 total
        return qtQueryCache.loadPage(CacheConstants.CACHE_QT_BOOK_LIST, QtCacheKeys.list(qtBook),
                CacheConstants.TTL_QT_BOOK_LIST, QtBook.class, () -> {
                    startPage();
                    List<QtBook> list = qtBookService.selectQtBookList(qtBook);
                    return getDataTable(list);
                });
    }

    /**
     * 导出实验室图书列表
     */
    @PreAuthorize("@ss.hasPermi('qt:book:export')")
    @Log(title = "实验室图书", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, QtBook qtBook)
    {
        List<QtBook> list = qtBookService.selectQtBookList(qtBook);
        ExcelUtil<QtBook> util = new ExcelUtil<QtBook>(QtBook.class);
        util.exportExcel(response, list, "实验室图书数据");
    }

    /**
     * 获取实验室图书详细信息
     */
    // @PreAuthorize("@ss.hasPermi('qt:book:query')")
    @GetMapping(value = "/{bookId}")
    public AjaxResult getInfo(@PathVariable("bookId") Long bookId)
    {
        QtAuthUtils.requireQuantaMember();
        return success(qtBookService.selectQtBookByBookId(bookId));
    }

    /**
     * 新增实验室图书
     */
    @PreAuthorize("@ss.hasPermi('qt:book:add')")
    @Log(title = "实验室图书", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody QtBook qtBook)
    {
        return toAjax(qtBookService.insertQtBook(qtBook));
    }

    /**
     * 修改实验室图书
     */
    @PreAuthorize("@ss.hasPermi('qt:book:edit')")
    @Log(title = "实验室图书", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody QtBook qtBook)
    {
        return toAjax(qtBookService.updateQtBook(qtBook));
    }

    /**
     * 删除实验室图书
     */
    @PreAuthorize("@ss.hasPermi('qt:book:remove')")
    @Log(title = "实验室图书", businessType = BusinessType.DELETE)
	@DeleteMapping("/{bookIds}")
    public AjaxResult remove(@PathVariable Long[] bookIds)
    {
        return toAjax(qtBookService.deleteQtBookByBookIds(bookIds));
    }
}
