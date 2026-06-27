package com.example.demo.order;

import com.example.demo.api.CreateOrderRequest;
import com.example.demo.db.OrderJdbcRepository;
import com.example.demo.db.OrderRecord;
import com.example.demo.payment.B2bPaymentRatioService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class OrderApplicationServiceTest {

    @Test
    void createOrderPersistsOrderAndRecordsRedisStatistic() {
        OrderJdbcRepository orderRepository = mock(OrderJdbcRepository.class);
        B2bPaymentRatioService ratioService = mock(B2bPaymentRatioService.class);
        OrderApplicationService service = new OrderApplicationService(orderRepository, ratioService);

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
}
