package com.nexusarchive.integration.yonsuite.mapper;

import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.entity.enums.PreArchiveStatus;
import com.nexusarchive.integration.yonsuite.dto.YonVoucherDetailResponse;
import com.nexusarchive.integration.yonsuite.dto.YonVoucherListResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 用友凭证到 Canonical 模型的映射器
 */
@Component
@Slf4j
public class YonVoucherMapper {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    public YonVoucherMapper(com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 从列表查询结果映射到 Archive
     */
    public Archive fromListRecord(YonVoucherListResponse.VoucherRecord record, String sourceSystem) {
        if (record == null || record.getHeader() == null) {
            return null;
        }
        
        YonVoucherListResponse.VoucherHeader header = record.getHeader();
        
        Archive archive = new Archive();
        
        // 基础信息
        archive.setTitle("会计凭证-" + header.getDisplayname());
        archive.setCategoryCode("AC01"); // 会计凭证
        archive.setStatus("draft"); // 同步后为草稿状态
        archive.setSecurityLevel("internal");
        
        // 期间信息
        if (header.getPeriod() != null && header.getPeriod().length() >= 4) {
            archive.setFiscalYear(header.getPeriod().substring(0, 4));
            archive.setFiscalPeriod(header.getPeriod());
        }
        
        // 金额 (借方合计)
        archive.setAmount(header.getTotalDebitOrg() != null ? header.getTotalDebitOrg() : BigDecimal.ZERO);
        
        // 制单人
        if (header.getMaker() != null) {
            archive.setCreator(header.getMaker().getName());
        }
        
        // 账簿信息 -> 全宗号
        if (header.getAccbook() != null) {
            archive.setFondsNo(header.getAccbook().getCode());
            if (header.getAccbook().getPkOrg() != null) {
                archive.setOrgName(header.getAccbook().getPkOrg().getName());
            }
        }
        
        // 凭证日期
        if (header.getMaketime() != null) {
            try {
                archive.setDocDate(LocalDate.parse(header.getMaketime(), DATE_FORMATTER));
            } catch (Exception e) {
                log.warn("Failed to parse maketime: {}", header.getMaketime());
            }
        }
        
        // 唯一业务ID (用于幂等性)
        archive.setUniqueBizId(sourceSystem + "_" + header.getId());
        
        // 生成档号 (格式: YS-年月-凭证号)
        String displayName = header.getDisplayname() != null ? header.getDisplayname() : header.getId();
        archive.setArchiveCode("YS-" + (header.getPeriod() != null ? header.getPeriod() : "0000-00") + "-" + displayName);
        
        // 状态映射
        archive.setStatus(mapVoucherStatus(header.getVoucherstatus()));
        
        // 保管期限 (默认10年)
        archive.setRetentionPeriod("10Y");
        
        // 序列化凭证分录到自定义元数据
        if (record.getBody() != null) {
            try {
                archive.setCustomMetadata(objectMapper.writeValueAsString(record.getBody()));
            } catch (Exception e) {
                log.warn("Failed to serialize voucher body: {}", header.getId(), e);
            }
        }
        
        return archive;
    }

    /**
     * 从详情查询结果映射到 Archive
     */
    public Archive fromDetail(YonVoucherDetailResponse.VoucherDetail detail, String sourceSystem) {
        if (detail == null) {
            return null;
        }
        
        Archive archive = new Archive();
        
        // 基础信息
        archive.setTitle("会计凭证-" + detail.getDisplayName());
        archive.setCategoryCode("AC01");
        archive.setSecurityLevel("internal");
        
        // 期间信息
        if (detail.getPeriodUnion() != null && detail.getPeriodUnion().length() >= 4) {
            archive.setFiscalYear(detail.getPeriodUnion().substring(0, 4));
            archive.setFiscalPeriod(detail.getPeriodUnion());
        }
        
        // 金额
        archive.setAmount(detail.getTotalDebitOrg() != null ? detail.getTotalDebitOrg() : BigDecimal.ZERO);
        
        // 制单人
        if (detail.getMakerObj() != null) {
            archive.setCreator(detail.getMakerObj().getName());
        }
        
        // 账簿信息
        if (detail.getAccBookObj() != null) {
            archive.setFondsNo(detail.getAccBookObj().getCode());
        }
        
        // 凭证日期
        if (detail.getMakeTime() != null) {
            try {
                archive.setDocDate(LocalDate.parse(detail.getMakeTime(), DATE_FORMATTER));
            } catch (Exception e) {
                log.warn("Failed to parse makeTime: {}", detail.getMakeTime());
            }
        }
        
        // 唯一业务ID
        archive.setUniqueBizId(sourceSystem + "_" + detail.getId());
        
        // 状态映射
        archive.setStatus(mapVoucherStatus(detail.getVoucherStatus()));
        
        // 保管期限
        archive.setRetentionPeriod("10Y");
        
        // 序列化凭证分录到自定义元数据
        if (detail.getBodies() != null) {
            try {
                archive.setCustomMetadata(objectMapper.writeValueAsString(detail.getBodies()));
            } catch (Exception e) {
                log.warn("Failed to serialize voucher bodies: {}", detail.getId(), e);
            }
        }
        
        return archive;
    }

    /**
     * YonSuite 状态码到系统状态的映射
     * 00:暂存 01:保存 02:纠错 03:审核 04:记账 05:作废
     */
    private String mapVoucherStatus(String yonStatus) {
        if (yonStatus == null) {
            return "draft";
        }
        switch (yonStatus) {
            case "00":
            case "01":
            case "02":
                return "draft";
            case "03":
                return "pending"; // 已审核 -> 待归档
            case "04":
                return "archived"; // 已记账 -> 可归档
            case "05":
                return "deleted"; // 作废
            default:
                return "draft";
        }
    }

    /**
     * 将凭证详情映射到预归档文件表 (ArcFileContent)
     * 用于 ERP 同步进入预归档库的场景
     * 
     * @param detail 凭证详情
     * @param sourceSystem 来源系统标识
     * @return ArcFileContent 预归档文件记录
     */
    public ArcFileContent toPreArchiveFile(YonVoucherDetailResponse.VoucherDetail detail, String sourceSystem) {
        if (detail == null) {
            return null;
        }
        
        // 生成临时档号 (待正式归档时会重新生成)
        String period = detail.getPeriodUnion() != null ? detail.getPeriodUnion() : "0000-00";
        String displayName = detail.getDisplayName() != null ? detail.getDisplayName() : detail.getId();
        String archivalCode = "YS-" + period + "-" + displayName;
        
        // 生成业务单据号
        String businessDocNo = sourceSystem + "_" + detail.getId();
        
        // 序列化凭证分录为 JSON (用于作为虚拟文件内容)
        String voucherJson = "";
        if (detail.getBodies() != null) {
            try {
                voucherJson = objectMapper.writeValueAsString(detail);
            } catch (Exception e) {
                log.warn("Failed to serialize voucher detail: {}", detail.getId(), e);
            }
        }
        
        // 计算 JSON 内容的大小和哈希
        long fileSize = voucherJson.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
        String fileHash = calculateHash(voucherJson);
        
        // 临时存储路径 (PDF 生成后会更新为实际路径)
        String tempStoragePath = "pending/" + sourceSystem + "/" + businessDocNo + ".json";
        
        return ArcFileContent.builder()
                .archivalCode(archivalCode)
                .fileName("会计凭证-" + displayName + ".json")
                .fileType("application/json")
                .fileSize(fileSize)
                .fileHash(fileHash)
                .hashAlgorithm("SM3")
                .storagePath(tempStoragePath)
                .preArchiveStatus(PreArchiveStatus.PENDING_CHECK.getCode())
                .sourceSystem(sourceSystem)
                .businessDocNo(businessDocNo)
                .erpVoucherNo(displayName)  // 用户可读的凭证号
                .fiscalYear(period.length() >= 4 ? period.substring(0, 4) : null)
                .voucherType("AC01")
                .creator(detail.getMakerObj() != null ? detail.getMakerObj().getName() : null)
                .fondsCode(detail.getAccBookObj() != null ? detail.getAccBookObj().getCode() : null)
                .createdTime(LocalDateTime.now())
                .build();
    }

    /**
     * 将列表记录映射到预归档文件表 (ArcFileContent)
     */
    public ArcFileContent toPreArchiveFile(YonVoucherListResponse.VoucherRecord record, String sourceSystem) {
        if (record == null || record.getHeader() == null) {
            return null;
        }
        
        YonVoucherListResponse.VoucherHeader header = record.getHeader();
        
        // 生成临时档号
        String period = header.getPeriod() != null ? header.getPeriod() : "0000-00";
        String displayName = header.getDisplayname() != null ? header.getDisplayname() : header.getId();
        String archivalCode = "YS-" + period + "-" + displayName;
        
        // 生成业务单据号
        String businessDocNo = sourceSystem + "_" + header.getId();
        
        // 序列化凭证为 JSON
        String voucherJson = "";
        try {
            voucherJson = objectMapper.writeValueAsString(record);
        } catch (Exception e) {
            log.warn("Failed to serialize voucher record: {}", header.getId(), e);
        }
        
        long fileSize = voucherJson.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
        String fileHash = calculateHash(voucherJson);
        
        // 临时存储路径 (PDF 生成后会更新为实际路径)
        String tempStoragePath = "pending/" + sourceSystem + "/" + businessDocNo + ".json";
        
        return ArcFileContent.builder()
                .archivalCode(archivalCode)
                .fileName("会计凭证-" + displayName + ".json")
                .fileType("application/json")
                .fileSize(fileSize)
                .fileHash(fileHash)
                .hashAlgorithm("SM3")
                .storagePath(tempStoragePath)
                .preArchiveStatus(PreArchiveStatus.PENDING_CHECK.getCode())
                .sourceSystem(sourceSystem)
                .businessDocNo(businessDocNo)
                .erpVoucherNo(displayName)  // 用户可读的凭证号
                .fiscalYear(period.length() >= 4 ? period.substring(0, 4) : null)
                .voucherType("AC01")
                .creator(header.getMaker() != null ? header.getMaker().getName() : null)
                .fondsCode(header.getAccbook() != null ? header.getAccbook().getCode() : null)
                .createdTime(LocalDateTime.now())
                .build();
    }

    /**
     * 计算字符串的 SM3 哈希值
     */
    private String calculateHash(String content) {
        try {
            byte[] data = content.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            // 使用 BouncyCastle SM3
            org.bouncycastle.crypto.digests.SM3Digest digest = new org.bouncycastle.crypto.digests.SM3Digest();
            digest.update(data, 0, data.length);
            byte[] hash = new byte[digest.getDigestSize()];
            digest.doFinal(hash, 0);
            // 转换为十六进制字符串
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            log.warn("Failed to calculate SM3 hash, falling back to empty", e);
            return "";
        }
    }
}
