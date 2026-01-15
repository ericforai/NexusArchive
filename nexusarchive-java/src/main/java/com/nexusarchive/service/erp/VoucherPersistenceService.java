// Input: VoucherDTO, ArcFileContent, Archive mappers, voucherJson
// Output: VoucherPersistenceService
// Pos: Service Layer
// 负责凭证持久化操作

package com.nexusarchive.service.erp;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.entity.ArcFileMetadataIndex;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.engine.ErpMappingEngine;
import com.nexusarchive.integration.erp.dto.VoucherDTO;
import com.nexusarchive.integration.erp.adapter.ErpAdapter;
import com.nexusarchive.mapper.ArcFileContentMapper;
import com.nexusarchive.mapper.ArcFileMetadataIndexMapper;
import com.nexusarchive.mapper.ArchiveMapper;
import com.nexusarchive.service.VoucherPdfGeneratorService;
import com.nexusarchive.security.FondsContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 凭证持久化服务
 *
 * <p>职责：</p>
 * <ul>
 *   <li>DTO → Entity 映射（ArcFileContent、Archive）</li>
 *   <li>存储路径设置</li>
 *   <li>元数据索引保存</li>
 *   <li>PDF 生成协调</li>
 * </ul>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class VoucherPersistenceService {

    private final ArcFileContentMapper arcFileContentMapper;
    private final ArcFileMetadataIndexMapper arcFileMetadataIndexMapper;
    private final ArchiveMapper archiveMapper;
    private final VoucherPdfGeneratorService pdfGeneratorService;
    private final ErpMappingEngine mappingEngine;

    @Value("${archive.root.path:./data/archives}")
    private String archiveRootPath;

    private static final String DEFAULT_FONDS_CODE = "DEFAULT";
    private static final String CATEGORY_CODE_VOUCHER = "AC01";
    private static final String RETENTION_PERIOD_30Y = "30Y";
    private static final String STATUS_DRAFT = "draft";
    private static final String PARSER_TYPE_ERP_SYNC = "ERP_SYNC";

    /**
     * 保存凭证到数据库
     *
     * @param dto 凭证 DTO
     * @param mappingConfig 映射配置
     * @param adapter 适配器
     * @param startDate 同步开始日期
     * @param sourceSystemName 源系统名称
     * @param voucherJson 原始凭证 JSON（用于生成 PDF）
     * @return 保存的文件内容实体，如果凭证已存在则返回 null
     */
    public ArcFileContent saveVoucher(VoucherDTO dto,
                                      cn.hutool.json.JSONObject mappingConfig,
                                      ErpAdapter adapter,
                                      LocalDate startDate,
                                      String sourceSystemName,
                                      String voucherJson) {
        // 检查凭证是否已存在（基于 erp_voucher_no）
        String voucherNo = dto.getVoucherNo();
        if (isVoucherExist(voucherNo)) {
            log.info("凭证已存在，跳过保存: voucherNo={}", voucherNo);
            return null;
        }

        ArcFileContent fileContent = mapToFileContent(dto, mappingConfig, startDate);
        fileContent.setSourceSystem(adapter.getName());

        setStoragePath(fileContent);
        setDefaultFields(fileContent, startDate);
        if (voucherJson != null && !voucherJson.isEmpty()) {
            fileContent.setSourceData(voucherJson);
        }

        // 保存文件内容
        arcFileContentMapper.insert(fileContent);

        // 生成 PDF
        generatePdf(fileContent);

        // 创建档案记录
        Archive archive = createArchiveFromVoucher(dto, fileContent, sourceSystemName);
        archiveMapper.insert(archive);
        log.info("创建档案记录: archiveCode={}, title={}", archive.getArchiveCode(), archive.getTitle());

        // 保存元数据索引
        saveMetadataIndex(fileContent, dto);

        return fileContent;
    }

    /**
     * 检查凭证是否已存在
     *
     * @param voucherNo 凭证号
     * @return 是否存在
     */
    public boolean isVoucherExist(String voucherNo) {
        // 精确查重：基于 erp_voucher_no 字段进行精确匹配
        return arcFileContentMapper.selectCount(
            new LambdaQueryWrapper<ArcFileContent>()
                .eq(ArcFileContent::getErpVoucherNo, voucherNo)) > 0;
    }

    /**
     * 映射 DTO 到文件内容
     */
    private ArcFileContent mapToFileContent(VoucherDTO dto,
                                            cn.hutool.json.JSONObject mappingConfig,
                                            LocalDate startDate) {
        if (mappingConfig != null) {
            log.info("提取映射配置，执行动态字段映射...");
            cn.hutool.json.JSONObject sourceJson = cn.hutool.json.JSONUtil.parseObj(dto);
            return mappingEngine.mapToArcFileContent(sourceJson, mappingConfig);
        } else {
            return VoucherMapper.toArcFileContent(dto);
        }
    }

    /**
     * 设置存储路径
     */
    private void setStoragePath(ArcFileContent fileContent) {
        if (fileContent.getStoragePath() == null || fileContent.getStoragePath().isEmpty()) {
            String fondsCode = fileContent.getFondsCode() != null ? fileContent.getFondsCode() : DEFAULT_FONDS_CODE;
            String fileName = fileContent.getFileName();
            String storagePath = Paths.get(archiveRootPath, "pre-archive", fondsCode, fileName).toString();
            fileContent.setStoragePath(storagePath);
        }
    }

    /**
     * 设置默认字段
     * 使用当前全宗上下文（从 FondsContext 获取）而不是硬编码的 DEFAULT
     */
    private void setDefaultFields(ArcFileContent fileContent, LocalDate startDate) {
        if (fileContent.getFondsCode() == null) {
            // 优先使用当前全宗上下文，其次使用默认值
            String currentFonds = FondsContext.getCurrentFondsNo();
            fileContent.setFondsCode(currentFonds != null ? currentFonds : DEFAULT_FONDS_CODE);
            log.debug("设置全宗代码: {} (来源: {})", fileContent.getFondsCode(),
                currentFonds != null ? "FondsContext" : "DEFAULT");
        }
        if (fileContent.getFiscalYear() == null) {
            fileContent.setFiscalYear(String.valueOf(startDate.getYear()));
        }
    }

    /**
     * 生成 PDF 文件
     */
    private void generatePdf(ArcFileContent fileContent) {
        try {
            String voucherJson = fileContent.getSourceData();
            if (voucherJson != null && !voucherJson.isEmpty()) {
                pdfGeneratorService.generatePdfForPreArchive(fileContent.getId(), voucherJson);
                log.info("PDF 生成成功: {}", fileContent.getErpVoucherNo());
            }
        } catch (Exception pdfEx) {
            log.warn("PDF 生成失败，不影响同步: {}", pdfEx.getMessage());
        }
    }

    /**
     * 保存元数据索引
     */
    private void saveMetadataIndex(ArcFileContent fileContent, VoucherDTO dto) {
        BigDecimal amount = dto.getDebitTotal() != null ? dto.getDebitTotal() : dto.getCreditTotal();
        if (amount != null) {
            ArcFileMetadataIndex metadataIndex = ArcFileMetadataIndex.builder()
                .fileId(fileContent.getId())
                .totalAmount(amount)
                .invoiceNumber(dto.getVoucherNo())
                .issueDate(dto.getVoucherDate() != null ? dto.getVoucherDate() : LocalDate.now())
                .parsedTime(LocalDateTime.now())
                .parserType(PARSER_TYPE_ERP_SYNC)
                .build();
            arcFileMetadataIndexMapper.insert(metadataIndex);
        }
    }

    /**
     * 从 ERP 凭证 DTO 创建档案记录
     */
    private Archive createArchiveFromVoucher(VoucherDTO dto, ArcFileContent fileContent, String sourceSystem) {
        Archive archive = new Archive();
        archive.setId(fileContent.getId());

        archive.setArchiveCode(fileContent.getArchivalCode());

        // 题名：优先使用 fileContent 中已解析好的 summary（如 "收款单: xxx, 客户: xxx"）
        // 而不是 dto 中的原始哈希字符串
        String title = "会计凭证-" + dto.getVoucherNo();
        String contentSummary = fileContent.getSummary();
        if (contentSummary != null && !contentSummary.isEmpty()) {
            title = contentSummary;
        }
        archive.setTitle(title);
        archive.setSummary(contentSummary);

        // 分类号
        archive.setCategoryCode(CATEGORY_CODE_VOUCHER);

        // 年度
        String fiscalYear = fileContent.getFiscalYear();
        if (fiscalYear == null && dto.getVoucherDate() != null) {
            fiscalYear = String.valueOf(dto.getVoucherDate().getYear());
        }
        if (fiscalYear == null) {
            fiscalYear = String.valueOf(LocalDate.now().getYear());
        }
        archive.setFiscalYear(fiscalYear);

        // 会计期间
        if (dto.getAccountPeriod() != null) {
            archive.setFiscalPeriod(dto.getAccountPeriod());
        }

        archive.setRetentionPeriod(RETENTION_PERIOD_30Y);
        archive.setFondsNo(fileContent.getFondsCode() != null ? fileContent.getFondsCode() : DEFAULT_FONDS_CODE);
        archive.setOrgName(sourceSystem);

        // 金额
        BigDecimal amount = dto.getDebitTotal() != null ? dto.getDebitTotal() : dto.getCreditTotal();
        archive.setAmount(amount);

        archive.setDocDate(dto.getVoucherDate() != null ? dto.getVoucherDate() : LocalDate.now());
        archive.setCreator(dto.getCreator());
        archive.setStatus(STATUS_DRAFT);
        archive.setUniqueBizId(sourceSystem + "_" + dto.getVoucherId());

        return archive;
    }
}
