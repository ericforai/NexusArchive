package com.nexusarchive.integration.yonsuite.mapper;

import com.nexusarchive.entity.Archive;
import com.nexusarchive.integration.yonsuite.dto.YonVoucherDetailResponse;
import com.nexusarchive.integration.yonsuite.dto.YonVoucherListResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
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
}
