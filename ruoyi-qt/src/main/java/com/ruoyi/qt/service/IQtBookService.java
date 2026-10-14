package com.ruoyi.qt.service;

import java.util.List;
import com.ruoyi.qt.domain.QtBook;

/**
 * 实验室图书Service接口
 * 
 * @author ruoyi
 * @date 2026-04-24
 */
public interface IQtBookService 
{
    /**
     * 查询实验室图书
     * 
     * @param bookId 实验室图书主键
     * @return 实验室图书
     */
    public QtBook selectQtBookByBookId(Long bookId);

    /**
     * 查询实验室图书列表
     * 
     * @param qtBook 实验室图书
     * @return 实验室图书集合
     */
    public List<QtBook> selectQtBookList(QtBook qtBook);

    /**
     * 新增实验室图书
     * 
     * @param qtBook 实验室图书
     * @return 结果
     */
    public int insertQtBook(QtBook qtBook);

    /**
     * 修改实验室图书
     * 
     * @param qtBook 实验室图书
     * @return 结果
     */
    public int updateQtBook(QtBook qtBook);

    /**
     * 批量删除实验室图书
     * 
     * @param bookIds 需要删除的实验室图书主键集合
     * @return 结果
     */
    public int deleteQtBookByBookIds(Long[] bookIds);

    /**
     * 删除实验室图书信息
     * 
     * @param bookId 实验室图书主键
     * @return 结果
     */
    public int deleteQtBookByBookId(Long bookId);
}
