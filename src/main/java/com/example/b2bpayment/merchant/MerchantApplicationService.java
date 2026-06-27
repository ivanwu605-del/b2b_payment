package com.example.b2bpayment.merchant;

import com.example.b2bpayment.api.CreateMerchantRequest;
import com.example.b2bpayment.api.UpdateMerchantRequest;
import com.example.b2bpayment.db.MerchantJdbcRepository;
import com.example.b2bpayment.db.MerchantRecord;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class MerchantApplicationService {

    private final MerchantJdbcRepository merchantRepository;

    public MerchantApplicationService(MerchantJdbcRepository merchantRepository) {
        this.merchantRepository = merchantRepository;
    }

    public MerchantRecord createMerchant(CreateMerchantRequest request) {
        LocalDateTime now = LocalDateTime.now();
        MerchantRecord merchant = new MerchantRecord(
                "mch_" + UUID.randomUUID(),
                request.merchantName(),
                parseStatusOrDefault(request.status()).name(),
                now,
                now
        );
        merchantRepository.insert(merchant);
        return merchant;
    }

    public MerchantRecord getMerchant(String merchantId) {
        return merchantRepository.findByMerchantId(merchantId)
                .orElseThrow(() -> new MerchantNotFoundException(merchantId));
    }

    public List<MerchantRecord> listMerchants() {
        return merchantRepository.findAll();
    }

    public MerchantRecord updateMerchant(String merchantId, UpdateMerchantRequest request) {
        MerchantRecord existing = getMerchant(merchantId);
        MerchantRecord updated = new MerchantRecord(
                existing.merchantId(),
                request.merchantName(),
                parseStatus(request.status()).name(),
                existing.createTime(),
                LocalDateTime.now()
        );
        merchantRepository.update(updated);
        return updated;
    }

    public void deleteMerchant(String merchantId) {
        getMerchant(merchantId);
        if (merchantRepository.hasOrderOrPaymentReferences(merchantId)) {
            throw new MerchantReferencedException(merchantId);
        }
        merchantRepository.deleteByMerchantId(merchantId);
    }

    public MerchantRecord requireActiveMerchant(String merchantId) {
        MerchantRecord merchant = getMerchant(merchantId);
        if (!MerchantStatus.ACTIVE.name().equals(merchant.status())) {
            throw new MerchantDisabledException(merchantId);
        }
        return merchant;
    }

    private static MerchantStatus parseStatusOrDefault(String status) {
        if (status == null || status.isBlank()) {
            return MerchantStatus.ACTIVE;
        }
        return parseStatus(status);
    }

    private static MerchantStatus parseStatus(String status) {
        try {
            return MerchantStatus.valueOf(status);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new InvalidMerchantStatusException(status);
        }
    }
}
