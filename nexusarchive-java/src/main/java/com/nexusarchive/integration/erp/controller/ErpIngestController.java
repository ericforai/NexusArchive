// Input: Lombok、Spring Framework、Java 标准库、本地模块
// Output: ErpIngestController 类
// Pos: 接口层 Controller
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.integration.erp.controller;

import com.nexusarchive.common.constants.OperationResult;
import com.nexusarchive.dto.sip.AccountingSipDto;
import com.nexusarchive.dto.sip.IngestResponse;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.integration.erp.dto.BatchIngestRequest;
import com.nexusarchive.integration.erp.dto.BatchIngestResponse;
import com.nexusarchive.integration.erp.dto.IngestResult;
import com.nexusarchive.service.ArchiveService;
import com.nexusarchive.service.IngestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/integration/erp")
@RequiredArgsConstructor
@Slf4j
public class ErpIngestController {

    private final IngestService ingestService;
    private final ArchiveService archiveService;

    @PostMapping("/receive-sip")
    public ResponseEntity<BatchIngestResponse> receiveSip(@Valid @RequestBody BatchIngestRequest request) {
        log.info("Received ERP SIP Push from system: {}, batchId: {}", request.getSourceSystem(), request.getBatchId());

        BatchIngestResponse response = new BatchIngestResponse();
        response.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        List<IngestResult> results = new ArrayList<>();

        if (request.getVouchers() == null || request.getVouchers().isEmpty()) {
            response.setStatus(OperationResult.FAIL);
            response.setGlobalError("No vouchers provided");
            return ResponseEntity.badRequest().body(response);
        }

        for (AccountingSipDto voucherSip : request.getVouchers()) {
            IngestResult result = new IngestResult();
            // Ensure header exists
            if (voucherSip.getHeader() == null) {
                result.setStatus(OperationResult.FAIL);
                result.setMessage("Missing voucher header");
                results.add(result);
                continue;
            }
            
            result.setSourceVoucherId(voucherSip.getHeader().getVoucherNumber());

            try {
                // 1. Idempotency Check
                String uniqueBizId = request.getSourceSystem() + "_" + voucherSip.getHeader().getVoucherNumber();
                Archive existing = archiveService.getByUniqueBizId(uniqueBizId);

                if (existing != null) {
                    if ("ARCHIVED".equalsIgnoreCase(existing.getStatus())) {
                        result.setStatus(OperationResult.FAIL);
                        result.setMessage("Voucher is already archived and cannot be modified.");
                        result.setArchivalCode(existing.getArchiveCode());
                        results.add(result);
                        continue;
                    } else {
                        log.info("Updating existing draft voucher: {}", uniqueBizId);
                        // TODO: Handle draft update logic (e.g., delete previous draft or just overwrite)
                    }
                }

                // 2. Ingest
                if (voucherSip.getRequestId() == null) {
                    voucherSip.setRequestId(UUID.randomUUID().toString());
                }
                // Propagate source system to SIP DTO if missing
                if (voucherSip.getSourceSystem() == null) {
                    voucherSip.setSourceSystem(request.getSourceSystem());
                }

                IngestResponse ingestResp = ingestService.ingestSip(voucherSip);

                result.setStatus(OperationResult.SUCCESS);
                result.setMessage(ingestResp.getMessage());
                // Note: Archival Code is generated asynchronously, so it might be null here.
                // We could predict it if needed, but for now we stick to the async response.
                if (ingestResp.getArchivalCode() != null) {
                    result.setArchivalCode(ingestResp.getArchivalCode());
                } else {
                    // Predict archival code for feedback (Optional, but good for DA/T 104)
                     String predictedCode = String.format("%s-%s-10Y-FIN-AC01-%s", 
                        voucherSip.getHeader().getFondsCode(), 
                        voucherSip.getHeader().getAccountPeriod().substring(0, 4),
                        voucherSip.getHeader().getVoucherNumber());
                     result.setArchivalCode(predictedCode);
                }
                
            } catch (Exception e) {
                log.error("Error processing voucher: {}", voucherSip.getHeader().getVoucherNumber(), e);
                result.setStatus(OperationResult.FAIL);
                result.setMessage(e.getMessage());
            }
            results.add(result);
        }

        response.setResults(results);

        // Determine global status
        boolean hasFailures = results.stream().anyMatch(r -> OperationResult.FAIL.equals(r.getStatus()));
        boolean hasSuccess = results.stream().anyMatch(r -> OperationResult.SUCCESS.equals(r.getStatus()));

        if (hasFailures && hasSuccess) {
            response.setStatus("PARTIAL_SUCCESS");
        } else if (hasFailures) {
            response.setStatus(OperationResult.FAIL);
        } else {
            response.setStatus(OperationResult.SUCCESS);
        }
        
        return ResponseEntity.ok(response);
    }
}
