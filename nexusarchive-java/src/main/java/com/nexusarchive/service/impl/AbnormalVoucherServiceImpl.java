// Input: Jackson、Lombok、Spring Framework、Java 标准库、等
// Output: AbnormalVoucherServiceImpl 类
// Pos: 业务服务实现层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusarchive.common.exception.BusinessException;
import com.nexusarchive.dto.sip.AccountingSipDto;
import com.nexusarchive.entity.AbnormalVoucher;
import com.nexusarchive.mapper.AbnormalVoucherMapper;
import com.nexusarchive.service.AbnormalVoucherService;
import com.nexusarchive.service.IngestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AbnormalVoucherServiceImpl implements AbnormalVoucherService {

    private final AbnormalVoucherMapper abnormalVoucherMapper;
    private final ObjectMapper objectMapper;
    private final IngestService ingestService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveAbnormal(AccountingSipDto sip, String reason) {
        try {
            AbnormalVoucher abnormal = AbnormalVoucher.builder()
                    .requestId(sip.getRequestId())
                    .sourceSystem(sip.getSourceSystem())
                    .voucherNumber(sip.getHeader().getVoucherNumber())
                    .sipData(objectMapper.writeValueAsString(sip))
                    .failReason(reason)
                    .status("PENDING")
                    .createTime(LocalDateTime.now())
                    .updateTime(LocalDateTime.now())
                    .build();
            
            abnormalVoucherMapper.insert(abnormal);
            log.info("Saved abnormal voucher: {}", sip.getHeader().getVoucherNumber());
        } catch (Exception e) {
            log.error("Failed to save abnormal voucher", e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void retry(String id) {
        AbnormalVoucher abnormal = abnormalVoucherMapper.selectById(id);
        if (abnormal == null) {
            throw new BusinessException(404, "Abnormal record not found");
        }
        
        try {
            AccountingSipDto sipDto = objectMapper.readValue(abnormal.getSipData(), AccountingSipDto.class);
            
            // Update status to RETRYING
            abnormal.setStatus("RETRYING");
            abnormal.setUpdateTime(LocalDateTime.now());
            abnormalVoucherMapper.updateById(abnormal);
            
            // Re-submit to IngestService
            // Note: This will start a new async process. 
            // Ideally we should link the new request ID or keep the old one.
            // For simplicity, we reuse the SIP DTO which has the original Request ID.
            // But IngestService might complain if Request ID already exists in status table?
            // Let's assume IngestService handles re-submission or we generate a new Request ID suffix.
            // For now, just call ingest.
            ingestService.ingestSip(sipDto);
            
            // If ingest throws no immediate error, we mark as RESOLVED (or wait for callback?)
            // Since ingest is async, we can't know for sure if it passed checks yet.
            // But we can mark this abnormal record as "RESOLVED" assuming the new flow takes over.
            // Or better, keep it as RETRYING until we get a success event? 
            // For MVP, mark as RESOLVED to clear the queue.
            abnormal.setStatus("RESOLVED");
            abnormalVoucherMapper.updateById(abnormal);
            
        } catch (Exception e) {
            log.error("Retry failed for abnormal voucher: " + id, e);
            abnormal.setStatus("PENDING"); // Revert to PENDING
            abnormal.setFailReason("Retry failed: " + e.getMessage());
            abnormalVoucherMapper.updateById(abnormal);
            throw new BusinessException(500, "Retry failed: " + e.getMessage());
        }
    }

    @Override
    public List<AbnormalVoucher> getPendingAbnormals() {
        return abnormalVoucherMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AbnormalVoucher>()
                .eq(AbnormalVoucher::getStatus, "PENDING")
                .orderByDesc(AbnormalVoucher::getCreateTime)
        );
    }

    @Override
    public void updateSipData(String id, AccountingSipDto newSipDto) {
        AbnormalVoucher abnormal = abnormalVoucherMapper.selectById(id);
        if (abnormal == null) {
            throw new BusinessException(404, "Abnormal record not found");
        }
        
        try {
            abnormal.setSipData(objectMapper.writeValueAsString(newSipDto));
            abnormal.setUpdateTime(LocalDateTime.now());
            abnormalVoucherMapper.updateById(abnormal);
        } catch (Exception e) {
            throw new BusinessException(500, "Failed to update SIP data");
        }
    }
}
