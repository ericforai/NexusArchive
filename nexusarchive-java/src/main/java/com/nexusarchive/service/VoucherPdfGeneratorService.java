package com.nexusarchive.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.entity.enums.PreArchiveStatus;
import com.nexusarchive.mapper.ArcFileContentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.bouncycastle.crypto.digests.SM3Digest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 凭证 PDF 生成服务
 * 负责将同步的 YonSuite 凭证数据生成为可预览的 PDF 文件
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class VoucherPdfGeneratorService {

    private final ArcFileContentMapper arcFileContentMapper;
    private final ObjectMapper objectMapper;

    @Value("${archive.root.path:./data/archives}")
    private String archiveRootPath;

    /**
     * 为预归档记录生成 PDF 文件
     * 
     * @param fileId      预归档记录 ID
     * @param voucherJson 凭证 JSON 数据
     * @return 更新后的 ArcFileContent 记录
     */
    public ArcFileContent generatePdfForPreArchive(String fileId, String voucherJson) {
        log.info("开始为预归档记录生成 PDF: fileId={}", fileId);

        ArcFileContent fileContent = arcFileContentMapper.selectById(fileId);
        if (fileContent == null) {
            log.error("预归档记录不存在: {}", fileId);
            return null;
        }

        try {
            // 1. 解析凭证数据
            JsonNode voucherData = objectMapper.readTree(voucherJson);

            // 2. 创建存储目录
            String fondsCode = fileContent.getFondsCode() != null ? fileContent.getFondsCode() : "DEFAULT";
            Path storageDir = Paths.get(archiveRootPath, "pre-archive", fondsCode);
            Files.createDirectories(storageDir);

            // 3. 生成 PDF 文件名 (使用ERP凭证号或业务单据号)
            String docNo = fileContent.getErpVoucherNo() != null ? fileContent.getErpVoucherNo()
                    : (fileContent.getBusinessDocNo() != null ? fileContent.getBusinessDocNo() : fileId);
            String pdfFileName = docNo + ".pdf";
            Path pdfPath = storageDir.resolve(pdfFileName);

            // 4. 根据单据类型选择 PDF 生成器
            String voucherType = fileContent.getVoucherType();
            if ("COLLECTION_BILL".equals(voucherType)) {
                // 收款单专用生成器
                log.debug("生成收款单 PDF: target={}", pdfPath);
                generateCollectionBillPdf(pdfPath, fileContent, voucherData);
            } else {
                // 默认会计凭证生成器
                generateVoucherPdf(pdfPath, fileContent, voucherData);
            }

            // 5. 计算文件哈希和大小
            byte[] pdfBytes = Files.readAllBytes(pdfPath);
            String fileHash = calculateSM3Hash(pdfBytes);
            long fileSize = pdfBytes.length;

            // 6. 更新 arc_file_content 记录
            fileContent.setFileName(pdfFileName);
            fileContent.setFileType("application/pdf");
            fileContent.setFileSize(fileSize);
            fileContent.setFileHash(fileHash);
            fileContent.setHashAlgorithm("SM3");
            fileContent.setStoragePath(pdfPath.toString());
            fileContent.setOriginalHash(fileHash);
            fileContent.setCurrentHash(fileHash);

            arcFileContentMapper.updateById(fileContent);

            log.info("PDF 生成成功: fileId={}, path={}, size={}", fileId, pdfPath, fileSize);
            return fileContent;

        } catch (Exception e) {
            log.error("PDF 生成失败: fileId={}", fileId, e);
            return null;
        }
    }

    /**
     * 生成收款单 PDF 文件
     * 参考用友YonSuite收款单样式，包含表头和收款明细
     */
    private void generateCollectionBillPdf(Path targetPath, ArcFileContent fileContent, JsonNode voucherData)
            throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            // 加载中文字体
            org.apache.pdfbox.pdmodel.font.PDFont chineseFont = loadChineseFont(document);
            boolean useChinese = chineseFont != null;
            org.apache.pdfbox.pdmodel.font.PDFont regularFont = useChinese ? chineseFont : PDType1Font.HELVETICA;
            org.apache.pdfbox.pdmodel.font.PDFont boldFont = useChinese ? chineseFont : PDType1Font.HELVETICA_BOLD;

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                int margin = 40;
                float yPosition = 750;

                // === 标题 ===
                contentStream.beginText();
                contentStream.setFont(boldFont, 18);
                contentStream.newLineAtOffset(220, yPosition);
                contentStream.showText(useChinese ? "收 款 单" : "Collection Bill");
                contentStream.endText();
                yPosition -= 35;

                // === 表头信息 (两列布局) ===
                // 从 VoucherDTO 解析数据 (如果 voucherData 为空，则提供默认值)
                String billCode = fileContent.getErpVoucherNo() != null ? fileContent.getErpVoucherNo() : "";
                String summary = voucherData != null ? voucherData.path("summary").asText("") : "";
                String creator = fileContent.getCreator() != null ? fileContent.getCreator() : "";
                String accountPeriod = voucherData != null ? voucherData.path("accountPeriod").asText("") : "";
                String voucherNo = voucherData != null ? voucherData.path("voucherNo").asText(billCode) : billCode;

                // 从 summary 解析客户名和金额
                String customerName = "-";
                String amount = "-";
                if (!summary.isEmpty()) {
                    if (summary.contains("客户:")) {
                        int start = summary.indexOf("客户:") + 3;
                        int end = summary.indexOf(",", start);
                        if (end > start)
                            customerName = summary.substring(start, end).trim();
                    }
                    if (summary.contains("金额:")) {
                        int start = summary.indexOf("金额:") + 3;
                        int end = summary.indexOf(" CNY", start);
                        if (end > start)
                            amount = summary.substring(start, end).trim();
                    }
                }

                contentStream.setFont(regularFont, 10);

                // 第一行: 单据编号、交易类型
                contentStream.beginText();
                contentStream.newLineAtOffset(margin, yPosition);
                contentStream.showText(safeText("单据编号: " + voucherNo, useChinese));
                contentStream.newLineAtOffset(250, 0);
                contentStream.showText(safeText("交易类型: 销售收款", useChinese));
                contentStream.endText();
                yPosition -= 18;

                // 第二行: 单据日期、会计期间
                String billDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                contentStream.beginText();
                contentStream.newLineAtOffset(margin, yPosition);
                contentStream.showText(safeText("单据日期: " + billDate, useChinese));
                contentStream.newLineAtOffset(250, 0);
                contentStream.showText(safeText("会计期间: " + accountPeriod, useChinese));
                contentStream.endText();
                yPosition -= 18;

                // 第三行: 客户
                contentStream.beginText();
                contentStream.newLineAtOffset(margin, yPosition);
                contentStream.showText(safeText("客户: " + customerName, useChinese));
                contentStream.newLineAtOffset(250, 0);
                contentStream.showText(safeText("往来对象类型: 客户", useChinese));
                contentStream.endText();
                yPosition -= 18;

                // 第四行: 来源系统、制单人
                contentStream.beginText();
                contentStream.newLineAtOffset(margin, yPosition);
                contentStream.showText(safeText("来源系统: "
                        + (fileContent.getSourceSystem() != null ? fileContent.getSourceSystem() : "用友YonSuite"),
                        useChinese));
                contentStream.newLineAtOffset(250, 0);
                contentStream.showText(safeText("创建人: " + creator, useChinese));
                contentStream.endText();
                yPosition -= 30;

                // === 分隔线 ===
                contentStream.moveTo(margin, yPosition);
                contentStream.lineTo(560, yPosition);
                contentStream.stroke();
                yPosition -= 10;

                // === 收款明细表头 ===
                contentStream.beginText();
                contentStream.setFont(boldFont, 10);
                contentStream.newLineAtOffset(margin, yPosition);
                contentStream.showText(safeText("收款明细", useChinese));
                contentStream.endText();
                yPosition -= 20;

                // 表格表头
                contentStream.beginText();
                contentStream.setFont(boldFont, 9);
                contentStream.newLineAtOffset(margin, yPosition);
                contentStream.showText(safeText("序号", useChinese));
                contentStream.newLineAtOffset(40, 0);
                contentStream.showText(safeText("款项类型", useChinese));
                contentStream.newLineAtOffset(80, 0);
                contentStream.showText(safeText("收款金额", useChinese));
                contentStream.newLineAtOffset(80, 0);
                contentStream.showText(safeText("本币金额", useChinese));
                contentStream.newLineAtOffset(80, 0);
                contentStream.showText(safeText("客户", useChinese));
                contentStream.newLineAtOffset(120, 0);
                contentStream.showText(safeText("备注", useChinese));
                contentStream.endText();
                yPosition -= 15;

                // 表格线
                contentStream.moveTo(margin, yPosition + 10);
                contentStream.lineTo(560, yPosition + 10);
                contentStream.stroke();

                // === 表体数据 ===
                contentStream.setFont(regularFont, 9);
                // 简化版：只显示一行汇总数据
                contentStream.beginText();
                contentStream.newLineAtOffset(margin, yPosition);
                contentStream.showText("1");
                contentStream.newLineAtOffset(40, 0);
                contentStream.showText(safeText("预收款", useChinese));
                contentStream.newLineAtOffset(80, 0);
                contentStream.showText(amount.isEmpty() ? "-" : amount);
                contentStream.newLineAtOffset(80, 0);
                contentStream.showText(amount.isEmpty() ? "-" : amount);
                contentStream.newLineAtOffset(80, 0);
                contentStream.showText(safeText(truncateText(customerName, 15), useChinese));
                contentStream.newLineAtOffset(120, 0);
                contentStream.showText("-");
                contentStream.endText();
                yPosition -= 20;

                // === 合计行 ===
                contentStream.moveTo(margin, yPosition + 10);
                contentStream.lineTo(560, yPosition + 10);
                contentStream.stroke();

                contentStream.beginText();
                contentStream.setFont(boldFont, 9);
                contentStream.newLineAtOffset(margin, yPosition);
                contentStream.showText(safeText("合计", useChinese));
                contentStream.newLineAtOffset(120, 0);
                contentStream.showText(amount.isEmpty() ? "-" : amount + " CNY");
                contentStream.endText();

                // === 底部信息 ===
                yPosition = 80;
                contentStream.moveTo(margin, yPosition + 20);
                contentStream.lineTo(560, yPosition + 20);
                contentStream.stroke();

                contentStream.beginText();
                contentStream.setFont(regularFont, 9);
                contentStream.newLineAtOffset(margin, yPosition);
                contentStream.showText(safeText("制单人: " + creator, useChinese));
                contentStream.newLineAtOffset(200, 0);
                contentStream.showText(safeText(
                        "审核人: -", useChinese));
                contentStream.endText();

                // 脚注
                contentStream.beginText();
                contentStream.setFont(regularFont, 8);
                contentStream.newLineAtOffset(margin, 30);
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                contentStream.showText(safeText("由NexusArchive系统自动生成 - " + timestamp, useChinese));
                contentStream.endText();
            }

            document.save(targetPath.toFile());
            log.info("收款单PDF生成成功: {}", targetPath);
        }
    }

    /**
     * 生成凭证 PDF 文件 - 完整版
     * 包含：账簿、凭证字号、期间、制单日期、分录序号、科目编码、币种等
     */
    private void generateVoucherPdf(Path targetPath, ArcFileContent fileContent, JsonNode voucherData)
            throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            // 加载中文字体
            org.apache.pdfbox.pdmodel.font.PDFont chineseFont = loadChineseFont(document);
            boolean useChinese = chineseFont != null;
            org.apache.pdfbox.pdmodel.font.PDFont regularFont = useChinese ? chineseFont : PDType1Font.HELVETICA;
            org.apache.pdfbox.pdmodel.font.PDFont boldFont = useChinese ? chineseFont : PDType1Font.HELVETICA_BOLD;

            // 从 JSON 解析完整头部信息
            JsonNode header = voucherData.has("header") ? voucherData.get("header") : voucherData;

            // 账簿/组织名称
            String orgName = "";
            if (header.has("accbook") && header.get("accbook").has("pk_org")) {
                orgName = header.path("accbook").path("pk_org").path("name").asText("");
            }
            if (orgName.isEmpty() && header.has("accbook")) {
                orgName = header.path("accbook").path("name").asText("");
            }

            // 凭证字号 (如 "记 3号")
            String voucherStr = header.path("vouchertype").path("voucherstr").asText("");
            String displayName = header.path("displayname").asText(
                    fileContent.getErpVoucherNo() != null ? fileContent.getErpVoucherNo() : "");
            String fullVoucherNo = (voucherStr.isEmpty() ? "" : voucherStr + " ") + displayName + "号";

            // 期间
            String period = header.path("period").asText(
                    fileContent.getFiscalYear() != null ? fileContent.getFiscalYear() : "");

            // 制单日期
            String makeTime = header.path("maketime").asText("");

            // 附单据数
            String attachmentQty = header.path("attachmentQuantity").asText("--");

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                int margin = 50;
                float yPosition = 750;

                // 1. 标题
                contentStream.beginText();
                contentStream.setFont(boldFont, 16);
                contentStream.newLineAtOffset(180, yPosition);
                contentStream.showText(useChinese ? "会计凭证 - Accounting Voucher" : "ACCOUNTING VOUCHER");
                contentStream.endText();
                yPosition -= 30;

                // 2. 第一行头部: 账簿名称
                if (!orgName.isEmpty()) {
                    contentStream.beginText();
                    contentStream.setFont(regularFont, 10);
                    contentStream.newLineAtOffset(margin, yPosition);
                    contentStream.showText((useChinese ? "账簿: " : "Book: ") + safeText(orgName, useChinese));
                    contentStream.endText();
                    yPosition -= 18;
                }

                // 3. 第二行头部: 凭证号、制单日期、期间
                contentStream.beginText();
                contentStream.setFont(regularFont, 10);
                contentStream.newLineAtOffset(margin, yPosition);
                contentStream.showText((useChinese ? "凭证号: " : "Voucher: ") + safeText(fullVoucherNo, useChinese));
                contentStream.newLineAtOffset(150, 0);
                contentStream.showText((useChinese ? "制单日期: " : "Date: ") + makeTime);
                contentStream.newLineAtOffset(150, 0);
                contentStream.showText((useChinese ? "期间: " : "Period: ") + period);
                contentStream.endText();
                yPosition -= 18;

                // 4. 第三行头部: 附单据数
                contentStream.beginText();
                contentStream.setFont(regularFont, 10);
                contentStream.newLineAtOffset(margin, yPosition);
                contentStream.showText((useChinese ? "附单据数: " : "Attachments: ") + attachmentQty);
                contentStream.endText();
                yPosition -= 25;

                // 5. 分录表格表头
                float tableStartY = yPosition;

                contentStream.beginText();
                contentStream.setFont(boldFont, 9);
                contentStream.newLineAtOffset(margin, yPosition);
                contentStream.showText(useChinese ? "分录" : "No.");
                contentStream.newLineAtOffset(30, 0);
                contentStream.showText(useChinese ? "摘要" : "Description");
                contentStream.newLineAtOffset(120, 0);
                contentStream.showText(useChinese ? "科目" : "Subject");
                contentStream.newLineAtOffset(160, 0);
                contentStream.showText(useChinese ? "币种" : "Cur");
                contentStream.newLineAtOffset(40, 0);
                contentStream.showText(useChinese ? "借方" : "Debit");
                contentStream.newLineAtOffset(70, 0);
                contentStream.showText(useChinese ? "贷方" : "Credit");
                contentStream.endText();

                yPosition -= 18;
                contentStream.moveTo(margin, yPosition + 12);
                contentStream.lineTo(560, yPosition + 12);
                contentStream.stroke();

                // 4. 解析并渲染分录
                boolean hasDetails = renderVoucherEntries(contentStream, voucherData, yPosition, margin, regularFont,
                        useChinese);

                if (!hasDetails) {
                    contentStream.beginText();
                    contentStream.setFont(regularFont, 10);
                    contentStream.newLineAtOffset(margin, yPosition);
                    contentStream.showText(useChinese ? "暂无详细分录信息" : "No detailed entries available.");
                    contentStream.endText();
                }

                // 5. 底部信息
                yPosition = 100;
                contentStream.moveTo(margin, yPosition + 25);
                contentStream.lineTo(550, yPosition + 25);
                contentStream.stroke();

                contentStream.beginText();
                contentStream.setFont(regularFont, 10);
                contentStream.newLineAtOffset(margin, yPosition);
                String creator = fileContent.getCreator() != null ? fileContent.getCreator() : "System";
                String sourceSystem = fileContent.getSourceSystem() != null ? fileContent.getSourceSystem() : "Unknown";
                contentStream.showText((useChinese ? "制单人: " : "Creator: ") + safeText(creator, useChinese));
                contentStream.newLineAtOffset(250, 0);
                contentStream.showText((useChinese ? "来源系统: " : "Source: ") + sourceSystem);
                contentStream.endText();

                // 6. 脚注
                contentStream.beginText();
                contentStream.setFont(regularFont, 8);
                contentStream.newLineAtOffset(margin, 30);
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                contentStream
                        .showText((useChinese ? "由NexusArchive系统自动生成 - " : "Generated by NexusArchive - ") + timestamp);
                contentStream.endText();
            }

            document.save(targetPath.toFile());
        }
    }

    /**
     * 渲染凭证分录
     */
    private boolean renderVoucherEntries(PDPageContentStream contentStream, JsonNode voucherData,
            float startY, int margin,
            org.apache.pdfbox.pdmodel.font.PDFont regularFont,
            boolean useChinese) throws IOException {
        float yPosition = startY;
        boolean hasDetails = false;

        // 尝试多种可能的数据结构
        // 1. VoucherRecord 结构: { header: {...}, body: [...] }
        // 2. VoucherDetail 结构: { bodies: [...] }
        // 3. 直接数组
        JsonNode bodies = null;

        if (voucherData.has("body") && voucherData.get("body").isArray()) {
            bodies = voucherData.get("body");
            log.debug("Found 'body' array with {} entries", bodies.size());
        } else if (voucherData.has("bodies") && voucherData.get("bodies").isArray()) {
            bodies = voucherData.get("bodies");
            log.debug("Found 'bodies' array with {} entries", bodies.size());
        } else if (voucherData.isArray()) {
            bodies = voucherData;
            log.debug("Using root array with {} entries", bodies.size());
        }

        if (bodies != null && bodies.isArray() && bodies.size() > 0) {
            hasDetails = true;
            contentStream.setFont(regularFont, 9);

            double totalDebit = 0.0;
            double totalCredit = 0.0;

            for (JsonNode body : bodies) {
                // 分录序号
                int recordNumber = body.path("recordnumber").asInt(0);
                if (recordNumber == 0) {
                    recordNumber = body.path("recordNumber").asInt(0);
                }

                // 摘要 (多种可能的字段名)
                String desc = getTextValue(body, "description", "digest", "摘要", "desc");

                // 科目编码 + 科目名称
                String subjectCode = "";
                String subjectName = "";
                if (body.has("accsubject") && !body.get("accsubject").isNull()) {
                    JsonNode accSubjectNode = body.get("accsubject");
                    subjectCode = accSubjectNode.path("code").asText("");
                    subjectName = accSubjectNode.path("name").asText("");
                }
                // 如果没有找到，尝试其他字段
                if (subjectName.isEmpty()) {
                    subjectName = getTextValue(body, "accSubject", "subjectName", "科目名称", "subject");
                }
                // 组合科目显示 (如 "6401 主营业务成本")
                String subjectDisplay = subjectCode.isEmpty() ? subjectName : subjectCode + " " + subjectName;

                // 币种
                String currencyCode = "CNY";
                if (body.has("currency") && !body.get("currency").isNull()) {
                    JsonNode currencyNode = body.get("currency");
                    if (currencyNode.isObject()) {
                        currencyCode = currencyNode.path("code").asText("CNY");
                    } else {
                        currencyCode = currencyNode.asText("CNY");
                    }
                }

                // 借方金额 (支持下划线和驼峰命名)
                double debit = getAmountValue(body, "debit_original", "debitOriginal", "debitOrg", "debit_org",
                        "debit");
                // 贷方金额
                double credit = getAmountValue(body, "credit_original", "creditOriginal", "creditOrg", "credit_org",
                        "credit");

                // 辅助核算项
                String auxiliaryInfo = parseAuxiliaryItems(body);

                // 现金流量项目
                String cashFlowInfo = parseCashFlowItems(body);

                totalDebit += debit;
                totalCredit += credit;

                log.debug(
                        "Rendering entry: no={}, desc='{}', subject='{}', cur={}, debit={}, credit={}, aux='{}', cf='{}'",
                        recordNumber, desc, subjectDisplay, currencyCode, debit, credit, auxiliaryInfo, cashFlowInfo);

                // 主分录行
                contentStream.beginText();
                contentStream.newLineAtOffset(margin, yPosition);
                // 分录序号
                contentStream.showText(recordNumber > 0 ? String.valueOf(recordNumber) : "");
                contentStream.newLineAtOffset(30, 0);
                // 摘要
                contentStream.showText(safeText(truncateText(desc, 18), useChinese));
                contentStream.newLineAtOffset(120, 0);
                // 科目 (编码 + 名称)
                contentStream.showText(safeText(truncateText(subjectDisplay, 22), useChinese));
                contentStream.newLineAtOffset(160, 0);
                // 币种
                contentStream.showText(currencyCode);
                contentStream.newLineAtOffset(40, 0);
                // 借方
                if (debit != 0)
                    contentStream.showText(String.format("%.2f", debit));
                contentStream.newLineAtOffset(70, 0);
                // 贷方
                if (credit != 0)
                    contentStream.showText(String.format("%.2f", credit));
                contentStream.endText();

                yPosition -= 13;

                // 辅助核算附加行 (如果有)
                if (!auxiliaryInfo.isEmpty()) {
                    contentStream.beginText();
                    contentStream.setFont(regularFont, 8);
                    contentStream.newLineAtOffset(margin + 30, yPosition);
                    contentStream.showText(safeText("  [辅助] " + truncateText(auxiliaryInfo, 60), useChinese));
                    contentStream.endText();
                    yPosition -= 11;
                    contentStream.setFont(regularFont, 9);
                }

                // 现金流量附加行 (如果有)
                if (!cashFlowInfo.isEmpty()) {
                    contentStream.beginText();
                    contentStream.setFont(regularFont, 8);
                    contentStream.newLineAtOffset(margin + 30, yPosition);
                    contentStream.showText(safeText("  [现金流量] " + truncateText(cashFlowInfo, 55), useChinese));
                    contentStream.endText();
                    yPosition -= 11;
                    contentStream.setFont(regularFont, 9);
                }

                if (yPosition < 130) { // 防止覆盖底部信息
                    break;
                }
            }

            // 绘制合计行
            yPosition -= 5;
            contentStream.moveTo(margin, yPosition + 10);
            contentStream.lineTo(560, yPosition + 10);
            contentStream.stroke();

            contentStream.beginText();
            contentStream.setFont(regularFont, 9);
            contentStream.newLineAtOffset(margin + 30 + 120 + 160, yPosition);
            contentStream.showText(useChinese ? "合计" : "Total");
            contentStream.newLineAtOffset(40, 0);
            contentStream.showText(String.format("%.2f", totalDebit));
            contentStream.newLineAtOffset(70, 0);
            contentStream.showText(String.format("%.2f", totalCredit));
            contentStream.endText();

        } else {
            log.warn("No voucher entries found in JSON data. Keys: {}",
                    voucherData.fieldNames().hasNext() ? voucherData.fieldNames().next() : "empty");
        }

        return hasDetails;
    }

    /**
     * 解析辅助核算项
     * clientAuxiliary: [{dataType, docType, code, name, value}, ...]
     */
    private String parseAuxiliaryItems(JsonNode body) {
        StringBuilder sb = new StringBuilder();

        // 尝试 clientAuxiliary 数组
        JsonNode clientAux = body.path("clientAuxiliary");
        if (clientAux.isArray() && clientAux.size() > 0) {
            for (JsonNode item : clientAux) {
                String name = item.path("name").asText("");
                String value = item.path("value").asText("");
                if (!name.isEmpty() || !value.isEmpty()) {
                    if (sb.length() > 0)
                        sb.append("; ");
                    sb.append(name.isEmpty() ? value : name + ":" + value);
                }
            }
        }

        // 尝试 auxiliary Map
        JsonNode auxiliary = body.path("auxiliary");
        if (auxiliary.isObject() && auxiliary.size() > 0) {
            var fields = auxiliary.fields();
            while (fields.hasNext()) {
                var entry = fields.next();
                String key = entry.getKey();
                String value = entry.getValue().asText("");
                if (!value.isEmpty()) {
                    if (sb.length() > 0)
                        sb.append("; ");
                    sb.append(key).append(":").append(value);
                }
            }
        }

        return sb.toString();
    }

    /**
     * 解析现金流量项目
     * cashFlowItem: [{itemId, itemCode, itemName, negative, amountOriginal,
     * amountOrg, innerOrg}, ...]
     */
    private String parseCashFlowItems(JsonNode body) {
        StringBuilder sb = new StringBuilder();

        JsonNode cashFlowItems = body.path("cashFlowItem");
        if (cashFlowItems.isArray() && cashFlowItems.size() > 0) {
            for (JsonNode item : cashFlowItems) {
                String itemName = item.path("itemName").asText("");
                String itemCode = item.path("itemCode").asText("");
                double amount = item.path("amountOriginal").asDouble(0.0);
                if (amount == 0.0) {
                    amount = item.path("amountOrg").asDouble(0.0);
                }
                boolean negative = item.path("negative").asBoolean(false);

                if (!itemName.isEmpty() || !itemCode.isEmpty()) {
                    if (sb.length() > 0)
                        sb.append("; ");
                    String display = itemCode.isEmpty() ? itemName : itemCode + " " + itemName;
                    if (amount != 0) {
                        display += (negative ? " -" : " ") + String.format("%.2f", Math.abs(amount));
                    }
                    sb.append(display);
                }
            }
        }

        return sb.toString();
    }

    /**
     * 从 JSON 中获取文本值（尝试多个字段名）
     */
    private String getTextValue(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            if (node.has(fieldName) && !node.get(fieldName).isNull()) {
                return node.get(fieldName).asText("");
            }
        }
        return "";
    }

    /**
     * 从 JSON 中获取金额值（尝试多个字段名）
     */
    private double getAmountValue(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            if (node.has(fieldName) && !node.get(fieldName).isNull()) {
                return node.get(fieldName).asDouble(0.0);
            }
        }
        return 0.0;
    }

    /**
     * 加载中文字体
     */
    private org.apache.pdfbox.pdmodel.font.PDFont loadChineseFont(PDDocument document) {
        String[] fontPaths = {
                // macOS
                "/System/Library/Fonts/STHeiti Light.ttc",
                "/System/Library/Fonts/STHeiti Medium.ttc",
                "/Library/Fonts/Arial Unicode.ttf",
                "/System/Library/Fonts/PingFang.ttc",
                // Linux
                "/usr/share/fonts/truetype/noto/NotoSansCJK-Regular.ttc",
                "/usr/share/fonts/noto-cjk/NotoSansSC-Regular.otf",
                "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc",
                "/usr/share/fonts/truetype/wqy/wqy-zenhei.ttc",
                // Windows
                "C:/Windows/Fonts/msyh.ttc",
                "C:/Windows/Fonts/simsun.ttc",
                "C:/Windows/Fonts/simhei.ttf"
        };

        for (String fontPath : fontPaths) {
            try {
                File fontFile = new File(fontPath);
                if (fontFile.exists()) {
                    log.debug("Loading Chinese font from: {}", fontPath);
                    return PDType0Font.load(document, fontFile);
                }
            } catch (Exception e) {
                log.trace("Failed to load font from: {}", fontPath);
            }
        }
        return null;
    }

    /**
     * 计算 SM3 哈希值
     */
    private String calculateSM3Hash(byte[] data) {
        SM3Digest digest = new SM3Digest();
        digest.update(data, 0, data.length);
        byte[] hash = new byte[digest.getDigestSize()];
        digest.doFinal(hash, 0);

        StringBuilder sb = new StringBuilder();
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * 截断文本
     */
    private String truncateText(String text, int maxLen) {
        if (text == null)
            return "";
        if (text.length() <= maxLen)
            return text;
        return text.substring(0, maxLen) + "...";
    }

    /**
     * 安全文本处理（中文支持）
     */
    private String safeText(String text, boolean supportChinese) {
        if (text == null)
            return "";
        if (supportChinese)
            return text;

        // 回退到 ASCII 模式
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            sb.append(c > 127 ? "?" : c);
        }
        return sb.toString();
    }
}
