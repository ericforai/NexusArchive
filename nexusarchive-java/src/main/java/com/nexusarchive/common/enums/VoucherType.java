package com.nexusarchive.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 凭证类型枚举
 * Reference: DA/T 94-2022 会计凭证分类
 */
@Getter
public enum VoucherType {
    
    /**
     * 收款凭证
     */
    RECEIPT("RECEIPT", "收款凭证"),
    
    /**
     * 付款凭证
     */
    PAYMENT("PAYMENT", "付款凭证"),
    
    /**
     * 转账凭证
     */
    TRANSFER("TRANSFER", "转账凭证");
    
    private final String code;
    private final String description;
    
    VoucherType(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    @JsonValue
    public String getCode() {
        return code;
    }
    
    @JsonCreator
    public static VoucherType fromCode(String code) {
        for (VoucherType type : VoucherType.values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid voucher type: " + code + 
            ". Must be one of: RECEIPT, PAYMENT, TRANSFER");
    }
}
