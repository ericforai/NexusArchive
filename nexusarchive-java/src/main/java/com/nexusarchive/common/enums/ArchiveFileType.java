package com.nexusarchive.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 档案文件类型枚举
 * Reference: DA/T 94-2022 电子文件格式规范
 * 优先使用 OFD 和 PDF 用于长期保存
 */
@Getter
public enum ArchiveFileType {
    
    /**
     * OFD 格式 (优先推荐)
     */
    OFD("OFD", "版式文件格式"),
    
    /**
     * PDF 格式
     */
    PDF("PDF", "便携式文档格式"),
    
    /**
     * XML 格式
     */
    XML("XML", "可扩展标记语言"),
    
    /**
     * JPG/JPEG 图片
     */
    JPG("JPG", "图片格式"),
    
    /**
     * JPEG 图片
     */
    JPEG("JPEG", "图片格式"),
    
    /**
     * PNG 图片
     */
    PNG("PNG", "图片格式");
    
    private final String code;
    private final String description;
    
    ArchiveFileType(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    @JsonValue
    public String getCode() {
        return code;
    }
    
    @JsonCreator
    public static ArchiveFileType fromCode(String code) {
        for (ArchiveFileType type : ArchiveFileType.values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid file type: " + code + 
            ". Must be one of: OFD, PDF, XML, JPG, JPEG, PNG");
    }
    
    /**
     * 验证文件类型是否在白名单中
     */
    public static boolean isValid(String code) {
        try {
            fromCode(code);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
