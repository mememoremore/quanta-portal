package com.ruoyi.qt.mapper;

import java.util.List;
import com.ruoyi.qt.domain.QtBookBorrow;

/**
 * 图书借阅记录Mapper接口
 * 
 * @author ruoyi
 * @date 2026-04-24
 */
public interface QtBookBorrowMapper 
{
    /**
     * 查询图书借阅记录
     * 
     * @param borrowId 图书借阅记录主键
     * @return 图书借阅记录
     */
    public QtBookBorrow selectQtBookBorrowByBorrowId(Long borrowId);

    /**
     * 查询图书借阅记录列表
     * 
     * @param qtBookBorrow 图书借阅记录
     * @return 图书借阅记录集合
     */
    public List<QtBookBorrow> selectQtBookBorrowList(QtBookBorrow qtBookBorrow);

    /**
     * 查询图书借阅记录详情列表
     *
     * @param qtBookBorrow 图书借阅记录
     * @return 图书借阅记录集合
     */
    public List<QtBookBorrow> selectQtBookBorrowDetailList(QtBookBorrow qtBookBorrow);

    /**
     * 新增图书借阅记录
     * 
     * @param qtBookBorrow 图书借阅记录
     * @return 结果
     */
    public int insertQtBookBorrow(QtBookBorrow qtBookBorrow);

    /**
     * 修改图书借阅记录
     * 
     * @param qtBookBorrow 图书借阅记录
     * @return 结果
     */
    public int updateQtBookBorrow(QtBookBorrow qtBookBorrow);

    /**
     * 删除图书借阅记录
     * 
     * @param borrowId 图书借阅记录主键
     * @return 结果
     */
    public int deleteQtBookBorrowByBorrowId(Long borrowId);

    /**
     * 批量删除图书借阅记录
     * 
     * @param borrowIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteQtBookBorrowByBorrowIds(Long[] borrowIds);
}
