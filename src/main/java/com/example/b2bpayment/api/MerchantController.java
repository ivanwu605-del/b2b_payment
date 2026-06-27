package com.example.b2bpayment.api;

import com.example.b2bpayment.db.MerchantRecord;
import com.example.b2bpayment.merchant.MerchantApplicationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class MerchantController {

    private final MerchantApplicationService merchantService;

    public MerchantController(MerchantApplicationService merchantService) {
        this.merchantService = merchantService;
    }

    @PostMapping("/merchants")
    @ResponseStatus(HttpStatus.CREATED)
    public MerchantRecord createMerchant(@RequestBody CreateMerchantRequest request) {
        return merchantService.createMerchant(request);
    }

    @GetMapping("/merchants/{merchantId}")
    public MerchantRecord getMerchant(@PathVariable String merchantId) {
        return merchantService.getMerchant(merchantId);
    }

    @GetMapping("/merchants")
    public List<MerchantRecord> listMerchants() {
        return merchantService.listMerchants();
    }

    @PutMapping("/merchants/{merchantId}")
    public MerchantRecord updateMerchant(
            @PathVariable String merchantId,
            @RequestBody UpdateMerchantRequest request
    ) {
        return merchantService.updateMerchant(merchantId, request);
    }

    @DeleteMapping("/merchants/{merchantId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMerchant(@PathVariable String merchantId) {
        merchantService.deleteMerchant(merchantId);
    }
}
