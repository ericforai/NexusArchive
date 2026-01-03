// Input: MatchingStrategy, Archive Entity
// Output: InvoiceNumberMatchStrategy 类
// Pos: 关联策略实现
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.strategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusarchive.dto.parser.ParsedInvoice;
import com.nexusarchive.entity.Archive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 发票号匹配策略
 * 
 * 通过发票号精确匹配记账凭证和发票
 * 置信度：100（完全匹配）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InvoiceNumberMatchStrategy implements MatchingStrategy {
    
    private final ObjectMapper objectMapper;
    
    @Override
    public String getName() {
        return "InvoiceNumberMatch";
    }
    
    @Override
    public int match(Archive voucher, Archive candidate) {
        try {
            // 1. 从凭证中提取发票号列表
            String voucherInvoiceNos = extractInvoiceNumbersFromVoucher(voucher);
            if (voucherInvoiceNos == null || voucherInvoiceNos.isEmpty()) {
                return 0;
            }
            
            // 2. 从候选文件中提取发票号
            String candidateInvoiceNo = extractInvoiceNumberFromFile(candidate);
            if (candidateInvoiceNo == null || candidateInvoiceNo.isEmpty()) {
                return 0;
            }
            
            // 3. 精确匹配
            if (voucherInvoiceNos.contains(candidateInvoiceNo)) {
                log.debug("发票号匹配成功: voucher={}, candidate={}, invoiceNo={}", 
                    voucher.getArchiveCode(), candidate.getArchiveCode(), candidateInvoiceNo);
                return 100; // 完全匹配
            }
            
        } catch (Exception e) {
            log.warn("发票号匹配策略执行失败: voucher={}, candidate={}", 
                voucher.getArchiveCode(), candidate.getArchiveCode(), e);
        }
        
        return 0;
    }
    
    /**
     * 从凭证中提取发票号列表
     */
    private String extractInvoiceNumbersFromVoucher(Archive voucher) {
        // TODO: 从 voucher 的 metadata_ext 或 standardMetadata 中提取发票号列表
        // 凭证可能关联多张发票，发票号可能是逗号分隔的字符串或数组
        // 这里简化处理，实际需要根据数据结构解析
        return null;
    }
    
    /**
     * 从文件中提取发票号
     */
    private String extractInvoiceNumberFromFile(Archive file) {
        try {
            if (file.getStandardMetadata() != null) {
                ParsedInvoice invoice = objectMapper.readValue(file.getStandardMetadata(), ParsedInvoice.class);
                return invoice.getInvoiceNumber();
            }
        } catch (Exception e) {
            log.warn("解析发票元数据失败: fileId={}", file.getId(), e);
        }
        return null;
    }
}


