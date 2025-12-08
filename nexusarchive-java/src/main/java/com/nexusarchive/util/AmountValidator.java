package com.nexusarchive.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Locale;

/**
 * 会计金额精度校验工具
 * 确保符合中国会计准则精度要求
 */
@Slf4j
@Component
public class AmountValidator {
    
    // 默认保留两位小数
    private static final int DEFAULT_SCALE = 2;
    
    // 最大金额限制（可根据实际业务调整）
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("999999999999.99");
    
    // 最小金额（通常为0，但根据业务需求可能允许负数，如退款）
    private static final BigDecimal MIN_AMOUNT = new BigDecimal("-999999999999.99");
    
    /**
     * 校验会计金额精度和有效性
     * @param amount 待校验金额
     * @return 校验结果
     */
    public ValidationResult validateAmount(BigDecimal amount) {
        if (amount == null) {
            return ValidationResult.fail("金额不能为空");
        }
        
        // 检查精度
        if (amount.scale() > DEFAULT_SCALE) {
            return ValidationResult.fail("金额精度超过两位小数");
        }
        
        // 检查范围
        if (amount.compareTo(MAX_AMOUNT) > 0) {
            return ValidationResult.fail("金额超过最大允许值");
        }
        
        if (amount.compareTo(MIN_AMOUNT) < 0) {
            return ValidationResult.fail("金额低于最小允许值");
        }
        
        return ValidationResult.success();
    }
    
    /**
     * 校验会计金额精度和有效性（支持自定义限制）
     * @param amount 待校验金额
     * @param minAmount 最小金额
     * @param maxAmount 最大金额
     * @param scale 小数位数
     * @return 校验结果
     */
    public ValidationResult validateAmount(BigDecimal amount, BigDecimal minAmount, BigDecimal maxAmount, int scale) {
        if (amount == null) {
            return ValidationResult.fail("金额不能为空");
        }
        
        // 检查精度
        if (amount.scale() > scale) {
            return ValidationResult.fail("金额精度超过" + scale + "位小数");
        }
        
        // 检查范围
        if (maxAmount != null && amount.compareTo(maxAmount) > 0) {
            return ValidationResult.fail("金额超过最大允许值" + maxAmount);
        }
        
        if (minAmount != null && amount.compareTo(minAmount) < 0) {
            return ValidationResult.fail("金额低于最小允许值" + minAmount);
        }
        
        return ValidationResult.success();
    }
    
    /**
     * 标准化金额格式
     * 确保金额格式符合会计准则
     * @param amount 原始金额
     * @return 标准化后的金额
     */
    public BigDecimal normalizeAmount(BigDecimal amount) {
        if (amount == null) {
            return null;
        }
        return amount.setScale(DEFAULT_SCALE, RoundingMode.HALF_UP);
    }
    
    /**
     * 标准化金额格式（支持自定义小数位）
     * @param amount 原始金额
     * @param scale 小数位数
     * @return 标准化后的金额
     */
    public BigDecimal normalizeAmount(BigDecimal amount, int scale) {
        if (amount == null) {
            return null;
        }
        return amount.setScale(scale, RoundingMode.HALF_UP);
    }
    
    /**
     * 格式化金额为货币显示格式
     * @param amount 金额
     * @param locale 地区设置
     * @return 格式化后的货币字符串
     */
    public String formatCurrency(BigDecimal amount, Locale locale) {
        if (amount == null) {
            return "0.00";
        }
        
        Currency currency = Currency.getInstance(locale);
        java.text.NumberFormat currencyFormat = java.text.NumberFormat.getCurrencyInstance(locale);
        return currencyFormat.format(amount);
    }
    
    /**
     * 格式化金额为人民币显示格式
     * @param amount 金额
     * @return 格式化后的货币字符串
     */
    public String formatRMB(BigDecimal amount) {
        return formatCurrency(amount, Locale.CHINA);
    }
    
    /**
     * 校验结果
     */
    public static class ValidationResult {
        private boolean valid;
        private String message;
        
        private ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }
        
        public static ValidationResult success() {
            return new ValidationResult(true, null);
        }
        
        public static ValidationResult fail(String message) {
            return new ValidationResult(false, message);
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public String getMessage() {
            return message;
        }
    }
}