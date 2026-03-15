// Input: Jackson、Lombok、Spring Framework、Java 标准库、等
// Output: AbnormalVoucherServiceImpl 类
// Pos: 业务服务实现层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusarchive.common.constants.DateFormat;
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
        
        // 保留原始失败原因用于追踪
        String originalReason = abnormal.getFailReason();
        int retryCount = parseRetryCount(originalReason);
        
        try {
            AccountingSipDto sipDto = objectMapper.readValue(abnormal.getSipData(), AccountingSipDto.class);
            
            // [FIX] 生成新的 RequestId 后缀，避免 IngestRequestStatus 表主键冲突
            // 格式: 原始ID-R{重试次数}
            String originalRequestId = sipDto.getRequestId();
            String newRequestId = originalRequestId + "-R" + (retryCount + 1);
            sipDto.setRequestId(newRequestId);
            
            log.info("Retrying abnormal voucher: id={}, originalRequestId={}, newRequestId={}", 
                    id, originalRequestId, newRequestId);
            
            // 更新状态为 RETRYING（重试中）
            abnormal.setStatus("RETRYING");
            abnormal.setUpdateTime(LocalDateTime.now());
            // [FIX] 保留原始原因 + 追加重试信息
            abnormal.setFailReason(String.format("[重试#%d] %s | 原因: %s", 
                    retryCount + 1, 
                    LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern(DateFormat.DATETIME)),
                    originalReason));
            abnormalVoucherMapper.updateById(abnormal);
            
            // 重新提交到 IngestService
            ingestService.ingestSip(sipDto);
            
            // [FIX] 不再立即标记 RESOLVED
            // 异步归档流程会在成功后通过事件机制更新状态
            // 保持 RETRYING 状态，等待 ComplianceListener 的处理结果
            // 如果二次归档成功，ComplianceListener 不会再调用 saveAbnormal
            // 如果二次归档失败，ComplianceListener 会创建新的异常记录
            log.info("Retry submitted for abnormal voucher: id={}, status remains RETRYING until async completion", id);
            
        } catch (Exception e) {
            log.error("Retry failed for abnormal voucher: " + id, e);
            abnormal.setStatus("PENDING"); // 回退到 PENDING 状态
            // [FIX] 保留原始原因 + 追加重试失败信息
            abnormal.setFailReason(String.format("[重试#%d失败] %s | 错误: %s | 原因: %s",
                    retryCount + 1,
                    LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern(DateFormat.DATETIME)),
                    e.getMessage(),
                    originalReason));
            abnormal.setUpdateTime(LocalDateTime.now());
            abnormalVoucherMapper.updateById(abnormal);
            throw new BusinessException(500, "Retry failed: " + e.getMessage());
        }
    }
    
    /**
     * 从失败原因中解析重试次数
     */
    private int parseRetryCount(String failReason) {
        if (failReason == null) {
            return 0;
        }
        // 匹配 [重试#N] 模式
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\[重试#(\\d+)");
        java.util.regex.Matcher matcher = pattern.matcher(failReason);
        int maxCount = 0;
        while (matcher.find()) {
            int count = Integer.parseInt(matcher.group(1));
            if (count > maxCount) {
                maxCount = count;
            }
        }
        return maxCount;
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markResolvedByRequestId(String originalRequestId) {
        // 支持带 -R1, -R2 等后缀的 requestId（重试场景）
        // 也支持不带后缀的原始 requestId
        String baseRequestId = originalRequestId.replaceAll("-R\\d+$", "");

        List<AbnormalVoucher> abnormals = abnormalVoucherMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AbnormalVoucher>()
                .eq(AbnormalVoucher::getStatus, "RETRYING")
                .likeRight(AbnormalVoucher::getRequestId, baseRequestId)
                .orderByDesc(AbnormalVoucher::getUpdateTime)
        );

        for (AbnormalVoucher abnormal : abnormals) {
            abnormal.setStatus("RESOLVED");
            abnormal.setUpdateTime(LocalDateTime.now());
            abnormalVoucherMapper.updateById(abnormal);
            log.info("Marked abnormal voucher as RESOLVED: requestId={}, originalRequestId={}",
                abnormal.getRequestId(), originalRequestId);
        }

        if (abnormals.isEmpty()) {
            log.warn("No RETRYING abnormal found for requestId: {}", originalRequestId);
        }
    }
}
