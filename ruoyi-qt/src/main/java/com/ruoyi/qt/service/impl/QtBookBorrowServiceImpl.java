package com.ruoyi.qt.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.qt.cache.QtQueryCache;
import com.ruoyi.qt.domain.QtBook;
import com.ruoyi.qt.domain.QtBookBorrow;
import com.ruoyi.qt.mapper.QtBookBorrowMapper;
import com.ruoyi.qt.mapper.QtBookMapper;
import com.ruoyi.qt.service.IQtBookBorrowService;

/**
 * 图书借阅记录Service业务层处理
 */
@Service
public class QtBookBorrowServiceImpl implements IQtBookBorrowService
{
    private static final String STATUS_BORROWED = "BORROWED";
    private static final String STATUS_RETURNED = "RETURNED";

    @Autowired
    private QtBookBorrowMapper qtBookBorrowMapper;

    @Autowired
    private QtBookMapper qtBookMapper;

    @Autowired
    private QtQueryCache qtQueryCache;

    private void evictBookCache()
    {
        qtQueryCache.evict(CacheConstants.CACHE_QT_BOOK_LIST);
    }

    @Override
    public QtBookBorrow selectQtBookBorrowByBorrowId(Long borrowId)
    {
        return qtBookBorrowMapper.selectQtBookBorrowByBorrowId(borrowId);
    }

    @Override
    public List<QtBookBorrow> selectQtBookBorrowList(QtBookBorrow qtBookBorrow)
    {
        return qtBookBorrowMapper.selectQtBookBorrowList(qtBookBorrow);
    }

    @Override
    public List<QtBookBorrow> selectQtBookBorrowDetailList(QtBookBorrow qtBookBorrow)
    {
        return qtBookBorrowMapper.selectQtBookBorrowDetailList(qtBookBorrow);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheConstants.CACHE_QT_BOOK_DETAIL, allEntries = true)
    public int insertQtBookBorrow(QtBookBorrow qtBookBorrow)
    {
        if (qtBookBorrow.getBookId() == null)
        {
            throw new ServiceException("bookId is required");
        }
        QtBook book = qtBookMapper.selectQtBookByBookIdForUpdate(qtBookBorrow.getBookId());
        if (book == null)
        {
            throw new ServiceException("图书不存在");
        }
        if ("1".equals(book.getStatus()))
        {
            throw new ServiceException("该图书已停借");
        }
        if (qtBookMapper.decrementAvailableCount(qtBookBorrow.getBookId()) == 0)
        {
            throw new ServiceException("暂无可借库存");
        }
        if (StringUtils.isEmpty(qtBookBorrow.getStatus()))
        {
            qtBookBorrow.setStatus(STATUS_BORROWED);
        }
        if (qtBookBorrow.getBorrowTime() == null)
        {
            qtBookBorrow.setBorrowTime(DateUtils.getNowDate());
        }
        qtBookBorrow.setCreateTime(DateUtils.getNowDate());
        int rows = qtBookBorrowMapper.insertQtBookBorrow(qtBookBorrow);
        evictBookCache();
        return rows;
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheConstants.CACHE_QT_BOOK_DETAIL, allEntries = true)
    public int updateQtBookBorrow(QtBookBorrow qtBookBorrow)
    {
        QtBookBorrow old = qtBookBorrowMapper.selectQtBookBorrowByBorrowId(qtBookBorrow.getBorrowId());
        qtBookBorrow.setUpdateTime(DateUtils.getNowDate());
        int rows = qtBookBorrowMapper.updateQtBookBorrow(qtBookBorrow);
        if (old != null && isOutstanding(old.getStatus()) && STATUS_RETURNED.equals(effectiveStatus(qtBookBorrow, old)))
        {
            qtBookMapper.incrementAvailableCount(old.getBookId());
        }
        evictBookCache();
        return rows;
    }

    @Override
    @Transactional
    public int deleteQtBookBorrowByBorrowIds(Long[] borrowIds)
    {
        if (borrowIds == null)
        {
            return 0;
        }
        int rows = 0;
        for (Long borrowId : borrowIds)
        {
            rows += deleteQtBookBorrowByBorrowId(borrowId);
        }
        return rows;
    }

    @Override
    @Transactional
    public int deleteQtBookBorrowByBorrowId(Long borrowId)
    {
        QtBookBorrow old = qtBookBorrowMapper.selectQtBookBorrowByBorrowId(borrowId);
        int rows = qtBookBorrowMapper.deleteQtBookBorrowByBorrowId(borrowId);
        if (rows > 0 && old != null && isOutstanding(old.getStatus()))
        {
            qtBookMapper.incrementAvailableCount(old.getBookId());
            evictBookCache();
        }
        return rows;
    }

    private static boolean isOutstanding(String status)
    {
        return !STATUS_RETURNED.equals(status);
    }

    private static String effectiveStatus(QtBookBorrow incoming, QtBookBorrow old)
    {
        return StringUtils.isNotEmpty(incoming.getStatus()) ? incoming.getStatus() : old.getStatus();
    }
}
