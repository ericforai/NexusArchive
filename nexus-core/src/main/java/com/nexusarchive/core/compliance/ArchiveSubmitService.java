// Input: 归档提交请求（含四性检测策略）
// Output: 归档记录（已通过四性检测）
// Pos: NexusCore compliance 整合示例
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core.compliance;

import com.nexusarchive.core.ArchiveYearContext;
import com.nexusarchive.core.FondsContext;
import com.nexusarchive.core.FondsIsolationException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 归档提交服务 - 整合示例
 * 
 * 演示 Day 3 隔离体系与 Day 4 四性引擎的整合调用链：
 * 1. 从上下文获取 fonds_no / archive_year
 * 2. 执行四性检测 (真实性/可用性)
 * 3. 持久化归档记录 (自动注入隔离字段)
 */
@Service
public class ArchiveSubmitService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ArchiveSubmitService.class);

    private final FourNatureCheckService fourNatureCheckService;
    private final FileHashService fileHashService;

    public ArchiveSubmitService(FourNatureCheckService fourNatureCheckService,
                                 FileHashService fileHashService) {
        this.fourNatureCheckService = Objects.requireNonNull(fourNatureCheckService);
        this.fileHashService = Objects.requireNonNull(fileHashService);
    }

    /**
     * 提交归档文件
     *
     * @param filePath 文件路径
     * @param request 归档请求
     * @return 归档结果
     * @throws IOException 文件读取失败
     * @throws FondsIsolationException 隔离上下文缺失或四性检测失败
     */
    public ArchiveSubmitResult submit(Path filePath, ArchiveSubmitRequest request) throws IOException {
        Objects.requireNonNull(filePath, "filePath must not be null");
        Objects.requireNonNull(request, "request must not be null");

        // Step 1: 验证隔离上下文
        String fondsNo = FondsContext.requireFondsNo();
        int archiveYear = ArchiveYearContext.requireArchiveYear();
        LOGGER.info("归档提交开始 - fonds_no={}, archive_year={}, file={}", 
                fondsNo, archiveYear, filePath.getFileName());

        // Step 2: 计算文件哈希 (如果未提供)
        String expectedHash = request.getExpectedHash();
        if (expectedHash == null || expectedHash.isBlank()) {
            expectedHash = fileHashService.hashFile(filePath, request.getHashAlgorithm());
            LOGGER.info("计算文件哈希 - algorithm={}, hash={}", 
                    request.getHashAlgorithm(), expectedHash);
        }

        // Step 3: 执行四性检测
        FourNatureCheckRequest checkRequest = FourNatureCheckRequest.builder()
                .expectedHash(expectedHash)
                .hashAlgorithm(request.getHashAlgorithm())
                .expectedExtension(request.getExpectedExtension())
                .metadataXmlPath(request.getMetadataXmlPath())
                .integrityRequired(request.isIntegrityRequired())
                .signatureRequired(request.isSignatureRequired())
                .virusScanRequired(request.isVirusScanRequired())
                .build();
        FourNatureCheckResult checkResult = fourNatureCheckService.check(filePath, checkRequest);

        if (!checkResult.isAllPassed()) {
            LOGGER.warn("四性检测失败 - authenticity={}, usability={}", 
                    checkResult.isAuthenticityPassed(), checkResult.isUsabilityPassed());
            throw new FondsIsolationException("四性检测失败: " + formatFailures(checkResult));
        }
        LOGGER.info("四性检测通过 - hash={}, fileType={}", 
                checkResult.getComputedHash(), checkResult.getDetectedFileType());

        // Step 4: 持久化 (此处省略 Mapper 调用，仅返回结果)
        // 实际场景: archiveMapper.insert(archiveEntity);
        // FondsIsolationInterceptor 会自动注入 fonds_no / archive_year

        return ArchiveSubmitResult.builder()
                .fondsNo(fondsNo)
                .archiveYear(archiveYear)
                .computedHash(checkResult.getComputedHash())
                .detectedFileType(checkResult.getDetectedFileType())
                .success(true)
                .build();
    }

    private String formatFailures(FourNatureCheckResult result) {
        StringBuilder sb = new StringBuilder();
        if (!result.isAuthenticityPassed()) {
            sb.append("真实性(").append(result.getAuthenticityMessage()).append("); ");
        }
        if (!result.isIntegrityPassed()) {
            sb.append("完整性(").append(result.getIntegrityMessage()).append("); ");
        }
        if (!result.isUsabilityPassed()) {
            sb.append("可用性(文件类型: ").append(result.getDetectedFileType()).append("); ");
        }
        if (!result.isSafetyPassed()) {
            sb.append("安全性(").append(result.getSafetyMessage()).append("); ");
        }
        return sb.toString().trim();
    }
}
