// Input: Lombok、Java 标准库、MyBatis-Plus、Swagger
// Output: ArchiveResponse DTO 类
// Pos: 数据传输对象
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto.response;

import com.nexusarchive.entity.Archive;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 档案响应 DTO
 * <p>
 * 从 Archive Entity 转换，隐藏以下敏感/大字段：
 * - customMetadata: 自定义元数据（可能包含敏感信息）
 * - standardMetadata: 标准元数据（大JSON字段）
 * - fixityValue: 哈希值（内部校验用）
 * - deleted: 逻辑删除标记
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "档案信息响应")
public class ArchiveResponse {

    /**
     * 档案ID
     */
    @Schema(description = "档案ID", example = "1234567890")
    private String id;

    /**
     * 全宗号 (M9)
     */
    @Schema(description = "全宗号", example = "F001")
    private String fondsNo;

    /**
     * 档号 (M13)
     */
    @Schema(description = "档号", example = "F001-2024-AC01-0001")
    private String archiveCode;

    /**
     * 类别号 (M14)
     */
    @Schema(description = "类别号", example = "AC01")
    private String categoryCode;

    /**
     * 题名 (M22) - 已解密
     */
    @Schema(description = "题名", example = "2024年1月采购凭证")
    private String title;

    /**
     * 年度 (M11)
     */
    @Schema(description = "年度", example = "2024")
    private String fiscalYear;

    /**
     * 会计月份/期间 (M41)
     */
    @Schema(description = "会计期间", example = "01")
    private String fiscalPeriod;

    /**
     * 保管期限 (M12)
     */
    @Schema(description = "保管期限", example = "30Y")
    private String retentionPeriod;

    /**
     * 保管期限起算日期
     */
    @Schema(description = "保管期限起算日期", example = "2024-01-01")
    private LocalDate retentionStartDate;

    /**
     * 立档单位名称 (M6)
     */
    @Schema(description = "立档单位名称", example = "示例公司")
    private String orgName;

    /**
     * 责任者/制单人 (M32) - 已解密
     */
    @Schema(description = "制单人", example = "张三")
    private String creator;

    /**
     * 摘要/说明 - 已解密
     */
    @Schema(description = "摘要", example = "采购办公用品")
    private String summary;

    /**
     * 状态: draft, pending, archived
     */
    @Schema(description = "状态", example = "archived")
    private String status;

    /**
     * 密级: internal, secret, top_secret
     */
    @Schema(description = "密级", example = "internal")
    private String securityLevel;

    /**
     * 存放位置
     */
    @Schema(description = "存放位置", example = "A区-01架-01层")
    private String location;

    /**
     * 所属部门ID
     */
    @Schema(description = "所属部门ID", example = "dept001")
    private String departmentId;

    /**
     * 创建人ID
     */
    @Schema(description = "创建人ID", example = "user001")
    private String createdBy;

    /**
     * 哈希算法类型
     */
    @Schema(description = "哈希算法", example = "SM3")
    private String fixityAlgo;

    /**
     * 唯一单据号
     */
    @Schema(description = "唯一单据号", example = "YonSuite_12345")
    private String uniqueBizId;

    /**
     * 金额
     */
    @Schema(description = "金额", example = "10000.00")
    private BigDecimal amount;

    /**
     * 业务日期
     */
    @Schema(description = "业务日期", example = "2024-01-15")
    private LocalDate docDate;

    /**
     * 所属案卷ID
     */
    @Schema(description = "所属案卷ID", example = "volume001")
    private String volumeId;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间", example = "2024-01-01T10:00:00")
    private LocalDateTime createdTime;

    /**
     * 最后修改时间
     */
    @Schema(description = "最后修改时间", example = "2024-01-01T10:00:00")
    private LocalDateTime lastModifiedTime;

    /**
     * 纸质档案关联号
     */
    @Schema(description = "纸质档案关联号", example = "PAPER-001")
    private String paperRefLink;

    /**
     * 销毁留置状态
     */
    @Schema(description = "销毁留置状态", example = "false")
    private Boolean destructionHold;

    /**
     * 留置原因
     */
    @Schema(description = "留置原因", example = "审计中")
    private String holdReason;

    /**
     * 智能匹配得分
     */
    @Schema(description = "智能匹配得分", example = "95")
    private Integer matchScore;

    /**
     * 销毁状态
     */
    @Schema(description = "销毁状态", example = "PENDING")
    private String destructionStatus;

    /**
     * 关联方式
     */
    @Schema(description = "关联方式", example = "AUTO_AMOUNT")
    private String matchMethod;

    /**
     * 关联文件数量
     */
    @Schema(description = "关联文件数量", example = "3")
    private Integer fileCount;

    /**
     * 关联文件列表 (简化信息)
     */
    @Schema(description = "关联文件列表")
    private List<FileContentSummary> files;

    /**
     * 从 Entity 转换为 DTO
     * 隐藏字段: customMetadata, standardMetadata, fixityValue, deleted
     */
    public static ArchiveResponse fromEntity(Archive entity) {
        return ArchiveResponse.builder()
                .id(entity.getId())
                .fondsNo(entity.getFondsNo())
                .archiveCode(entity.getArchiveCode())
                .categoryCode(entity.getCategoryCode())
                .title(entity.getTitle())
                .fiscalYear(entity.getFiscalYear())
                .fiscalPeriod(entity.getFiscalPeriod())
                .retentionPeriod(entity.getRetentionPeriod())
                .retentionStartDate(entity.getRetentionStartDate())
                .orgName(entity.getOrgName())
                .creator(entity.getCreator())
                .summary(entity.getSummary())
                .status(entity.getStatus())
                .securityLevel(entity.getSecurityLevel())
                .location(entity.getLocation())
                .departmentId(entity.getDepartmentId())
                .createdBy(entity.getCreatedBy())
                .fixityAlgo(entity.getFixityAlgo())
                .uniqueBizId(entity.getUniqueBizId())
                .amount(entity.getAmount())
                .docDate(entity.getDocDate())
                .volumeId(entity.getVolumeId())
                .createdTime(entity.getCreatedTime())
                .lastModifiedTime(entity.getLastModifiedTime())
                .paperRefLink(entity.getPaperRefLink())
                .destructionHold(entity.getDestructionHold())
                .holdReason(entity.getHoldReason())
                .matchScore(entity.getMatchScore())
                .destructionStatus(entity.getDestructionStatus())
                .matchMethod(entity.getMatchMethod())
                .build();
    }

    /**
     * 文件内容摘要 (嵌套 DTO)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "文件内容摘要")
    public static class FileContentSummary {
        @Schema(description = "文件ID")
        private String id;
        @Schema(description = "文件名")
        private String fileName;
        @Schema(description = "文件类型")
        private String fileType;
        @Schema(description = "文件大小(字节)")
        private Long fileSize;
        @Schema(description = "哈希值")
        private String fileHash;
        @Schema(description = "预归档状态")
        private String preArchiveStatus;
    }

    // Manual Setters
    public void setId(String id) { this.id = id; }
    public void setFondsNo(String fondsNo) { this.fondsNo = fondsNo; }
    public void setArchiveCode(String archiveCode) { this.archiveCode = archiveCode; }
    public void setCategoryCode(String categoryCode) { this.categoryCode = categoryCode; }
    public void setTitle(String title) { this.title = title; }
    public void setFiscalYear(String fiscalYear) { this.fiscalYear = fiscalYear; }
    public void setFiscalPeriod(String fiscalPeriod) { this.fiscalPeriod = fiscalPeriod; }
    public void setRetentionPeriod(String retentionPeriod) { this.retentionPeriod = retentionPeriod; }
    public void setRetentionStartDate(LocalDate retentionStartDate) { this.retentionStartDate = retentionStartDate; }
    public void setOrgName(String orgName) { this.orgName = orgName; }
    public void setCreator(String creator) { this.creator = creator; }
    public void setSummary(String summary) { this.summary = summary; }
    public void setStatus(String status) { this.status = status; }
    public void setSecurityLevel(String securityLevel) { this.securityLevel = securityLevel; }
    public void setLocation(String location) { this.location = location; }
    public void setDepartmentId(String departmentId) { this.departmentId = departmentId; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public void setFixityAlgo(String fixityAlgo) { this.fixityAlgo = fixityAlgo; }
    public void setUniqueBizId(String uniqueBizId) { this.uniqueBizId = uniqueBizId; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public void setDocDate(LocalDate docDate) { this.docDate = docDate; }
    public void setVolumeId(String volumeId) { this.volumeId = volumeId; }
    public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }
    public void setLastModifiedTime(LocalDateTime lastModifiedTime) { this.lastModifiedTime = lastModifiedTime; }
    public void setPaperRefLink(String paperRefLink) { this.paperRefLink = paperRefLink; }
    public void setDestructionHold(Boolean destructionHold) { this.destructionHold = destructionHold; }
    public void setHoldReason(String holdReason) { this.holdReason = holdReason; }
    public void setMatchScore(Integer matchScore) { this.matchScore = matchScore; }
    public void setDestructionStatus(String destructionStatus) { this.destructionStatus = destructionStatus; }
    public void setMatchMethod(String matchMethod) { this.matchMethod = matchMethod; }

    // Manual Builder (Already added below...)
    public static ArchiveResponseBuilder builder() { return new ArchiveResponseBuilder(); }

    public static class ArchiveResponseBuilder {
        private ArchiveResponse dto = new ArchiveResponse();
        public ArchiveResponseBuilder id(String id) { dto.setId(id); return this; }
        public ArchiveResponseBuilder fondsNo(String fondsNo) { dto.setFondsNo(fondsNo); return this; }
        public ArchiveResponseBuilder archiveCode(String archiveCode) { dto.setArchiveCode(archiveCode); return this; }
        public ArchiveResponseBuilder categoryCode(String categoryCode) { dto.setCategoryCode(categoryCode); return this; }
        public ArchiveResponseBuilder title(String title) { dto.setTitle(title); return this; }
        public ArchiveResponseBuilder fiscalYear(String fiscalYear) { dto.setFiscalYear(fiscalYear); return this; }
        public ArchiveResponseBuilder fiscalPeriod(String fiscalPeriod) { dto.setFiscalPeriod(fiscalPeriod); return this; }
        public ArchiveResponseBuilder retentionPeriod(String retentionPeriod) { dto.setRetentionPeriod(retentionPeriod); return this; }
        public ArchiveResponseBuilder retentionStartDate(LocalDate retentionStartDate) { dto.setRetentionStartDate(retentionStartDate); return this; }
        public ArchiveResponseBuilder orgName(String orgName) { dto.setOrgName(orgName); return this; }
        public ArchiveResponseBuilder creator(String creator) { dto.setCreator(creator); return this; }
        public ArchiveResponseBuilder summary(String summary) { dto.setSummary(summary); return this; }
        public ArchiveResponseBuilder status(String status) { dto.setStatus(status); return this; }
        public ArchiveResponseBuilder securityLevel(String securityLevel) { dto.setSecurityLevel(securityLevel); return this; }
        public ArchiveResponseBuilder location(String location) { dto.setLocation(location); return this; }
        public ArchiveResponseBuilder departmentId(String departmentId) { dto.setDepartmentId(departmentId); return this; }
        public ArchiveResponseBuilder createdBy(String createdBy) { dto.setCreatedBy(createdBy); return this; }
        public ArchiveResponseBuilder fixityAlgo(String fixityAlgo) { dto.setFixityAlgo(fixityAlgo); return this; }
        public ArchiveResponseBuilder uniqueBizId(String uniqueBizId) { dto.setUniqueBizId(uniqueBizId); return this; }
        public ArchiveResponseBuilder amount(BigDecimal amount) { dto.setAmount(amount); return this; }
        public ArchiveResponseBuilder docDate(LocalDate docDate) { dto.setDocDate(docDate); return this; }
        public ArchiveResponseBuilder volumeId(String volumeId) { dto.setVolumeId(volumeId); return this; }
        public ArchiveResponseBuilder createdTime(LocalDateTime createdTime) { dto.setCreatedTime(createdTime); return this; }
        public ArchiveResponseBuilder lastModifiedTime(LocalDateTime lastModifiedTime) { dto.setLastModifiedTime(lastModifiedTime); return this; }
        public ArchiveResponseBuilder paperRefLink(String paperRefLink) { dto.setPaperRefLink(paperRefLink); return this; }
        public ArchiveResponseBuilder destructionHold(Boolean destructionHold) { dto.setDestructionHold(destructionHold); return this; }
        public ArchiveResponseBuilder holdReason(String holdReason) { dto.setHoldReason(holdReason); return this; }
        public ArchiveResponseBuilder matchScore(Integer matchScore) { dto.setMatchScore(matchScore); return this; }
        public ArchiveResponseBuilder destructionStatus(String destructionStatus) { dto.setDestructionStatus(destructionStatus); return this; }
        public ArchiveResponseBuilder matchMethod(String matchMethod) { dto.setMatchMethod(matchMethod); return this; }
        public ArchiveResponse build() { return dto; }
    }
}
