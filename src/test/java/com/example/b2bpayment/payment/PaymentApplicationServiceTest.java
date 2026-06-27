package com.example.b2bpayment.payment;

import com.example.b2bpayment.api.CreatePaymentRequest;
import com.example.b2bpayment.db.OrderJdbcRepository;
import com.example.b2bpayment.db.OrderRecord;
import com.example.b2bpayment.db.PaymentJdbcRepository;
import com.example.b2bpayment.db.PaymentRecord;
import com.example.b2bpayment.merchant.MerchantApplicationService;
import com.example.b2bpayment.merchant.MerchantDisabledException;
import com.example.b2bpayment.merchant.MerchantNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PaymentApplicationServiceTest {

    private OrderJdbcRepository orderRepository;
    private PaymentJdbcRepository paymentRepository;
    private B2bPaymentRatioService ratioService;
    private MerchantApplicationService merchantService;
    private PaymentApplicationService service;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderJdbcRepository.class);
        paymentRepository = mock(PaymentJdbcRepository.class);
        ratioService = mock(B2bPaymentRatioService.class);
        merchantService = mock(MerchantApplicationService.class);
        service = new PaymentApplicationService(orderRepository, paymentRepository, ratioService, merchantService);
    }

    @Test
    void createSuccessfulPaymentPersistsPaymentAndRecordsRedisStatistic() {
        OrderRecord order = orderWithMerchant("merchant-1");
        when(orderRepository.findByOrderId("order-1")).thenReturn(Optional.of(order));

        PaymentRecord payment = service.createPayment(
                new CreatePaymentRequest("order-1", "merchant-1", "WECHAT", PaymentJdbcRepository.STATUS_SUCCESS)
        );

        assertNotNull(payment.transactionId());
        assertEquals("order-1", payment.orderId());
        assertEquals("merchant-1", payment.merchantId());
        assertEquals("WECHAT", payment.channel());
        assertEquals(PaymentJdbcRepository.STATUS_SUCCESS, payment.status());
        assertNotNull(payment.createTime());
        assertNotNull(payment.updateTime());
        verify(paymentRepository).insert(payment);
        verify(ratioService).onPaymentSuccess(payment.transactionId(), payment.createTime());
    }

    @Test
    void createPaymentFailsWhenOrderDoesNotExist() {
        when(orderRepository.findByOrderId("missing")).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> service.createPayment(
                new CreatePaymentRequest("missing", "merchant-1", "WECHAT", PaymentJdbcRepository.STATUS_SUCCESS)
        ));

        verifyNoInteractions(paymentRepository);
    }

    @Test
    void createPaymentFailsWhenMerchantDoesNotMatchOrder() {
        when(orderRepository.findByOrderId("order-1")).thenReturn(Optional.of(orderWithMerchant("merchant-2")));

        assertThrows(MerchantMismatchException.class, () -> service.createPayment(
                new CreatePaymentRequest("order-1", "merchant-1", "WECHAT", PaymentJdbcRepository.STATUS_SUCCESS)
        ));

        verifyNoInteractions(paymentRepository);
    }

    @Test
    void createPaymentFailsWhenMerchantDoesNotExist() {
        doThrow(new MerchantNotFoundException("missing")).when(merchantService).requireActiveMerchant("missing");

        assertThrows(MerchantNotFoundException.class, () -> service.createPayment(
                new CreatePaymentRequest("order-1", "missing", "WECHAT", PaymentJdbcRepository.STATUS_SUCCESS)
        ));

        verifyNoInteractions(orderRepository, paymentRepository);
    }

    @Test
    void createPaymentFailsWhenMerchantIsDisabled() {
        doThrow(new MerchantDisabledException("merchant-1")).when(merchantService).requireActiveMerchant("merchant-1");

        assertThrows(MerchantDisabledException.class, () -> service.createPayment(
                new CreatePaymentRequest("order-1", "merchant-1", "WECHAT", PaymentJdbcRepository.STATUS_SUCCESS)
        ));

        verifyNoInteractions(orderRepository, paymentRepository);
    }

    private static OrderRecord orderWithMerchant(String merchantId) {
        LocalDateTime now = LocalDateTime.now();
        return new OrderRecord(
                "order-1",
                merchantId,
                "out-1",
                100L,
                "CNY",
                "CREATED",
                now,
                now
        );
    }
}
