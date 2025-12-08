package com.nexusarchive.controller;

import com.nexusarchive.common.result.Result;
import com.nexusarchive.dto.sip.AccountingSipDto;
import com.nexusarchive.entity.AbnormalVoucher;
import com.nexusarchive.service.AbnormalVoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/abnormal")
@RequiredArgsConstructor
public class AbnormalVoucherController {

    private final AbnormalVoucherService abnormalVoucherService;

    @GetMapping
    public Result<List<AbnormalVoucher>> listPending() {
        return Result.success(abnormalVoucherService.getPendingAbnormals());
    }

    @PostMapping("/{id}/retry")
    public Result<Void> retry(@PathVariable String id) {
        abnormalVoucherService.retry(id);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<Void> updateSip(@PathVariable String id, @RequestBody AccountingSipDto sipDto) {
        abnormalVoucherService.updateSipData(id, sipDto);
        return Result.success();
    }
}
