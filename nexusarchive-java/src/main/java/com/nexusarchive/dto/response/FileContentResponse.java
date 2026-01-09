// Input: Lombok、Java 标准库、Swagger、MyBatis-Plus
// Output: FileContentResponse 类
// Pos: 数据传输对象
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto.response;

import com.nexusarchive.entity.ArcFileContent;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 文件内容响应 DTO
 * <p>
 * 从 ArcFileContent Entity 转换，隐藏以下敏感/大字段：
 * - timestampToken: 时间戳令牌（二进制数据）
 * - signValue: 签名值（二进制数据）
 * - sourceData: 原始业务数据（可能包含敏感信息）
 * - certificate: 数字证书（敏感信息）
 * - storagePath: 物理存储路径（内部信息）
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "文件内容响应")
public class FileContentResponse {

    /**
     * 文件ID
     */
    @Schema(description = "文件ID", example = "1234567890")
    private String id;

    /**
     * 关联的档案号
     */
    @Schema(description = "档案号", example = "F001-2024-AC01-0001")
    private String archivalCode;

    /**
     * 文件名
     */
    @Schema(description = "文件名", example = "invoice_001.pdf")
    private String fileName;

    /**
     * 文件类型
     */
    @Schema(description = "文件类型", example = "application/pdf")
    private String fileType;

    /**
     * 文件大小（字节）
     */
    @Schema(description = "文件大小(字节)", example = "1024000")
    private Long fileSize;

    /**
     * 文件哈希值
     */
    @Schema(description = "文件哈希值", example = "a1b2c3d4...")
    private String fileHash;

    /**
     * 哈希算法 (SM3, SHA256)
     */
    @Schema(description = "哈希算法", example = "SM3")
    private String hashAlgorithm;

    /**
     * 关联单据ID
     */
    @Schema(description = "关联单据ID", example = "archive001")
    private String itemId;

    /**
     * 原始哈希值
     */
    @Schema(description = "原始哈希值", example = "original_hash_123")
    private String originalHash;

    /**
     * 当前哈希值
     */
    @Schema(description = "当前哈希值", example = "current_hash_456")
    private String currentHash;

    /**
     * 完整性校验状态
     */
    @Schema(description = "完整性校验状态", example = "VALID")
    private String integrityStatus;

    /**
     * 预归档状态
     */
    @Schema(description = "预归档状态", example = "ARCHIVED")
    private String preArchiveStatus;

    /**
     * 四性检测结果
     */
    @Schema(description = "四性检测结果", example = "{\"authenticity\": true}")
    private String checkResultSummary;

    /**
     * 检测时间
     */
    @Schema(description = "检测时间", example = "2024-01-01T10:00:00")
    private LocalDateTime checkedTime;

    /**
     * 归档时间
     */
    @Schema(description = "归档时间", example = "2024-01-01T10:00:00")
    private LocalDateTime archivedTime;

    /**
     * 会计年度
     */
    @Schema(description = "会计年度", example = "2024")
    private String fiscalYear;

    /**
     * 凭证类型
     */
    @Schema(description = "凭证类型", example = "记")
    private String voucherType;

    /**
     * 责任者/创建人
     */
    @Schema(description = "创建人", example = "张三")
    private String creator;

    /**
     * 全宗号
     */
    @Schema(description = "全宗号", example = "F001")
    private String fondsCode;

    /**
     * 来源系统
     */
    @Schema(description = "来源系统", example = "YonSuite")
    private String sourceSystem;

    /**
     * 来源唯一标识
     */
    @Schema(description = "来源唯一标识", example = "YonSuite_12345")
    private String businessDocNo;

    /**
     * ERP原始凭证号
     */
    @Schema(description = "ERP原始凭证号", example = "记-3")
    private String erpVoucherNo;

    /**
     * 归档批次 ID
     */
    @Schema(description = "归档批次ID", example = "1001")
    private Long batchId;

    /**
     * 批次内序号
     */
    @Schema(description = "批次内序号", example = "1")
    private Integer sequenceInBatch;

    /**
     * 摘要
     */
    @Schema(description = "摘要", example = "采购凭证")
    private String summary;

    /**
     * 凭证字
     */
    @Schema(description = "凭证字", example = "记")
    private String voucherWord;

    /**
     * 业务日期
     */
    @Schema(description = "业务日期", example = "2024-01-15")
    private LocalDate docDate;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间", example = "2024-01-01T10:00:00")
    private LocalDateTime createdTime;

    /**
     * 是否有数字签名
     */
    @Schema(description = "是否有数字签名", example = "true")
    private Boolean hasSignature;

    /**
     * 签名验证状态
     */
    @Schema(description = "签名验证状态", example = "VALID")
    private String signatureStatus;

    /**
     * 从 Entity 转换为 DTO
     * 隐藏字段: timestampToken, signValue, certificate, storagePath, sourceData
     */
    public static FileContentResponse fromEntity(ArcFileContent entity) {
        // 计算完整性状态
        String integrityStatus = "UNKNOWN";
        if (entity.getOriginalHash() != null && entity.getCurrentHash() != null) {
            integrityStatus = entity.getOriginalHash().equals(entity.getCurrentHash()) ? "VALID" : "INVALID";
        }

        // 判断是否有签名
        boolean hasSignature = entity.getSignValue() != null && entity.getSignValue().length > 0;

        return FileContentResponse.builder()
                .id(entity.getId())
                .archivalCode(entity.getArchivalCode())
                .fileName(entity.getFileName())
                .fileType(entity.getFileType())
                .fileSize(entity.getFileSize())
                .fileHash(entity.getFileHash())
                .hashAlgorithm(entity.getHashAlgorithm())
                .itemId(entity.getItemId())
                .originalHash(entity.getOriginalHash())
                .currentHash(entity.getCurrentHash())
                .integrityStatus(integrityStatus)
                .preArchiveStatus(entity.getPreArchiveStatus())
                .checkResultSummary(entity.getCheckResult())
                .checkedTime(entity.getCheckedTime())
                .archivedTime(entity.getArchivedTime())
                .fiscalYear(entity.getFiscalYear())
                .voucherType(entity.getVoucherType())
                .creator(entity.getCreator())
                .fondsCode(entity.getFondsCode())
                .sourceSystem(entity.getSourceSystem())
                .businessDocNo(entity.getBusinessDocNo())
                .erpVoucherNo(entity.getErpVoucherNo())
                .batchId(entity.getBatchId())
                .sequenceInBatch(entity.getSequenceInBatch())
                .summary(entity.getSummary())
                .voucherWord(entity.getVoucherWord())
                .docDate(entity.getDocDate())
                .createdTime(entity.getCreatedTime())
                .hasSignature(hasSignature)
                .signatureStatus(hasSignature ? "SIGNED" : "UNSIGNED")
                .build();
    }

    /**
     * 从 Entity 转换为 DTO（包含签名状态）
     */
    public static FileContentResponse fromEntityWithSignature(ArcFileContent entity, String signatureStatus) {
        String integrityStatus = "UNKNOWN";
        if (entity.getOriginalHash() != null && entity.getCurrentHash() != null) {
            integrityStatus = entity.getOriginalHash().equals(entity.getCurrentHash()) ? "VALID" : "INVALID";
        }

        boolean hasSignature = entity.getSignValue() != null && entity.getSignValue().length > 0;

        return FileContentResponse.builder()
                .id(entity.getId())
                .archivalCode(entity.getArchivalCode())
                .fileName(entity.getFileName())
                .fileType(entity.getFileType())
                .fileSize(entity.getFileSize())
                .fileHash(entity.getFileHash())
                .hashAlgorithm(entity.getHashAlgorithm())
                .itemId(entity.getItemId())
                .originalHash(entity.getOriginalHash())
                .currentHash(entity.getCurrentHash())
                .integrityStatus(integrityStatus)
                .preArchiveStatus(entity.getPreArchiveStatus())
                .checkResultSummary(entity.getCheckResult())
                .checkedTime(entity.getCheckedTime())
                .archivedTime(entity.getArchivedTime())
                .fiscalYear(entity.getFiscalYear())
                .voucherType(entity.getVoucherType())
                .creator(entity.getCreator())
                .fondsCode(entity.getFondsCode())
                .sourceSystem(entity.getSourceSystem())
                .businessDocNo(entity.getBusinessDocNo())
                .erpVoucherNo(entity.getErpVoucherNo())
                .batchId(entity.getBatchId())
                .sequenceInBatch(entity.getSequenceInBatch())
                .summary(entity.getSummary())
                .voucherWord(entity.getVoucherWord())
                .docDate(entity.getDocDate())
                .createdTime(entity.getCreatedTime())
                .hasSignature(hasSignature)
                .signatureStatus(signatureStatus)
                .build();
    }
}
