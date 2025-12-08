package com.nexusarchive.service.strategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusarchive.dto.parser.ParsedInvoice;
import com.nexusarchive.dto.sip.VoucherHeadDto;
import com.nexusarchive.entity.Archive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 规则A: 精确匹配策略
 * 逻辑: 凭证摘要(Remark) 包含 发票号码(InvoiceNumber)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExactMatchStrategy implements MatchingStrategy {

    private final ObjectMapper objectMapper;

    @Override
    public int match(Archive voucher, Archive otherFile) {
        try {
            if (voucher.getStandardMetadata() == null || otherFile.getStandardMetadata() == null) {
                return 0;
            }

            VoucherHeadDto voucherHead = objectMapper.readValue(voucher.getStandardMetadata(), VoucherHeadDto.class);
            ParsedInvoice invoice = objectMapper.readValue(otherFile.getStandardMetadata(), ParsedInvoice.class);

            if (voucherHead.getRemark() != null && invoice.getInvoiceNumber() != null) {
                if (voucherHead.getRemark().contains(invoice.getInvoiceNumber())) {
                    return 100;
                }
            }
        } catch (Exception e) {
            log.warn("Error parsing metadata for matching: {}", e.getMessage());
        }
        return 0;
    }

    @Override
    public String getName() {
        return "ExactMatch(Summary-InvoiceNo)";
    }
}
