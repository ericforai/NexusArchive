// Input: Jackson、Lombok、Spring Framework、Java 标准库
// Output: VoucherPdfGeneratorService 类（PDF 生成协调层）
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.mapper.ArcFileContentMapper;
import com.nexusarchive.service.pdf.CollectionPdfGenerator;
import com.nexusarchive.service.pdf.PaymentPdfGenerator;
import com.nexusarchive.service.pdf.PdfUtils;
import com.nexusarchive.service.pdf.VoucherPdfGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.nexusarchive.common.constants.HttpConstants;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 凭证 PDF 生成服务（协调层）
 * <p>
 * 负责将同步的 YonSuite 凭证数据生成为可预览的 PDF 文件。
 * 具体生成逻辑已委托给专用生成器：
 * <ul>
 * <li>{@link com.nexusarchive.service.pdf.PaymentPdfGenerator} - 付款单</li>
 * <li>{@link com.nexusarchive.service.pdf.CollectionPdfGenerator} - 收款单</li>
 * <li>{@link com.nexusarchive.service.pdf.VoucherPdfGenerator} - 会计凭证</li>
 * </ul>
 * </p>
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

            // 3. 生成 PDF 文件名
            String docNo = fileContent.getErpVoucherNo() != null ? fileContent.getErpVoucherNo()
                    : (fileContent.getBusinessDocNo() != null ? fileContent.getBusinessDocNo() : fileId);
            String suffix = getSuffixForVoucherType(fileContent.getVoucherType());
            String pdfFileName = docNo + suffix;
            Path pdfPath = storageDir.resolve(pdfFileName);

            // 4. 生成 PDF 文件
            generatePdfByType(pdfPath, fileContent, voucherData);

            // 5. 计算文件哈希和大小
            byte[] pdfBytes = Files.readAllBytes(pdfPath);
            String fileHash = PdfUtils.calculateSM3Hash(pdfBytes);
            long fileSize = pdfBytes.length;

            // 6. 更新 arc_file_content 记录
            fileContent.setFileName(pdfFileName);
            fileContent.setFileType(HttpConstants.APPLICATION_PDF);
            fileContent.setFileSize(fileSize);
            fileContent.setFileHash(fileHash);
            fileContent.setHashAlgorithm("SM3");
            fileContent.setStoragePath(pdfPath.toString());
            if (fileContent.getOriginalHash() == null || fileContent.getOriginalHash().isEmpty()) {
                fileContent.setOriginalHash(fileHash);
            }
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
     * 根据单据类型获取文件名后缀
     */
    private String getSuffixForVoucherType(String voucherType) {
        if ("PAYMENT".equals(voucherType)) {
            return "_Payment.pdf";
        } else if ("COLLECTION_BILL".equals(voucherType)) {
            return "_Collection.pdf";
        } else {
            return "_Voucher.pdf";
        }
    }

    /**
     * 根据类型生成 PDF
     */
    private void generatePdfByType(Path targetPath, ArcFileContent fileContent, JsonNode voucherData)
            throws Exception {
        String voucherType = fileContent.getVoucherType();

        try (PDDocument document = new PDDocument()) {
            if ("COLLECTION_BILL".equals(voucherType)) {
                // 收款单专用生成器
                log.debug("生成收款单 PDF: target={}", targetPath);
                new CollectionPdfGenerator().generate(document, fileContent, voucherData, targetPath);
            } else if ("PAYMENT".equals(voucherType)) {
                // 付款单专用生成器
                log.debug("生成付款单 PDF: target={}", targetPath);
                new PaymentPdfGenerator().generate(document, fileContent, voucherData, targetPath);
            } else {
                // 默认会计凭证生成器
                new VoucherPdfGenerator().generate(document, fileContent, voucherData, targetPath);
            }
            document.save(targetPath.toFile());
        } catch (Exception e) {
            log.error("PDF生成失败: type={}, target={}", voucherType, targetPath, e);
            throw e;
        }
    }
}
