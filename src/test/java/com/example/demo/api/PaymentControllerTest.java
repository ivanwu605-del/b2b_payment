package com.example.demo.api;

import com.example.demo.db.PaymentJdbcRepository;
import com.example.demo.db.PaymentRecord;
import com.example.demo.payment.MerchantMismatchException;
import com.example.demo.payment.OrderNotFoundException;
import com.example.demo.payment.PaymentApplicationService;
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

class PaymentControllerTest {

    @Test
    void createPaymentReturnsCreatedPayment() throws Exception {
        PaymentApplicationService service = mock(PaymentApplicationService.class);
        LocalDateTime now = LocalDateTime.of(2026, 6, 27, 16, 0);
        when(service.createPayment(any(CreatePaymentRequest.class))).thenReturn(new PaymentRecord(
                "pay_1",
                "ord_1",
                "merchant-1",
                "WECHAT",
                PaymentJdbcRepository.STATUS_SUCCESS,
                now,
                now
        ));
        MockMvc mockMvc = mockMvc(service);

        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderId": "ord_1",
                                  "merchantId": "merchant-1",
                                  "channel": "WECHAT",
                                  "status": "SUCCESS"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionId").value("pay_1"))
                .andExpect(jsonPath("$.orderId").value("ord_1"))
                .andExpect(jsonPath("$.status").value(PaymentJdbcRepository.STATUS_SUCCESS));
    }

    @Test
    void createPaymentReturnsNotFoundWhenOrderDoesNotExist() throws Exception {
        PaymentApplicationService service = mock(PaymentApplicationService.class);
        when(service.createPayment(any(CreatePaymentRequest.class))).thenThrow(new OrderNotFoundException("missing"));
        MockMvc mockMvc = mockMvc(service);

        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderId": "missing",
                                  "merchantId": "merchant-1",
                                  "channel": "WECHAT",
                                  "status": "SUCCESS"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Order not found: missing"));
    }

    @Test
    void createPaymentReturnsBadRequestWhenMerchantDoesNotMatch() throws Exception {
        PaymentApplicationService service = mock(PaymentApplicationService.class);
        when(service.createPayment(any(CreatePaymentRequest.class)))
                .thenThrow(new MerchantMismatchException("ord_1", "merchant-2"));
        MockMvc mockMvc = mockMvc(service);

        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderId": "ord_1",
                                  "merchantId": "merchant-2",
                                  "channel": "WECHAT",
                                  "status": "SUCCESS"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Merchant does not match order ord_1: merchant-2"));
    }

    private static MockMvc mockMvc(PaymentApplicationService service) {
        return MockMvcBuilders.standaloneSetup(new PaymentController(service))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }
}
