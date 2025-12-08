package com.nexusarchive.service.strategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusarchive.dto.parser.ParsedInvoice;
import com.nexusarchive.dto.sip.VoucherHeadDto;
import com.nexusarchive.entity.Archive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * 规则B: 金额+日期匹配策略
 * 逻辑: 金额相等 且 日期相差 <= 3天
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AmountDateMatchStrategy implements MatchingStrategy {

    private final ObjectMapper objectMapper;
    private static final int DATE_THRESHOLD_DAYS = 3;
    private static final int CONFIDENCE_SCORE = 80;

    @Override
    public int match(Archive voucher, Archive otherFile) {
        try {
            if (voucher.getStandardMetadata() == null || otherFile.getStandardMetadata() == null) {
                return 0;
            }

            VoucherHeadDto voucherHead = objectMapper.readValue(voucher.getStandardMetadata(), VoucherHeadDto.class);
            ParsedInvoice invoice = objectMapper.readValue(otherFile.getStandardMetadata(), ParsedInvoice.class);

            BigDecimal voucherAmount = voucherHead.getTotalAmount();
            BigDecimal invoiceAmount = invoice.getTotalAmount();

            if (voucherAmount != null && invoiceAmount != null && voucherAmount.compareTo(invoiceAmount) == 0) {
                LocalDate voucherDate = voucherHead.getVoucherDate();
                LocalDate invoiceDate = invoice.getIssueDate();

                if (voucherDate != null && invoiceDate != null) {
                    long daysDiff = Math.abs(ChronoUnit.DAYS.between(voucherDate, invoiceDate));
                    if (daysDiff <= DATE_THRESHOLD_DAYS) {
                        return CONFIDENCE_SCORE;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Error parsing metadata for matching: {}", e.getMessage());
        }
        return 0;
    }

    @Override
    public String getName() {
        return "AmountDateMatch";
    }
}
