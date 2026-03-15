// Input: OriginalVoucher Mappers and Entities
// Output: OriginalVoucherHelper (原始凭证辅助类)
// Pos: Service Helper Layer
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.helper;

import com.nexusarchive.entity.OriginalVoucher;
import com.nexusarchive.entity.OriginalVoucherType;
import com.nexusarchive.mapper.OriginalVoucherMapper;
import com.nexusarchive.mapper.OriginalVoucherTypeMapper;
import com.nexusarchive.common.constants.HttpConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Year;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OriginalVoucherHelper {

    private final OriginalVoucherMapper voucherMapper;
    private final OriginalVoucherTypeMapper typeMapper;

    public synchronized String generateVoucherNo(String fondsCode, String fiscalYear, String category) {
        if (!StringUtils.hasText(fiscalYear)) {
            fiscalYear = String.valueOf(Year.now().getValue());
        }

        Long seq = voucherMapper.getNextSequence(fondsCode, fiscalYear, category);
        voucherMapper.updateSequence(UUID.randomUUID().toString(), fondsCode, fiscalYear, category, seq);

        String typeCode = getCategoryShortCode(category);
        return String.format("OV-%s-%s-%06d", fiscalYear, typeCode, seq);
    }

    public String getCategoryShortCode(String category) {
        return switch (category) {
            case "INVOICE" -> "INV";
            case "BANK" -> "BNK";
            case "DOCUMENT" -> "DOC";
            case "CONTRACT" -> "CON";
            default -> "OTH";
        };
    }

    public List<String> getTypeAliases(String typeCode) {
        return switch (typeCode) {
            case "BANK_RECEIPT" -> List.of("BANK_RECEIPT", "BANK_SLIP");
            case "INV_VAT_E" -> List.of("INV_VAT_E", "VAT_INVOICE");
            default -> List.of(typeCode);
        };
    }

    public void validateVoucherType(String category, String type) {
        OriginalVoucherType typeInfo = typeMapper.findByTypeCode(type);
        if (typeInfo == null || !typeInfo.getEnabled()) {
            throw new com.nexusarchive.common.exception.BusinessException("无效的凭证类型: " + type);
        }
        if (!typeInfo.getCategoryCode().equals(category)) {
            throw new com.nexusarchive.common.exception.BusinessException("凭证类型与类别不匹配: " + category + " / " + type);
        }
    }

    public String determineContentType(String fileType, String fileName) {
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

    public String calculateHash(byte[] content) {
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
            return UUID.randomUUID().toString();
        }
    }
}
