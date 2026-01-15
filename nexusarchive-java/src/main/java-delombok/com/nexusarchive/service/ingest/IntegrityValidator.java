// Input: Spring Framework, Lombok
// Output: IntegrityValidator 类
// Pos: 服务层 - 完整性验证器

package com.nexusarchive.service.ingest;

import com.nexusarchive.common.constant.ErrorCode;
import com.nexusarchive.common.exception.BusinessException;
import com.nexusarchive.dto.sip.AccountingSipDto;
import com.nexusarchive.dto.sip.VoucherHeadDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * 完整性验证器
 * <p>
 * 验证附件数量一致性、分录金额平衡性
 * </p>
 */
@Slf4j
@Component
public class IntegrityValidator implements IngestValidator {

    @Override
    public void validate(AccountingSipDto sipDto) {
        VoucherHeadDto header = sipDto.getHeader();
        List<com.nexusarchive.dto.sip.VoucherEntryDto> entries = sipDto.getEntries();
        List<com.nexusarchive.dto.sip.AttachmentDto> attachments = sipDto.getAttachments();

        // Rule 1: Integrity - attachment_count check
        int actualAttachmentCount = (attachments == null) ? 0 : attachments.size();
        if (!header.getAttachmentCount().equals(actualAttachmentCount)) {
            throw new BusinessException(
                    Integer.parseInt(ErrorCode.EAA_1001_COUNT_MISMATCH.replace("EAA_", "")),
                    String.format(ErrorCode.EAA_1001_MSG, header.getAttachmentCount(), actualAttachmentCount));
        }

        // Rule 2: Balance - entry_amount sum check
        BigDecimal totalEntryAmount = entries.stream()
                .map(com.nexusarchive.dto.sip.VoucherEntryDto::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 允许 0.00 的误差
        if (totalEntryAmount.compareTo(header.getTotalAmount()) != 0) {
            throw new BusinessException(
                    Integer.parseInt(ErrorCode.EAA_1002_BALANCE_ERROR.replace("EAA_", "")),
                    String.format(ErrorCode.EAA_1002_MSG, header.getTotalAmount(), totalEntryAmount,
                            header.getTotalAmount().subtract(totalEntryAmount)));
        }

        log.debug("完整性验证通过: attachmentCount={}, totalAmount={}",
                actualAttachmentCount, totalEntryAmount);
    }

    @Override
    public String getName() {
        return "完整性验证器";
    }

    @Override
    public int getPriority() {
        return 1;
    }
}
