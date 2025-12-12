package com.nexusarchive.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 电子文件存储记录
 * 对应表: arc_file_content
 */
@TableName("arc_file_content")
public class ArcFileContent {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 关联的档案号 (Item Level)
     */
    private String archivalCode;

    private String fileName;

    private String fileType;

    private Long fileSize;

    private String fileHash;

    private String hashAlgorithm;

    /**
     * 物理存储路径 (相对路径或绝对路径)
     */
    private String storagePath;

    /**
     * 关联单据ID
     */
    private String itemId;

    /**
     * 原始哈希值 (接收时)
     */
    private String originalHash;

    /**
     * 当前哈希值 (巡检时)
     */
    private String currentHash;

    /**
     * 时间戳Token
     */
    private byte[] timestampToken;

    /**
     * 电子签名值
     */
    private byte[] signValue;

    /**
     * 数字证书 (Base64)
     */
    private String certificate;

    // ===== 预归档状态管理 (第一阶段新增) =====

    /**
     * 预归档状态: PENDING_CHECK/CHECK_FAILED/PENDING_METADATA/PENDING_ARCHIVE/ARCHIVED
     */
    private String preArchiveStatus;

    /**
     * 四性检测结果 (JSON格式)
     */
    private String checkResult;

    /**
     * 检测时间
     */
    private LocalDateTime checkedTime;

    /**
     * 归档时间
     */
    private LocalDateTime archivedTime;

    // ===== DA/T 94-2022 必填元数据 =====

    /**
     * 会计年度
     */
    private String fiscalYear;

    /**
     * 凭证类型
     */
    private String voucherType;

    /**
     * 责任者/创建人
     */
    private String creator;

    /**
     * 全宗号
     */
    private String fondsCode;

    /**
     * 来源系统
     */
    private String sourceSystem;

    /**
     * 来源唯一标识（幂等性控制，如 YonSuite_xxx）
     */
    private String businessDocNo;

    /**
     * ERP原始凭证号（用户可读，如 记-3）
     */
    private String erpVoucherNo;

    private LocalDateTime createdTime;

    public ArcFileContent() {
    }

    public ArcFileContent(String id, String archivalCode, String fileName, String fileType, Long fileSize,
            String fileHash, String hashAlgorithm, String storagePath, LocalDateTime createdTime, String itemId,
            String originalHash, String currentHash, byte[] timestampToken, byte[] signValue) {
        this.id = id;
        this.archivalCode = archivalCode;
        this.fileName = fileName;
        this.fileType = fileType;
        this.fileSize = fileSize;
        this.fileHash = fileHash;
        this.hashAlgorithm = hashAlgorithm;
        this.storagePath = storagePath;
        this.createdTime = createdTime;
        this.itemId = itemId;
        this.originalHash = originalHash;
        this.currentHash = currentHash;
        this.timestampToken = timestampToken;
        this.signValue = signValue;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getArchivalCode() {
        return archivalCode;
    }

    public void setArchivalCode(String archivalCode) {
        this.archivalCode = archivalCode;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getFileHash() {
        return fileHash;
    }

    public void setFileHash(String fileHash) {
        this.fileHash = fileHash;
    }

    public String getHashAlgorithm() {
        return hashAlgorithm;
    }

    public void setHashAlgorithm(String hashAlgorithm) {
        this.hashAlgorithm = hashAlgorithm;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }

    public LocalDateTime getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(LocalDateTime createdTime) {
        this.createdTime = createdTime;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public String getOriginalHash() {
        return originalHash;
    }

    public void setOriginalHash(String originalHash) {
        this.originalHash = originalHash;
    }

    public String getCurrentHash() {
        return currentHash;
    }

    public void setCurrentHash(String currentHash) {
        this.currentHash = currentHash;
    }

    public byte[] getTimestampToken() {
        return timestampToken;
    }

    public void setTimestampToken(byte[] timestampToken) {
        this.timestampToken = timestampToken;
    }

    public byte[] getSignValue() {
        return signValue;
    }

    public void setSignValue(byte[] signValue) {
        this.signValue = signValue;
    }

    public String getCertificate() {
        return certificate;
    }

    public void setCertificate(String certificate) {
        this.certificate = certificate;
    }

    // New field getters/setters
    public String getPreArchiveStatus() {
        return preArchiveStatus;
    }

    public void setPreArchiveStatus(String preArchiveStatus) {
        this.preArchiveStatus = preArchiveStatus;
    }

    public String getCheckResult() {
        return checkResult;
    }

    public void setCheckResult(String checkResult) {
        this.checkResult = checkResult;
    }

    public LocalDateTime getCheckedTime() {
        return checkedTime;
    }

    public void setCheckedTime(LocalDateTime checkedTime) {
        this.checkedTime = checkedTime;
    }

    public LocalDateTime getArchivedTime() {
        return archivedTime;
    }

    public void setArchivedTime(LocalDateTime archivedTime) {
        this.archivedTime = archivedTime;
    }

    public String getFiscalYear() {
        return fiscalYear;
    }

    public void setFiscalYear(String fiscalYear) {
        this.fiscalYear = fiscalYear;
    }

    public String getVoucherType() {
        return voucherType;
    }

    public void setVoucherType(String voucherType) {
        this.voucherType = voucherType;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public String getFondsCode() {
        return fondsCode;
    }

    public void setFondsCode(String fondsCode) {
        this.fondsCode = fondsCode;
    }

    public String getSourceSystem() {
        return sourceSystem;
    }

    public void setSourceSystem(String sourceSystem) {
        this.sourceSystem = sourceSystem;
    }

    public String getBusinessDocNo() {
        return businessDocNo;
    }

    public void setBusinessDocNo(String businessDocNo) {
        this.businessDocNo = businessDocNo;
    }

    public String getErpVoucherNo() {
        return erpVoucherNo;
    }

    public void setErpVoucherNo(String erpVoucherNo) {
        this.erpVoucherNo = erpVoucherNo;
    }

    public static ArcFileContentBuilder builder() {
        return new ArcFileContentBuilder();
    }

    public static class ArcFileContentBuilder {
        private ArcFileContent entity = new ArcFileContent();

        public ArcFileContentBuilder id(String id) {
            entity.setId(id);
            return this;
        }

        public ArcFileContentBuilder archivalCode(String archivalCode) {
            entity.setArchivalCode(archivalCode);
            return this;
        }

        public ArcFileContentBuilder fileName(String fileName) {
            entity.setFileName(fileName);
            return this;
        }

        public ArcFileContentBuilder fileType(String fileType) {
            entity.setFileType(fileType);
            return this;
        }

        public ArcFileContentBuilder fileSize(Long fileSize) {
            entity.setFileSize(fileSize);
            return this;
        }

        public ArcFileContentBuilder fileHash(String fileHash) {
            entity.setFileHash(fileHash);
            return this;
        }

        public ArcFileContentBuilder hashAlgorithm(String hashAlgorithm) {
            entity.setHashAlgorithm(hashAlgorithm);
            return this;
        }

        public ArcFileContentBuilder storagePath(String storagePath) {
            entity.setStoragePath(storagePath);
            return this;
        }

        public ArcFileContentBuilder createdTime(LocalDateTime createdTime) {
            entity.setCreatedTime(createdTime);
            return this;
        }

        public ArcFileContentBuilder itemId(String itemId) {
            entity.setItemId(itemId);
            return this;
        }

        public ArcFileContentBuilder originalHash(String originalHash) {
            entity.setOriginalHash(originalHash);
            return this;
        }

        public ArcFileContentBuilder currentHash(String currentHash) {
            entity.setCurrentHash(currentHash);
            return this;
        }

        public ArcFileContentBuilder timestampToken(byte[] timestampToken) {
            entity.setTimestampToken(timestampToken);
            return this;
        }

        public ArcFileContentBuilder signValue(byte[] signValue) {
            entity.setSignValue(signValue);
            return this;
        }

        public ArcFileContentBuilder certificate(String certificate) {
            entity.setCertificate(certificate);
            return this;
        }

        public ArcFileContentBuilder preArchiveStatus(String preArchiveStatus) {
            entity.setPreArchiveStatus(preArchiveStatus);
            return this;
        }

        public ArcFileContentBuilder checkResult(String checkResult) {
            entity.setCheckResult(checkResult);
            return this;
        }

        public ArcFileContentBuilder checkedTime(LocalDateTime checkedTime) {
            entity.setCheckedTime(checkedTime);
            return this;
        }

        public ArcFileContentBuilder archivedTime(LocalDateTime archivedTime) {
            entity.setArchivedTime(archivedTime);
            return this;
        }

        public ArcFileContentBuilder fiscalYear(String fiscalYear) {
            entity.setFiscalYear(fiscalYear);
            return this;
        }

        public ArcFileContentBuilder voucherType(String voucherType) {
            entity.setVoucherType(voucherType);
            return this;
        }

        public ArcFileContentBuilder creator(String creator) {
            entity.setCreator(creator);
            return this;
        }

        public ArcFileContentBuilder fondsCode(String fondsCode) {
            entity.setFondsCode(fondsCode);
            return this;
        }

        public ArcFileContentBuilder sourceSystem(String sourceSystem) {
            entity.setSourceSystem(sourceSystem);
            return this;
        }

        public ArcFileContentBuilder businessDocNo(String businessDocNo) {
            entity.setBusinessDocNo(businessDocNo);
            return this;
        }

        public ArcFileContentBuilder erpVoucherNo(String erpVoucherNo) {
            entity.setErpVoucherNo(erpVoucherNo);
            return this;
        }

        public ArcFileContent build() {
            return entity;
        }
    }
}
