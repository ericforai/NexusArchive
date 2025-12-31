// Input: MatchingStrategy, Archive Entity
// Output: CounterpartyMatchStrategy 类
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
 * 对方单位匹配策略
 * 
 * 通过对方单位名称匹配记账凭证和发票
 * 置信度：60（中等匹配，因为可能存在同名单位）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CounterpartyMatchStrategy implements MatchingStrategy {
    
    private final ObjectMapper objectMapper;
    
    @Override
    public String getName() {
        return "CounterpartyMatch";
    }
    
    @Override
    public int match(Archive voucher, Archive candidate) {
        try {
            // 1. 从凭证中提取对方单位
            String voucherCounterparty = extractCounterpartyFromVoucher(voucher);
            if (voucherCounterparty == null || voucherCounterparty.isEmpty()) {
                return 0;
            }
            
            // 2. 从候选文件中提取对方单位
            String candidateCounterparty = extractCounterpartyFromFile(candidate);
            if (candidateCounterparty == null || candidateCounterparty.isEmpty()) {
                return 0;
            }
            
            // 3. 模糊匹配（去除空格、统一大小写）
            String normalizedVoucher = normalizeCounterparty(voucherCounterparty);
            String normalizedCandidate = normalizeCounterparty(candidateCounterparty);
            
            if (normalizedVoucher.equals(normalizedCandidate)) {
                log.debug("对方单位匹配成功: voucher={}, candidate={}, counterparty={}", 
                    voucher.getArchiveCode(), candidate.getArchiveCode(), candidateCounterparty);
                return 60; // 中等匹配
            }
            
            // 4. 包含匹配（如果一方包含另一方）
            if (normalizedVoucher.contains(normalizedCandidate) || 
                normalizedCandidate.contains(normalizedVoucher)) {
                log.debug("对方单位部分匹配: voucher={}, candidate={}", 
                    voucher.getArchiveCode(), candidate.getArchiveCode());
                return 40; // 部分匹配
            }
            
        } catch (Exception e) {
            log.warn("对方单位匹配策略执行失败: voucher={}, candidate={}", 
                voucher.getArchiveCode(), candidate.getArchiveCode(), e);
        }
        
        return 0;
    }
    
    /**
     * 从凭证中提取对方单位
     */
    private String extractCounterpartyFromVoucher(Archive voucher) {
        // TODO: 从 voucher 的 metadata_ext 或 standardMetadata 中提取对方单位
        // 这里简化处理，实际需要根据数据结构解析
        return null;
    }
    
    /**
     * 从文件中提取对方单位
     */
    private String extractCounterpartyFromFile(Archive file) {
        try {
            if (file.getStandardMetadata() != null) {
                ParsedInvoice invoice = objectMapper.readValue(file.getStandardMetadata(), ParsedInvoice.class);
                return invoice.getBuyerName(); // 或 sellerName，根据发票类型
            }
        } catch (Exception e) {
            log.warn("解析发票元数据失败: fileId={}", file.getId(), e);
        }
        return null;
    }
    
    /**
     * 标准化对方单位名称
     */
    private String normalizeCounterparty(String counterparty) {
        if (counterparty == null) {
            return "";
        }
        return counterparty.trim()
            .replaceAll("\\s+", "")
            .toUpperCase()
            .replaceAll("[（()）]", ""); // 去除括号
    }
}

