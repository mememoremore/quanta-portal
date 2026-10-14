package com.ruoyi.qt.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.qt.cache.QtQueryCache;
import com.ruoyi.qt.domain.QtBook;
import com.ruoyi.qt.domain.QtBookBorrow;
import com.ruoyi.qt.mapper.QtBookBorrowMapper;
import com.ruoyi.qt.mapper.QtBookMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QtBookBorrowServiceImplTest
{
    @Mock
    private QtBookBorrowMapper qtBookBorrowMapper;

    @Mock
    private QtBookMapper qtBookMapper;

    @Mock
    private QtQueryCache qtQueryCache;

    @InjectMocks
    private QtBookBorrowServiceImpl service;

    @Test
    void insertDecrementsStock()
    {
        QtBook book = new QtBook();
        book.setBookId(1L);
        book.setStatus("0");
        when(qtBookMapper.selectQtBookByBookIdForUpdate(1L)).thenReturn(book);
        when(qtBookMapper.decrementAvailableCount(1L)).thenReturn(1);
        when(qtBookBorrowMapper.insertQtBookBorrow(any())).thenReturn(1);

        QtBookBorrow borrow = new QtBookBorrow();
        borrow.setBookId(1L);
        assertEquals(1, service.insertQtBookBorrow(borrow));
        verify(qtBookMapper).decrementAvailableCount(1L);
    }

    @Test
    void insertFailsWhenNoStock()
    {
        QtBook book = new QtBook();
        book.setBookId(1L);
        book.setStatus("0");
        when(qtBookMapper.selectQtBookByBookIdForUpdate(1L)).thenReturn(book);
        when(qtBookMapper.decrementAvailableCount(1L)).thenReturn(0);

        QtBookBorrow borrow = new QtBookBorrow();
        borrow.setBookId(1L);
        assertThrows(ServiceException.class, () -> service.insertQtBookBorrow(borrow));
        verify(qtBookBorrowMapper, never()).insertQtBookBorrow(any());
    }

    @Test
    void returnIncrementsStock()
    {
        QtBookBorrow old = new QtBookBorrow();
        old.setBorrowId(3L);
        old.setBookId(1L);
        old.setStatus("BORROWED");
        when(qtBookBorrowMapper.selectQtBookBorrowByBorrowId(3L)).thenReturn(old);
        when(qtBookBorrowMapper.updateQtBookBorrow(any())).thenReturn(1);

        QtBookBorrow incoming = new QtBookBorrow();
        incoming.setBorrowId(3L);
        incoming.setStatus("RETURNED");
        service.updateQtBookBorrow(incoming);
        verify(qtBookMapper).incrementAvailableCount(1L);
    }
}
