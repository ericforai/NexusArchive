// Input: Lombok、Spring Framework、Java 标准库、本地模块
// Output: ComplianceListener 类
// Pos: 事件监听
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.listener;

import com.nexusarchive.dto.sip.report.FourNatureReport;
import com.nexusarchive.dto.sip.report.OverallStatus;
import com.nexusarchive.entity.IngestRequestStatus;
import com.nexusarchive.event.CheckPassedEvent;
import com.nexusarchive.event.VoucherReceivedEvent;
import com.nexusarchive.mapper.IngestRequestStatusMapper;
import com.nexusarchive.service.AbnormalVoucherService;
import com.nexusarchive.service.FourNatureCheckService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class ComplianceListener {

    private final FourNatureCheckService fourNatureCheckService;
    private final IngestRequestStatusMapper statusMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final AbnormalVoucherService abnormalVoucherService;

    @Async("ingestTaskExecutor")
    @EventListener
    public void handleVoucherReceived(VoucherReceivedEvent event) {
        String requestId = event.getSipDto().getRequestId();
        log.info("Async Phase 1: Starting Compliance Check for requestId={}", requestId);

        try {
            // Update status to CHECKING
            updateStatus(requestId, "CHECKING", "正在进行四性检测...");

            // Perform Four Nature Check
            FourNatureReport report = fourNatureCheckService.performFullCheck(event.getSipDto(), event.getFileStreams());

            if (report.getStatus() == OverallStatus.FAIL) {
                String errorMsg = buildFourNatureErrorMsg(report);
                log.error("Compliance Check Failed for requestId={}: {}", requestId, errorMsg);
                updateStatus(requestId, "FAILED", errorMsg);
                
                // Save to Abnormal Pool
                abnormalVoucherService.saveAbnormal(event.getSipDto(), errorMsg);
                return;
            }

            // Update status to CHECK_PASSED
            updateStatus(requestId, "CHECK_PASSED", "四性检测通过");

            // Publish CheckPassedEvent
            eventPublisher.publishEvent(new CheckPassedEvent(this, event.getSipDto(), event.getTempPath(), report, event.getFileStreams()));

        } catch (Exception e) {
            log.error("Error during Compliance Check for requestId={}", requestId, e);
            updateStatus(requestId, "FAILED", "系统内部错误: " + e.getMessage());
            // Also save system errors to abnormal pool for retry
            abnormalVoucherService.saveAbnormal(event.getSipDto(), "系统内部错误: " + e.getMessage());
        }
    }

    private void updateStatus(String requestId, String status, String message) {
        IngestRequestStatus statusEntity = new IngestRequestStatus();
        statusEntity.setRequestId(requestId);
        statusEntity.setStatus(status);
        statusEntity.setMessage(message);
        statusEntity.setUpdatedTime(LocalDateTime.now());
        statusMapper.updateById(statusEntity);
    }

    private String buildFourNatureErrorMsg(FourNatureReport report) {
        StringBuilder errorMsg = new StringBuilder("四性检测失败: ");
        if (report.getAuthenticity() != null && report.getAuthenticity().getStatus() == OverallStatus.FAIL) {
            errorMsg.append("真实性校验未通过; ");
        }
        if (report.getSafety() != null && report.getSafety().getStatus() == OverallStatus.FAIL) {
            errorMsg.append("安全性检测发现威胁; ");
        }
        if (report.getIntegrity() != null && report.getIntegrity().getStatus() == OverallStatus.FAIL) {
            errorMsg.append("完整性校验失败; ");
        }
        if (report.getUsability() != null && report.getUsability().getStatus() == OverallStatus.FAIL) {
            errorMsg.append("可用性检测失败; ");
        }
        return errorMsg.toString();
    }
}
