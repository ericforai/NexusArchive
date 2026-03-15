// Input: Spring Framework, Lombok
// Output: VoucherFileManager 类
// Pos: 服务层 - 原始凭证文件管理器

package com.nexusarchive.service.voucher;

import com.nexusarchive.common.exception.BusinessException;
import com.nexusarchive.entity.OriginalVoucher;
import com.nexusarchive.entity.OriginalVoucherFile;
import com.nexusarchive.mapper.OriginalVoucherFileMapper;
import com.nexusarchive.mapper.OriginalVoucherMapper;
import com.nexusarchive.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import com.nexusarchive.common.constants.HttpConstants;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 原始凭证文件管理器
 * <p>
 * 负责原始凭证文件的添加、下载、管理
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VoucherFileManager {

    private final OriginalVoucherMapper voucherMapper;
    private final OriginalVoucherFileMapper fileMapper;
    private final FileStorageService fileStorageService;
    private final com.nexusarchive.service.parser.PdfInvoiceParser pdfInvoiceParser;

    /**
     * 添加文件到原始凭证
     */
    @Transactional
    public OriginalVoucherFile addFile(String voucherId, MultipartFile file, String fileRole, String userId) {
        // 校验凭证存在
        OriginalVoucher voucher = voucherMapper.selectById(voucherId);
        if (voucher == null || voucher.getDeleted() == 1) {
            throw new BusinessException("原始凭证不存在: " + voucherId);
        }

        if (file.isEmpty()) {
            throw new BusinessException("上传文件为空");
        }

        try {
            String originalFilename = file.getOriginalFilename();
            String fileId = UUID.randomUUID().toString();
            String extension = "";

            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String fileType = extension.replace(".", "").toUpperCase();

            // 存储文件 (使用 FileStorageService 标准化流程)
            String relativePath = "original-vouchers/" + voucherId + "/" + fileId + extension;
            fileStorageService.saveFile(file.getInputStream(), relativePath);

            // 计算哈希
            byte[] content = file.getBytes();
            String fileHash = calculateHash(content);

            // 设置序号
            List<OriginalVoucherFile> existingFiles = fileMapper.findByVoucherId(voucherId);
            int sequenceNo = existingFiles.size() + 1;

            // 构建文件记录
            OriginalVoucherFile voucherFile = OriginalVoucherFile.builder()
                    .id(fileId)
                    .voucherId(voucherId)
                    .fileName(originalFilename)
                    .fileType(fileType)
                    .fileSize(file.getSize())
                    .storagePath(relativePath)
                    .fileHash(fileHash)
                    .hashAlgorithm("SM3")
                    .fileRole(fileRole != null ? fileRole : "PRIMARY")
                    .sequenceNo(sequenceNo)
                    .createdBy(userId)
                    .createdTime(LocalDateTime.now())
                    .build();

            fileMapper.insert(voucherFile);

            // 解析发票逻辑 (自动识别金额)
            if ("PDF".equals(fileType) && ("PRIMARY".equals(voucherFile.getFileRole()) || "ORIGINAL".equals(voucherFile.getFileRole()))) {
                try {
                    java.util.Map<String, Object> parseResult = pdfInvoiceParser.parse(
                            fileStorageService.resolvePath(relativePath).toFile());
                    if (parseResult.containsKey("total_amount_value")) {
                        String amountStr = (String) parseResult.get("total_amount_value");
                        log.info("OCR Identified amount string: {}", amountStr);
                        try {
                            java.math.BigDecimal amount = new java.math.BigDecimal(amountStr);
                            // 修正：即使凭证为 0 也更新，除非已经有人工输入了大于 0 的值
                            if (voucher.getAmount() == null || voucher.getAmount().compareTo(java.math.BigDecimal.ZERO) == 0) {
                                voucher.setAmount(amount);
                                voucherMapper.updateById(voucher);
                                log.info("Automatically filled amount {} for voucher {}", amount, voucherId);
                            }
                        } catch (Exception e) {
                            log.warn("Failed to convert amount: {}", amountStr);
                        }
                    }
                } catch (Exception e) {
                    log.error("Parsing failed", e);
                }
            }

            log.info("Added file {} to original voucher: {}", originalFilename, voucherId);
            return voucherFile;

        } catch (java.io.IOException e) {
            log.error("文件上传失败", e);
            throw new BusinessException("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 计算文件哈希 (SM3 或 SHA-256)
     */
    private String calculateHash(byte[] content) {
        try {
            java.security.MessageDigest md;
            try {
                md = java.security.MessageDigest.getInstance("SM3");
            } catch (Exception e) {
                md = java.security.MessageDigest.getInstance("SHA-256");
            }
            byte[] digest = md.digest(content);
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("Hash calculation failed", e);
            return UUID.randomUUID().toString();
        }
    }

    /**
     * 下载原始凭证文件内容
     */
    public org.springframework.http.ResponseEntity<org.springframework.core.io.Resource> downloadFile(String fileId) {
        OriginalVoucherFile fileInfo = fileMapper.selectById(fileId);
        if (fileInfo == null || !StringUtils.hasText(fileInfo.getStoragePath())) {
            throw new BusinessException("文件不存在: " + fileId);
        }

        java.nio.file.Path filePath = fileStorageService.resolvePath(fileInfo.getStoragePath());
        if (!fileStorageService.exists(fileInfo.getStoragePath())) {
            throw new BusinessException("物理文件不存在: " + fileInfo.getStoragePath());
        }

        org.springframework.core.io.Resource resource = new org.springframework.core.io.FileSystemResource(filePath.toFile());
        String contentType = determineContentType(fileInfo.getFileType(), fileInfo.getFileName());

        // 使用 RFC 5987 标准编码处理中文文件名
        String encodedFileName = java.net.URLEncoder.encode(fileInfo.getFileName(), java.nio.charset.StandardCharsets.UTF_8)
                .replace("+", "%20");

        return org.springframework.http.ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "inline; filename*=UTF-8''" + encodedFileName)
                .contentType(org.springframework.http.MediaType.parseMediaType(contentType))
                .body(resource);
    }

    private String determineContentType(String fileType, String fileName) {
        if (StringUtils.hasText(fileType)) {
            switch (fileType.toLowerCase()) {
                case "ofd": return HttpConstants.APPLICATION_OFD;
                case "pdf": return HttpConstants.APPLICATION_PDF;
                case "jpg":
                case "jpeg": return "image/jpeg";
                case "png": return "image/png";
                case "xml": return "application/xml";
            }
        }
        if (fileName != null) {
            String lowerName = fileName.toLowerCase();
            if (lowerName.endsWith(".ofd")) return HttpConstants.APPLICATION_OFD;
            if (lowerName.endsWith(".pdf")) return HttpConstants.APPLICATION_PDF;
            if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")) return "image/jpeg";
            if (lowerName.endsWith(".png")) return "image/png";
        }
        return "application/octet-stream";
    }
}
