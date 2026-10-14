package com.ruoyi.qt.service.impl;

import java.math.BigDecimal;
import java.util.Date;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.qt.domain.QtClothingOrder;
import com.ruoyi.qt.mapper.QtClothingOrderMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QtClothingOrderServiceImplTest
{
    @Mock
    private QtClothingOrderMapper qtClothingOrderMapper;

    @InjectMocks
    private QtClothingOrderServiceImpl service;

    @Test
    void insertIgnoresClientApprovedStatusAndPrices()
    {
        when(qtClothingOrderMapper.insertQtClothingOrder(any())).thenReturn(1);

        QtClothingOrder order = new QtClothingOrder();
        order.setOrderNo("CLIENT-NO");
        order.setStatus("APPROVED");
        order.setConfirmedBy("hacker");
        order.setConfirmedAt(new Date());
        order.setUnitPrice(new BigDecimal("1.00"));
        order.setTotalAmount(new BigDecimal("99.00"));

        assertEquals(1, service.insertQtClothingOrder(order));

        ArgumentCaptor<QtClothingOrder> captor = ArgumentCaptor.forClass(QtClothingOrder.class);
        verify(qtClothingOrderMapper).insertQtClothingOrder(captor.capture());
        QtClothingOrder saved = captor.getValue();
        assertEquals("DRAFT", saved.getStatus());
        assertNull(saved.getUnitPrice());
        assertNull(saved.getTotalAmount());
        assertNull(saved.getConfirmedBy());
        assertNull(saved.getConfirmedAt());
        assertNotEquals("CLIENT-NO", saved.getOrderNo());
        assertTrue(saved.getOrderNo().startsWith("QT"));
    }

    @Test
    void insertWithPaymentProofCapsStatusAtSubmitted()
    {
        when(qtClothingOrderMapper.insertQtClothingOrder(any())).thenReturn(1);

        QtClothingOrder order = new QtClothingOrder();
        order.setPaymentProofPath("/profile/upload/qt/payment-proof/a.png");
        order.setStatus("APPROVED");
        order.setConfirmedBy("hacker");
        order.setUnitPrice(new BigDecimal("1"));
        order.setTotalAmount(new BigDecimal("1"));

        service.insertQtClothingOrder(order);

        ArgumentCaptor<QtClothingOrder> captor = ArgumentCaptor.forClass(QtClothingOrder.class);
        verify(qtClothingOrderMapper).insertQtClothingOrder(captor.capture());
        QtClothingOrder saved = captor.getValue();
        assertEquals("SUBMITTED", saved.getStatus());
        assertNotNull(saved.getPaymentTime());
        assertNull(saved.getUnitPrice());
        assertNull(saved.getTotalAmount());
        assertNull(saved.getConfirmedBy());
    }

    @Test
    void updateCannotSetApproved()
    {
        QtClothingOrder order = new QtClothingOrder();
        order.setOrderId(1L);
        order.setStatus("APPROVED");

        assertThrows(ServiceException.class, () -> service.updateQtClothingOrder(order));
        verify(qtClothingOrderMapper, never()).updateQtClothingOrder(any());
    }
}
