package com.example.b2bpayment.merchant;

import com.example.b2bpayment.api.CreateMerchantRequest;
import com.example.b2bpayment.api.UpdateMerchantRequest;
import com.example.b2bpayment.db.MerchantJdbcRepository;
import com.example.b2bpayment.db.MerchantRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class MerchantApplicationServiceTest {

    private MerchantJdbcRepository merchantRepository;
    private MerchantApplicationService service;

    @BeforeEach
    void setUp() {
        merchantRepository = mock(MerchantJdbcRepository.class);
        service = new MerchantApplicationService(merchantRepository);
    }

    @Test
    void createMerchantPersistsActiveMerchantByDefault() {
        MerchantRecord merchant = service.createMerchant(new CreateMerchantRequest("Acme", null));

        assertNotNull(merchant.merchantId());
        assertEquals("Acme", merchant.merchantName());
        assertEquals(MerchantStatus.ACTIVE.name(), merchant.status());
        assertNotNull(merchant.createTime());
        assertNotNull(merchant.updateTime());
        verify(merchantRepository).insert(merchant);
    }

    @Test
    void getMerchantReturnsExistingMerchant() {
        MerchantRecord merchant = merchant("mch_1", MerchantStatus.ACTIVE);
        when(merchantRepository.findByMerchantId("mch_1")).thenReturn(Optional.of(merchant));

        assertEquals(merchant, service.getMerchant("mch_1"));
    }

    @Test
    void getMerchantFailsWhenMissing() {
        when(merchantRepository.findByMerchantId("missing")).thenReturn(Optional.empty());

        assertThrows(MerchantNotFoundException.class, () -> service.getMerchant("missing"));
    }

    @Test
    void listMerchantsReturnsRepositoryResults() {
        List<MerchantRecord> merchants = List.of(merchant("mch_1", MerchantStatus.ACTIVE));
        when(merchantRepository.findAll()).thenReturn(merchants);

        assertEquals(merchants, service.listMerchants());
    }

    @Test
    void updateMerchantPersistsNewNameAndStatus() {
        MerchantRecord existing = merchant("mch_1", MerchantStatus.ACTIVE);
        when(merchantRepository.findByMerchantId("mch_1")).thenReturn(Optional.of(existing));

        MerchantRecord updated = service.updateMerchant("mch_1", new UpdateMerchantRequest("New Name", "DISABLED"));

        assertEquals("mch_1", updated.merchantId());
        assertEquals("New Name", updated.merchantName());
        assertEquals(MerchantStatus.DISABLED.name(), updated.status());
        verify(merchantRepository).update(updated);
    }

    @Test
    void deleteMerchantFailsWhenReferenced() {
        when(merchantRepository.findByMerchantId("mch_1")).thenReturn(Optional.of(merchant("mch_1", MerchantStatus.ACTIVE)));
        when(merchantRepository.hasOrderOrPaymentReferences("mch_1")).thenReturn(true);

        assertThrows(MerchantReferencedException.class, () -> service.deleteMerchant("mch_1"));

        verify(merchantRepository).findByMerchantId("mch_1");
        verify(merchantRepository).hasOrderOrPaymentReferences("mch_1");
        verifyNoMoreInteractions(merchantRepository);
    }

    @Test
    void deleteMerchantDeletesUnreferencedMerchant() {
        when(merchantRepository.findByMerchantId("mch_1")).thenReturn(Optional.of(merchant("mch_1", MerchantStatus.ACTIVE)));
        when(merchantRepository.hasOrderOrPaymentReferences("mch_1")).thenReturn(false);

        service.deleteMerchant("mch_1");

        verify(merchantRepository).deleteByMerchantId("mch_1");
    }

    @Test
    void requireActiveMerchantFailsWhenDisabled() {
        when(merchantRepository.findByMerchantId("mch_1")).thenReturn(Optional.of(merchant("mch_1", MerchantStatus.DISABLED)));

        assertThrows(MerchantDisabledException.class, () -> service.requireActiveMerchant("mch_1"));
    }

    private static MerchantRecord merchant(String merchantId, MerchantStatus status) {
        LocalDateTime now = LocalDateTime.now();
        return new MerchantRecord(merchantId, "Acme", status.name(), now, now);
    }
}
