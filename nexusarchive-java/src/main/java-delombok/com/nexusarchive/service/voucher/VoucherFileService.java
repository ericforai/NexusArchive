// Input: Spring Framework, MyBatis-Plus, Java 标准库
// Output: VoucherFileService 类
// Pos: Service Layer

package com.nexusarchive.service.voucher;

import com.nexusarchive.common.exception.BusinessException;
import com.nexusarchive.entity.OriginalVoucher;
import com.nexusarchive.entity.OriginalVoucherFile;
import com.nexusarchive.mapper.OriginalVoucherFileMapper;
import com.nexusarchive.mapper.OriginalVoucherMapper;
import com.nexusarchive.service.FileStorageService;
import com.nexusarchive.service.parser.PdfInvoiceParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import java.security.MessageDigest;

/**
 * 原始凭证文件服务
 *
 * 负责凭证文件的添加和管理
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class VoucherFileService {

    private final OriginalVoucherFileMapper fileMapper;
    private final OriginalVoucherMapper voucherMapper;
    private final FileStorageService fileStorageService;
    private final PdfInvoiceParser pdfInvoiceParser;

    /**
     * 添加文件到原始凭证
     *
     * @param voucherId 凭证ID
     * @param file 上传的文件
     * @param fileRole 文件角色 (PRIMARY/ORIGINAL/ATTACHMENT)
     * @param userId 操作人ID
     * @return 创建的文件记录
     */
    @Transactional
    public OriginalVoucherFile addFile(String voucherId, MultipartFile file, String fileRole, String userId) {
        // 校验凭证存在
        OriginalVoucher voucher = voucherMapper.selectById(voucherId);
        if (voucher == null) {
            throw new BusinessException("原始凭证不存在: " + voucherId);
        }

        if (file.isEmpty()) {
            throw new BusinessException("上传文件为空");
        }

        try {
            String originalFilename = file.getOriginalFilename();
            String fileId = UUID.randomUUID().toString();
            String extension = extractExtension(originalFilename);
            String fileType = extension.replace(".", "").toUpperCase();

            // 存储文件
            String relativePath = "original-vouchers/" + voucherId + "/" + fileId + extension;
            fileStorageService.saveFile(file.getInputStream(), relativePath);

            // 计算哈希
            byte[] content = file.getBytes();
            String fileHash = calculateHash(content);

            // 设置序号
            List<OriginalVoucherFile> existingFiles = getFiles(voucherId);
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

            // 解析发票识别金额
            processPdfAmount(voucherId, fileType, fileRole, voucherFile, relativePath);

            log.info("Added file {} to original voucher: {}", originalFilename, voucherId);
            return voucherFile;

        } catch (IOException e) {
            log.error("文件上传失败", e);
            throw new BusinessException("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 获取凭证的文件列表
     */
    public List<OriginalVoucherFile> getFiles(String voucherId) {
        return fileMapper.findByVoucherId(voucherId);
    }

    /**
     * 根据ID获取文件
     */
    public OriginalVoucherFile getFileById(String fileId) {
        return fileMapper.selectById(fileId);
    }

    /**
     * 提取文件扩展名
     */
    private String extractExtension(String filename) {
        if (filename != null && filename.contains(".")) {
            return filename.substring(filename.lastIndexOf("."));
        }
        return "";
    }

    /**
     * 计算文件哈希 (SM3 或 SHA-256)
     */
    private String calculateHash(byte[] content) {
        try {
            MessageDigest md;
            try {
                md = MessageDigest.getInstance("SM3");
            } catch (Exception e) {
                md = MessageDigest.getInstance("SHA-256");
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
     * 处理 PDF 发票金额识别
     */
    private void processPdfAmount(String voucherId, String fileType, String fileRole,
                                  OriginalVoucherFile voucherFile, String relativePath) {
        if ("PDF".equals(fileType) && ("PRIMARY".equals(voucherFile.getFileRole()) || "ORIGINAL".equals(voucherFile.getFileRole()))) {
            try {
                Path filePath = fileStorageService.resolvePath(relativePath);
                java.util.Map<String, Object> parseResult = pdfInvoiceParser.parse(filePath.toFile());
                if (parseResult.containsKey("total_amount_value")) {
                    String amountStr = (String) parseResult.get("total_amount_value");
                    log.info("OCR Identified amount string: {}", amountStr);
                    try {
                        java.math.BigDecimal amount = new java.math.BigDecimal(amountStr);
                        OriginalVoucher voucher = voucherMapper.selectById(voucherId);
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
    }
}
