// Input: Spring Framework、本地服务
// Output: PoolFileArchiver 类
// Pos: 业务服务层

package com.nexusarchive.service.ingest;

import com.nexusarchive.common.constant.ErrorCode;
import com.nexusarchive.common.exception.BusinessException;
import com.nexusarchive.dto.sip.AccountingSipDto;
import com.nexusarchive.dto.sip.AttachmentDto;
import com.nexusarchive.dto.sip.VoucherHeadDto;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.entity.enums.PreArchiveStatus;
import com.nexusarchive.mapper.ArcFileContentMapper;
import com.nexusarchive.service.ArchiveSecurityService;
import com.nexusarchive.service.ArchiveService;
import com.nexusarchive.service.ArchivalPackageService;
import com.nexusarchive.service.ErpFeedbackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 预归档池文件归档处理器
 * <p>
 * 处理从预归档池到正式档案的转换流水线：
 * <ul>
 *   <li>生成档号</li>
 *   <li>文件物理复制</li>
 *   <li>构建 AIP 封装包</li>
 *   <li>创建档案索引</li>
 *   <li>更新状态</li>
 *   <li>ERP 反馈</li>
 * </ul>
 * </p>
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PoolFileArchiver {

    private final ArchivalCodeGenerator archivalCodeGenerator;
    private final ArcFileContentMapper arcFileContentMapper;
    private final ArchivalPackageService archivalPackageService;
    private final ArchiveService archiveService;
    private final ErpFeedbackService erpFeedbackService;
    private final ArchiveSecurityService archiveSecurityService;

    /**
     * 归档处理结果
     */
    public record ArchiveResult(
            String archivalCode,
            String poolItemId,
            boolean success,
            String errorMessage) {
    }

    /**
     * 执行归档处理单个文件
     *
     * @param poolItemId 预归档文件ID
     * @param tempRootPath 临时目录
     * @param userId 操作人ID
     * @return 归档结果
     */
    public ArchiveResult archiveFile(String poolItemId, String tempRootPath, String userId) {
        try {
            // 1. 获取最新记录
            ArcFileContent originalFile = arcFileContentMapper.selectById(poolItemId);
            if (originalFile == null) {
                return new ArchiveResult(null, poolItemId, false, "文件不存在");
            }

            // 2. 生成档号与物理操作
            String archivalCode = archivalCodeGenerator.generate(originalFile);
            String tempPath = tempRootPath + "/uploads";
            String targetFileName = "voucher_" + archivalCode + "." + originalFile.getFileType().toLowerCase();

            Path sourcePath = Path.of(originalFile.getStoragePath());
            Path targetPath = Path.of(tempPath, targetFileName);

            if (!Files.exists(sourcePath)) {
                return new ArchiveResult(null, poolItemId, false, "文件物理丢失: " + originalFile.getFileName());
            }

            Files.createDirectories(targetPath.getParent());
            Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);

            // 3. 构建并保存 AIP 封装包
            AccountingSipDto sip = buildSimpleSip(archivalCode, poolItemId, targetFileName, originalFile);
            archivalPackageService.archivePackage(sip, tempPath);

            // 4. 创建正式档案索引
            Archive archive = createArchiveRecord(archivalCode, poolItemId, originalFile, userId);
            archiveService.createArchive(archive, userId != null ? userId : "user_admin");

            // 5. 更新预归档文件状态为"已归档"
            originalFile.setPreArchiveStatus(PreArchiveStatus.ARCHIVED.getCode());
            arcFileContentMapper.updateById(originalFile);

            // 6. ERP 异步反馈
            erpFeedbackService.triggerFeedback(originalFile, archivalCode);

            log.info("文件归档成功: {} -> {}", poolItemId, archivalCode);
            return new ArchiveResult(archivalCode, poolItemId, true, null);

        } catch (Exception e) {
            log.error("归档流水线单项处理失败: poolItemId={}", poolItemId, e);
            return new ArchiveResult(null, poolItemId, false, e.getMessage());
        }
    }

    /**
     * 回滚失败的归档
     *
     * @param poolItemId 文件ID
     * @param archivalCode 档号
     */
    public void rollbackFailedArchive(String poolItemId, String archivalCode) {
        // 补偿：删除已创建的档案索引
        try {
            Archive existing = archiveService.getByUniqueBizId(poolItemId);
            if (existing != null) {
                archiveService.deleteArchive(existing.getId());
                log.info("已回滚失败的档案索引: {}", existing.getArchiveCode());
            }
        } catch (Exception ex) {
            log.error("回滚档案索引失败: {}", ex.getMessage());
        }

        // 补偿：恢复状态为待归档，以便重试
        ArcFileContent f = arcFileContentMapper.selectById(poolItemId);
        if (f != null) {
            f.setPreArchiveStatus(PreArchiveStatus.PENDING_ARCHIVE.getCode());
            arcFileContentMapper.updateById(f);
        }
    }

    /**
     * 批量挂载安全存证批次
     *
     * @param processedFiles 已处理的文件列表
     * @param userId 操作人ID
     * @return 批次号
     */
    public String attachSecurityBatch(List<ArcFileContent> processedFiles, String userId) {
        if (processedFiles.isEmpty()) {
            return null;
        }

        try {
            String batchNo = "BAT-" + System.currentTimeMillis();
            var batch = archiveSecurityService.createSecurityBatch(
                    batchNo, processedFiles, userId != null ? userId : "user_admin");

            if (batch != null) {
                for (int i = 0; i < processedFiles.size(); i++) {
                    ArcFileContent f = processedFiles.get(i);
                    f.setBatchId(batch.getId());
                    f.setSequenceInBatch(i + 1);
                    arcFileContentMapper.updateById(f);
                }
            }
            log.info("已完成 {} 笔存证封卷挂链", processedFiles.size());
            return batchNo;
        } catch (Exception e) {
            log.error("存证挂链失败", e);
            return null;
        }
    }

    /**
     * 创建档案记录
     */
    private Archive createArchiveRecord(String archivalCode, String poolItemId,
                                        ArcFileContent originalFile, String userId) {
        Archive archive = new Archive();
        archive.setArchiveCode(archivalCode);
        archive.setTitle("会计凭证-" + archivalCode);
        archive.setFondsNo(originalFile.getFondsCode());
        archive.setFiscalYear(originalFile.getFiscalYear() != null ? originalFile.getFiscalYear()
                : String.valueOf(LocalDate.now().getYear()));
        archive.setFiscalPeriod(
                LocalDate.now().format(DateTimeFormatter.ofPattern("MM")));
        archive.setUniqueBizId(poolItemId);
        return archive;
    }

    /**
     * 构建简化的 SIP
     */
    private AccountingSipDto buildSimpleSip(String archivalCode, String poolItemId, String fileName,
                                              ArcFileContent originalFile) {
        AccountingSipDto sip = new AccountingSipDto();
        sip.setRequestId(archivalCode);
        sip.setSourceSystem(originalFile.getSourceSystem() != null ? originalFile.getSourceSystem() : "Pool Archive");

        // 构建凭证头 - 从元数据读取
        VoucherHeadDto header = new VoucherHeadDto();
        header.setFondsCode(originalFile.getFondsCode());

        // 会计期间：从元数据或当前日期
        String fiscalYear = originalFile.getFiscalYear() != null
                ? originalFile.getFiscalYear()
                : String.valueOf(LocalDate.now().getYear());
        header.setAccountPeriod(fiscalYear + "-" + LocalDate.now().format(
                DateTimeFormatter.ofPattern("MM")));

        header.setVoucherType(com.nexusarchive.common.enums.VoucherType.PAYMENT);
        header.setVoucherNumber("V-" + poolItemId.substring(0, Math.min(6, poolItemId.length())));
        header.setVoucherDate(LocalDate.now());
        header.setTotalAmount(java.math.BigDecimal.ZERO);
        header.setCurrencyCode("CNY");
        header.setIssuer(originalFile.getCreator() != null ? originalFile.getCreator() : "System");
        header.setAttachmentCount(1);
        sip.setHeader(header);

        // 构建附件列表
        List<AttachmentDto> attachments = new ArrayList<>();
        AttachmentDto attachment = new AttachmentDto();
        attachment.setFileName(fileName);
        attachment.setFileType(originalFile.getFileType());
        attachment.setFileSize(originalFile.getFileSize());
        attachment.setFileHash(originalFile.getFileHash());
        attachment.setHashAlgorithm(originalFile.getHashAlgorithm());
        attachments.add(attachment);
        sip.setAttachments(attachments);

        return sip;
    }
}
