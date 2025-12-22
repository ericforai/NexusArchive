// Input: Jackson、Lombok、Spring Framework、Java 标准库、等
// Output: ProcessingListener 类
// Pos: 事件监听
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusarchive.dto.sip.AccountingSipDto;
import com.nexusarchive.dto.sip.VoucherHeadDto;
import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.entity.IngestRequestStatus;
import com.nexusarchive.event.CheckPassedEvent;
import com.nexusarchive.mapper.ArchiveMapper;
import com.nexusarchive.mapper.IngestRequestStatusMapper;
import com.nexusarchive.service.ArchivalPackageService;
import com.nexusarchive.service.IAutoAssociationService;
import com.nexusarchive.service.SmartParserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.FileSystemUtils;

import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProcessingListener {

    private final ArchivalPackageService archivalPackageService;
    private final SmartParserService smartParserService;
    private final IAutoAssociationService autoAssociationService;
    private final ArchiveMapper archiveMapper;
    private final IngestRequestStatusMapper statusMapper;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Async("ingestTaskExecutor")
    @EventListener
    public void handleCheckPassed(CheckPassedEvent event) {
        String requestId = event.getSipDto().getRequestId();
        log.info("Async Phase 2: Starting Processing for requestId={}", requestId);

        try {
            updateStatus(requestId, "PROCESSING", "正在进行归档和智能解析...");

            AccountingSipDto sipDto = event.getSipDto();
            String tempPath = event.getTempPath();

            // 1. AIP 封装与物理归档
            List<ArcFileContent> archivedFiles = archivalPackageService.archivePackage(sipDto, tempPath);
            String storagePath = archivedFiles.isEmpty() ? "No files" : archivedFiles.get(0).getStoragePath();

            // 2. 保存 Archive 实体 (Voucher Level)
            Archive archive = saveArchiveEntity(sipDto, archivedFiles);

            // 3. 触发智能解析
            if (!archivedFiles.isEmpty()) {
                smartParserService.parseAndIndex(archivedFiles);
            }

            // 4. 触发自动关联
            if (archive != null) {
                autoAssociationService.triggerAssociation(archive.getId());
            }

            // 5. 发布归档完成事件（触发签章和时间戳）
            if (archive != null && !archivedFiles.isEmpty()) {
                eventPublisher.publishEvent(
                    new com.nexusarchive.listener.SignatureTimestampListener.ArchiveCompletedEvent(
                        archive, archivedFiles
                    )
                );
            }

            // 6. 完成
            updateStatus(requestId, "COMPLETED", "归档完成，已存储至: " + storagePath);

        } catch (Exception e) {
            log.error("Error during Processing for requestId={}", requestId, e);
            updateStatus(requestId, "FAILED", "处理失败: " + e.getMessage());
        } finally {
            // 清理临时目录
            cleanupTempDir(event.getTempPath());
        }
    }

    private Archive saveArchiveEntity(AccountingSipDto sipDto, List<ArcFileContent> archivedFiles) {
        try {
            VoucherHeadDto header = sipDto.getHeader();
            Archive archive = new Archive();
            
            // 基础信息映射
            archive.setFondsNo(header.getFondsCode());
            // 假设 ArchivalPackageService 生成了档号，这里我们可能需要重新生成或者从 files 里获取
            // 这里简单重新生成一个一致的，或者更好的方式是 ArchivalPackageService 返回档号
            // 暂时使用 header 中的信息生成
            String archivalCode = archivedFiles.isEmpty() ? generateArchivalCode(header) : archivedFiles.get(0).getArchivalCode();
            archive.setArchiveCode(archivalCode);
            
            archive.setCategoryCode("AC01"); // 会计凭证
            archive.setTitle("会计凭证-" + header.getVoucherNumber());
            archive.setFiscalYear(header.getAccountPeriod().substring(0, 4));
            archive.setFiscalPeriod(header.getAccountPeriod());
            archive.setRetentionPeriod("10Y"); // Mock
            archive.setOrgName(header.getIssuer()); // Mock
            archive.setCreator(header.getIssuer());
            archive.setStatus("ARCHIVED");
            archive.setSecurityLevel("INTERNAL");
            
            // Set uniqueBizId for idempotency
            // Format: SourceSystem_VoucherNumber
            String sourceSystem = sipDto.getSourceSystem() != null ? sipDto.getSourceSystem() : "UNKNOWN";
            archive.setUniqueBizId(sourceSystem + "_" + header.getVoucherNumber());
            
            // 保存元数据
            archive.setStandardMetadata(objectMapper.writeValueAsString(header));
            
            // 完整性校验值 (取第一个文件的 hash 作为代表，或者聚合)
            if (!archivedFiles.isEmpty()) {
                archive.setFixityValue(archivedFiles.get(0).getFileHash());
                archive.setFixityAlgo(archivedFiles.get(0).getHashAlgorithm());
                archive.setLocation(archivedFiles.get(0).getStoragePath());
            }

            archiveMapper.insert(archive);
            return archive;
        } catch (Exception e) {
            log.error("Failed to save Archive entity", e);
            throw new RuntimeException("Failed to save Archive entity", e);
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

    private void cleanupTempDir(String tempPath) {
        try {
            FileSystemUtils.deleteRecursively(Paths.get(tempPath));
        } catch (Exception e) {
            log.warn("临时目录清理失败: {}", tempPath, e);
        }
    }

    private String generateArchivalCode(VoucherHeadDto header) {
        return String.format("%s-%s-10Y-FIN-AC01-%s", 
                header.getFondsCode(), 
                header.getAccountPeriod().substring(0, 4),
                header.getVoucherNumber());
    }
}
