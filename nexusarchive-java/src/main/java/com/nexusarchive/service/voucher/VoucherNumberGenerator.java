// Input: MyBatis-Plus, Java 标准库
// Output: VoucherNumberGenerator 类
// Pos: Service Layer

package com.nexusarchive.service.voucher;

import com.nexusarchive.mapper.OriginalVoucherMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.time.Year;

/**
 * 原始凭证编号生成器
 *
 * 负责生成符合格式的原始凭证编号
 */
@Service
@RequiredArgsConstructor
public class VoucherNumberGenerator {

    private final OriginalVoucherMapper voucherMapper;

    /**
     * 生成原始凭证编号
     * 格式: OV-{年度}-{类型简码}-{6位序号}
     *
     * @param fondsCode 全宗代码
     * @param fiscalYear 会计年度
     * @param category 凭证类别
     * @return 凭证编号
     */
    public synchronized String generate(String fondsCode, String fiscalYear, String category) {
        if (fiscalYear == null || fiscalYear.isEmpty()) {
            fiscalYear = String.valueOf(Year.now().getValue());
        }

        // 获取并更新序号
        Long seq = voucherMapper.getNextSequence(fondsCode, fiscalYear, category);
        voucherMapper.updateSequence(
                UUID.randomUUID().toString(),
                fondsCode, fiscalYear, category, seq);

        // 类型简码映射
        String typeCode = getCategoryShortCode(category);

        return String.format("OV-%s-%s-%06d", fiscalYear, typeCode, seq);
    }

    /**
     * 获取类别简码
     */
    public String getCategoryShortCode(String category) {
        return switch (category) {
            case "INVOICE" -> "INV";
            case "BANK" -> "BNK";
            case "DOCUMENT" -> "DOC";
            case "CONTRACT" -> "CON";
            default -> "OTH";
        };
    }
}
