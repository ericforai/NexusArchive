package com.nexusarchive.controller;

import com.nexusarchive.common.result.Result;
import com.nexusarchive.dto.sip.AccountingSipDto;
import com.nexusarchive.entity.AbnormalVoucher;
import com.nexusarchive.service.AbnormalVoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/abnormal")
@RequiredArgsConstructor
@Validated
public class AbnormalVoucherController {

    private final AbnormalVoucherService abnormalVoucherService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public Result<List<AbnormalVoucher>> listPending() {
        return Result.success(abnormalVoucherService.getPendingAbnormals());
    }

    @PostMapping("/{id}/retry")
    @PreAuthorize("isAuthenticated()")
    public Result<Void> retry(@PathVariable String id) {
        if (id == null || id.isBlank()) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST, "id 不能为空");
        }
        abnormalVoucherService.retry(id);
        return Result.success();
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public Result<Void> updateSip(@PathVariable String id, @Valid @RequestBody AccountingSipDto sipDto) {
        if (id == null || id.isBlank()) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST, "id 不能为空");
        }
        abnormalVoucherService.updateSipData(id, sipDto);
        return Result.success();
    }
}
