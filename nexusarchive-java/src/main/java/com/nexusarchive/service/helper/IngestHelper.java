// Input: Hutool JSON/Base64, Local DTOs, ErpAdapterFactory
// Output: IngestHelper (采集入库辅助类)
// Pos: Service Helper Layer
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.helper;

import java.util.UUID;
import cn.hutool.core.codec.Base64;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.nexusarchive.common.constant.ErrorCode;
import com.nexusarchive.common.exception.BusinessException;
import com.nexusarchive.dto.sip.*;
import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.entity.ErpConfig;
import com.nexusarchive.integration.erp.adapter.ErpAdapter;
import com.nexusarchive.integration.erp.adapter.ErpAdapterFactory;
import com.nexusarchive.integration.erp.dto.FeedbackResult;
import com.nexusarchive.util.FileMagicValidator;
import com.nexusarchive.util.PathSecurityUtils;
import com.nexusarchive.util.SM4Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class IngestHelper {

    private final PathSecurityUtils pathSecurityUtils;
    private final ErpAdapterFactory erpAdapterFactory;

    public void prepareTempFiles(AccountingSipDto sipDto, String tempPath, Map<String, byte[]> fileStreams) throws IOException {
        if (sipDto.getAttachments() == null) return;
        Files.createDirectories(Paths.get(tempPath));
        FileMagicValidator validator = new FileMagicValidator();

        for (AttachmentDto attachment : sipDto.getAttachments()) {
            String safeName = pathSecurityUtils.getSafeFileName(attachment.getFileName());
            if (fileStreams.containsKey(safeName)) throw new BusinessException(400, "重复附件文件名: " + safeName);

            byte[] decoded = Base64.decode(attachment.getBase64Content());
            FileMagicValidator.ValidationResult vResult = validator.validate(decoded, safeName);
            if (!vResult.isValid()) throw new BusinessException(400, "文件类型验证失败: " + vResult.getMessage());

            fileStreams.put(safeName, decoded);
            attachment.setFileName(safeName);
            Files.write(Paths.get(tempPath, safeName), decoded);
        }
    }

    public void validateBusinessRules(AccountingSipDto sipDto) {
        VoucherHeadDto header = sipDto.getHeader();
        int actualCount = (sipDto.getAttachments() == null) ? 0 : sipDto.getAttachments().size();
        if (!header.getAttachmentCount().equals(actualCount)) {
            throw new BusinessException(Integer.parseInt(ErrorCode.EAA_1001_COUNT_MISMATCH.replace("EAA_", "")),
                    String.format(ErrorCode.EAA_1001_MSG, header.getAttachmentCount(), actualCount));
        }

        BigDecimal totalAmount = sipDto.getEntries().stream().map(VoucherEntryDto::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalAmount.compareTo(header.getTotalAmount()) != 0) {
            throw new BusinessException(Integer.parseInt(ErrorCode.EAA_1002_BALANCE_ERROR.replace("EAA_", "")),
                    String.format(ErrorCode.EAA_1002_MSG, header.getTotalAmount(), totalAmount, header.getTotalAmount().subtract(totalAmount)));
        }
    }

    public String generateArchivalCode(ArcFileContent file) {
        String fonds = file.getFondsCode();
        if (fonds == null || fonds.isBlank()) throw new BusinessException(400, "全宗号未配置");
        String year = file.getFiscalYear() != null ? file.getFiscalYear() : String.valueOf(LocalDate.now().getYear());
        String category = file.getVoucherType() != null ? file.getVoucherType() : "AC04";
        // 使用 UUID 确保唯一性，取前4位作为序列号
        String sequence = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return String.format("%s-%s-30Y-FIN-%s-V%s", fonds, year, category, sequence);
    }

    public AccountingSipDto buildSimpleSip(String code, String itemId, String fileName, ArcFileContent file) {
        AccountingSipDto sip = new AccountingSipDto();
        sip.setRequestId(code);
        sip.setSourceSystem(file.getSourceSystem() != null ? file.getSourceSystem() : "Pool Archive");

        VoucherHeadDto h = new VoucherHeadDto();
        h.setFondsCode(file.getFondsCode());
        h.setAccountPeriod((file.getFiscalYear() != null ? file.getFiscalYear() : String.valueOf(LocalDate.now().getYear())) + "-" + LocalDate.now().format(DateTimeFormatter.ofPattern("MM")));
        h.setVoucherType(com.nexusarchive.common.enums.VoucherType.PAYMENT);
        h.setVoucherNumber("V-" + itemId.substring(0, Math.min(6, itemId.length())));
        h.setVoucherDate(LocalDate.now());
        h.setTotalAmount(BigDecimal.ZERO);
        h.setCurrencyCode("CNY");
        h.setIssuer(file.getCreator() != null ? file.getCreator() : "System");
        h.setAttachmentCount(1);
        sip.setHeader(h);

        AttachmentDto a = new AttachmentDto();
        a.setFileName(fileName);
        a.setFileType(file.getFileType());
        a.setFileSize(file.getFileSize());
        a.setFileHash(file.getFileHash());
        a.setHashAlgorithm(file.getHashAlgorithm());
        sip.setAttachments(List.of(a));
        return sip;
    }

    public FeedbackResult triggerErpFeedback(ArcFileContent file, String code, ErpConfig config) {
        if (file.getSourceSystem() == null || file.getErpVoucherNo() == null || config == null) return null;
        
        ErpAdapter adapter = erpAdapterFactory.getAdapter(config.getErpType());
        if (adapter == null) return FeedbackResult.failure(file.getErpVoucherNo(), code, config.getErpType(), "适配器未找到");

        com.nexusarchive.integration.erp.dto.ErpConfig dto = new com.nexusarchive.integration.erp.dto.ErpConfig();
        dto.setId(String.valueOf(config.getId()));
        dto.setName(config.getName());
        dto.setAdapterType(config.getErpType());

        if (config.getConfigJson() != null) {
            JSONObject json = JSONUtil.parseObj(config.getConfigJson());
            dto.setBaseUrl(json.getStr("baseUrl"));
            String key = json.getStr("appKey", json.getStr("clientId"));
            dto.setAppKey(key);
            String secret = json.getStr("appSecret", json.getStr("clientSecret"));
            dto.setAppSecret(SM4Utils.decryptStrict(secret));
            dto.setAccbookCode(json.getStr("accbookCode"));
            dto.setExtraConfig(config.getConfigJson());
        }
        return adapter.feedbackArchivalStatus(dto, file.getErpVoucherNo(), code, "ARCHIVED");
    }
}
