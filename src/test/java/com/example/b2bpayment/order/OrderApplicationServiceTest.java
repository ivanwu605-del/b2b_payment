package com.example.b2bpayment.order;

import com.example.b2bpayment.api.CreateOrderRequest;
import com.example.b2bpayment.db.OrderJdbcRepository;
import com.example.b2bpayment.db.OrderRecord;
import com.example.b2bpayment.merchant.MerchantApplicationService;
import com.example.b2bpayment.merchant.MerchantDisabledException;
import com.example.b2bpayment.merchant.MerchantNotFoundException;
import com.example.b2bpayment.payment.B2bPaymentRatioService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class OrderApplicationServiceTest {

    @Test
    void createOrderPersistsOrderAndRecordsRedisStatistic() {
        OrderJdbcRepository orderRepository = mock(OrderJdbcRepository.class);
        B2bPaymentRatioService ratioService = mock(B2bPaymentRatioService.class);
        MerchantApplicationService merchantService = mock(MerchantApplicationService.class);
        OrderApplicationService service = new OrderApplicationService(orderRepository, ratioService, merchantService);

        OrderRecord order = service.createOrder(new CreateOrderRequest("merchant-1", "out-1", 100L, "CNY"));

        assertEquals("merchant-1", order.merchantId());
        assertEquals("out-1", order.outTradeNo());
        assertEquals(100L, order.amount());
        assertEquals("CNY", order.currency());
        assertEquals("CREATED", order.status());
        assertNotNull(order.orderId());
        assertNotNull(order.createTime());
        assertNotNull(order.updateTime());
        verify(orderRepository).insert(order);
        verify(ratioService).onOrderCreated(order.orderId(), order.createTime());
    }

    @Test
    void createOrderFailsWhenMerchantDoesNotExist() {
        OrderJdbcRepository orderRepository = mock(OrderJdbcRepository.class);
        B2bPaymentRatioService ratioService = mock(B2bPaymentRatioService.class);
        MerchantApplicationService merchantService = mock(MerchantApplicationService.class);
        OrderApplicationService service = new OrderApplicationService(orderRepository, ratioService, merchantService);
        doThrow(new MerchantNotFoundException("missing")).when(merchantService).requireActiveMerchant("missing");

        assertThrows(MerchantNotFoundException.class, () ->
                service.createOrder(new CreateOrderRequest("missing", "out-1", 100L, "CNY"))
        );

        verifyNoInteractions(orderRepository, ratioService);
    }

    @Test
    void createOrderFailsWhenMerchantIsDisabled() {
        OrderJdbcRepository orderRepository = mock(OrderJdbcRepository.class);
        B2bPaymentRatioService ratioService = mock(B2bPaymentRatioService.class);
        MerchantApplicationService merchantService = mock(MerchantApplicationService.class);
        OrderApplicationService service = new OrderApplicationService(orderRepository, ratioService, merchantService);
        doThrow(new MerchantDisabledException("merchant-1")).when(merchantService).requireActiveMerchant("merchant-1");

        assertThrows(MerchantDisabledException.class, () ->
                service.createOrder(new CreateOrderRequest("merchant-1", "out-1", 100L, "CNY"))
        );

        verifyNoInteractions(orderRepository, ratioService);
    }
}
