package com.example.b2bpayment.api;

import com.example.b2bpayment.db.MerchantRecord;
import com.example.b2bpayment.merchant.MerchantApplicationService;
import com.example.b2bpayment.merchant.MerchantNotFoundException;
import com.example.b2bpayment.merchant.MerchantReferencedException;
import com.example.b2bpayment.merchant.MerchantStatus;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MerchantControllerTest {

    @Test
    void createMerchantReturnsCreatedMerchant() throws Exception {
        MerchantApplicationService service = mock(MerchantApplicationService.class);
        when(service.createMerchant(any(CreateMerchantRequest.class)))
                .thenReturn(merchant("mch_1", "Acme", MerchantStatus.ACTIVE));

        mockMvc(service).perform(post("/merchants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "merchantName": "Acme"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.merchantId").value("mch_1"))
                .andExpect(jsonPath("$.merchantName").value("Acme"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void getMerchantReturnsNotFoundWhenMissing() throws Exception {
        MerchantApplicationService service = mock(MerchantApplicationService.class);
        when(service.getMerchant("missing")).thenThrow(new MerchantNotFoundException("missing"));

        mockMvc(service).perform(get("/merchants/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Merchant not found: missing"));
    }

    @Test
    void listMerchantsReturnsAllMerchants() throws Exception {
        MerchantApplicationService service = mock(MerchantApplicationService.class);
        when(service.listMerchants()).thenReturn(List.of(merchant("mch_1", "Acme", MerchantStatus.ACTIVE)));

        mockMvc(service).perform(get("/merchants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].merchantId").value("mch_1"));
    }

    @Test
    void updateMerchantReturnsUpdatedMerchant() throws Exception {
        MerchantApplicationService service = mock(MerchantApplicationService.class);
        when(service.updateMerchant(any(String.class), any(UpdateMerchantRequest.class)))
                .thenReturn(merchant("mch_1", "New Name", MerchantStatus.DISABLED));

        mockMvc(service).perform(put("/merchants/mch_1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "merchantName": "New Name",
                                  "status": "DISABLED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.merchantName").value("New Name"))
                .andExpect(jsonPath("$.status").value("DISABLED"));
    }

    @Test
    void deleteMerchantReturnsNoContent() throws Exception {
        MerchantApplicationService service = mock(MerchantApplicationService.class);

        mockMvc(service).perform(delete("/merchants/mch_1"))
                .andExpect(status().isNoContent());

        verify(service).deleteMerchant("mch_1");
    }

    @Test
    void deleteMerchantReturnsBadRequestWhenReferenced() throws Exception {
        MerchantApplicationService service = mock(MerchantApplicationService.class);
        org.mockito.Mockito.doThrow(new MerchantReferencedException("mch_1"))
                .when(service).deleteMerchant("mch_1");

        mockMvc(service).perform(delete("/merchants/mch_1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Merchant is referenced by orders or payments: mch_1"));
    }

    private static MockMvc mockMvc(MerchantApplicationService service) {
        return MockMvcBuilders.standaloneSetup(new MerchantController(service))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    private static MerchantRecord merchant(String merchantId, String merchantName, MerchantStatus status) {
        LocalDateTime now = LocalDateTime.of(2026, 6, 27, 18, 0);
        return new MerchantRecord(merchantId, merchantName, status.name(), now, now);
    }
}
