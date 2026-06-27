package com.example.b2bpayment.api;

import com.example.b2bpayment.db.OrderRecord;
import com.example.b2bpayment.order.OrderApplicationService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderControllerTest {

    @Test
    void createOrderReturnsCreatedOrder() throws Exception {
        OrderApplicationService service = mock(OrderApplicationService.class);
        LocalDateTime now = LocalDateTime.of(2026, 6, 27, 16, 0);
        when(service.createOrder(any(CreateOrderRequest.class))).thenReturn(new OrderRecord(
                "ord_1",
                "merchant-1",
                "out-1",
                100L,
                "CNY",
                "CREATED",
                now,
                now
        ));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new OrderController(service))
                .build();

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "merchantId": "merchant-1",
                                  "outTradeNo": "out-1",
                                  "amount": 100,
                                  "currency": "CNY"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value("ord_1"))
                .andExpect(jsonPath("$.merchantId").value("merchant-1"))
                .andExpect(jsonPath("$.status").value("CREATED"));
    }
}
