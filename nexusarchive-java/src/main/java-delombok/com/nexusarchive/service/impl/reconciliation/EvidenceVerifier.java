// Input: MyBatis-Plus、Lombok、Java 标准库
// Output: EvidenceVerifier 类
// Pos: 对账服务 - 证据链验证层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl.reconciliation;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.mapper.ArcFileContentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 证据链验证器
 * <p>
 * 验证档案的原始证据文件是否完整（标准文件、哈希、签名）
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EvidenceVerifier {

    private static final int ATTACHMENT_BATCH_SIZE = 500;
    private static final String PRE_ARCHIVE_STATUS_ARCHIVED = "ARCHIVED";

    private final ArcFileContentMapper arcFileContentMapper;

    /**
     * 验证证据链完整性
     *
     * @param archiveCodes 档案代码列表
     * @return 证据汇总结果
     */
    public EvidenceSummary verifyEvidence(List<String> archiveCodes) {
        EvidenceSummary summary = new EvidenceSummary();
        if (archiveCodes == null || archiveCodes.isEmpty()) {
            return summary;
        }

        List<String> uniqueCodes = new ArrayList<>(new LinkedHashSet<>(archiveCodes));
        Map<String, List<ArcFileContent>> filesByCode = new HashMap<>();

        // 分批查询文件
        for (int i = 0; i < uniqueCodes.size(); i += ATTACHMENT_BATCH_SIZE) {
            int end = Math.min(i + ATTACHMENT_BATCH_SIZE, uniqueCodes.size());
            List<String> batch = uniqueCodes.subList(i, end);
            QueryWrapper<ArcFileContent> fileQuery = new QueryWrapper<>();
            fileQuery.select("archival_code", "file_name", "file_type", "file_hash", "hash_algorithm",
                            "original_hash", "current_hash", "timestamp_token", "sign_value", "certificate",
                            "pre_archive_status")
                    .in("archival_code", batch);

            List<ArcFileContent> batchFiles = arcFileContentMapper.selectList(fileQuery);
            if (batchFiles == null) {
                continue;
            }
            summary.totalAttachments += batchFiles.size();
            for (ArcFileContent file : batchFiles) {
                filesByCode.computeIfAbsent(file.getArchivalCode(), key -> new ArrayList<>()).add(file);
            }
        }

        // 验证每个档案的证据
        for (String code : uniqueCodes) {
            List<ArcFileContent> files = filesByCode.get(code);
            if (files == null || files.isEmpty()) {
                summary.missingEvidenceCount++;
                continue;
            }
            boolean hasArchived = files.stream().anyMatch(this::isArchived);
            if (!hasArchived) {
                summary.missingEvidenceCount++;
                continue;
            }
            boolean hasStandardFile = files.stream().anyMatch(this::isStandardFile);
            boolean hasHash = files.stream().anyMatch(this::hasHash);
            boolean hasSignature = files.stream().anyMatch(this::hasSignature);
            if (!hasStandardFile || !hasHash || !hasSignature) {
                summary.invalidEvidenceCount++;
            }
        }

        return summary;
    }

    /**
     * 检查文件是否已归档
     */
    private boolean isArchived(ArcFileContent file) {
        if (file == null) {
            return false;
        }
        String status = file.getPreArchiveStatus();
        return status == null || PRE_ARCHIVE_STATUS_ARCHIVED.equalsIgnoreCase(status);
    }

    /**
     * 检查是否为标准文件（PDF/OFD）
     */
    private boolean isStandardFile(ArcFileContent file) {
        if (file == null) {
            return false;
        }
        String type = file.getFileType();
        String name = file.getFileName();
        if (hasText(type)) {
            String normalized = type.trim().toLowerCase();
            if (normalized.contains("pdf") || normalized.contains("ofd")) {
                return true;
            }
        }
        if (hasText(name)) {
            String normalized = name.trim().toLowerCase();
            return normalized.endsWith(".pdf") || normalized.endsWith(".ofd");
        }
        return false;
    }

    /**
     * 检查是否有哈希值
     */
    private boolean hasHash(ArcFileContent file) {
        if (file == null) {
            return false;
        }
        boolean hasValue = hasText(file.getFileHash()) || hasText(file.getOriginalHash())
                || hasText(file.getCurrentHash());
        return hasValue && hasText(file.getHashAlgorithm());
    }

    /**
     * 检查是否有签名
     */
    private boolean hasSignature(ArcFileContent file) {
        if (file == null) {
            return false;
        }
        return (file.getTimestampToken() != null && file.getTimestampToken().length > 0)
                || (file.getSignValue() != null && file.getSignValue().length > 0)
                || hasText(file.getCertificate());
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    /**
     * 证据汇总结果
     */
    public static class EvidenceSummary {
        public int totalAttachments = 0;
        public int missingEvidenceCount = 0;
        public int invalidEvidenceCount = 0;
    }
}
