// Input: Java 标准库
// Output: FileMagicValidator 类
// Pos: 工具模块
// [ADDED P1-8] 文件 Magic Number 验证器

package com.nexusarchive.util;

import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 文件 Magic Number 验证器
 * 
 * [FIXED P1-8] 通过检测文件头部的 Magic Number 来验证文件真实类型
 * 防止恶意文件伪装成合法文件格式
 */
@Slf4j
@Component
public class FileMagicValidator {
    
    // 支持的文件类型及其 Magic Number
    private static final Map<String, byte[][]> MAGIC_NUMBERS = new HashMap<>();
    
    // 允许的文件扩展名集合（白名单）
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
        "pdf", "ofd", "xml", "jpg", "jpeg", "png", "gif", "tiff", "tif",
        "doc", "docx", "xls", "xlsx", "zip"
    );
    
    static {
        // PDF: %PDF
        MAGIC_NUMBERS.put("pdf", new byte[][] {
            {0x25, 0x50, 0x44, 0x46}
        });
        
        // OFD: ZIP header (PK..) - OFD 是基于 ZIP 的容器格式
        MAGIC_NUMBERS.put("ofd", new byte[][] {
            {0x50, 0x4B, 0x03, 0x04}
        });
        
        // JPEG: FFD8FF
        MAGIC_NUMBERS.put("jpg", new byte[][] {
            {(byte)0xFF, (byte)0xD8, (byte)0xFF}
        });
        MAGIC_NUMBERS.put("jpeg", new byte[][] {
            {(byte)0xFF, (byte)0xD8, (byte)0xFF}
        });
        
        // PNG: 89 50 4E 47 0D 0A 1A 0A
        MAGIC_NUMBERS.put("png", new byte[][] {
            {(byte)0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}
        });
        
        // GIF: GIF87a or GIF89a
        MAGIC_NUMBERS.put("gif", new byte[][] {
            {0x47, 0x49, 0x46, 0x38, 0x37, 0x61},
            {0x47, 0x49, 0x46, 0x38, 0x39, 0x61}
        });
        
        // TIFF: II or MM
        MAGIC_NUMBERS.put("tiff", new byte[][] {
            {0x49, 0x49, 0x2A, 0x00},
            {0x4D, 0x4D, 0x00, 0x2A}
        });
        MAGIC_NUMBERS.put("tif", new byte[][] {
            {0x49, 0x49, 0x2A, 0x00},
            {0x4D, 0x4D, 0x00, 0x2A}
        });
        
        // XML: <?xml (text file, check for XML declaration)
        MAGIC_NUMBERS.put("xml", new byte[][] {
            {0x3C, 0x3F, 0x78, 0x6D, 0x6C},  // <?xml
            {(byte)0xEF, (byte)0xBB, (byte)0xBF, 0x3C, 0x3F}  // UTF-8 BOM + <?
        });
        
        // ZIP: PK..
        MAGIC_NUMBERS.put("zip", new byte[][] {
            {0x50, 0x4B, 0x03, 0x04},
            {0x50, 0x4B, 0x05, 0x06}  // Empty ZIP
        });
        
        // DOCX/XLSX: ZIP format (Office Open XML)
        MAGIC_NUMBERS.put("docx", new byte[][] {
            {0x50, 0x4B, 0x03, 0x04}
        });
        MAGIC_NUMBERS.put("xlsx", new byte[][] {
            {0x50, 0x4B, 0x03, 0x04}
        });
        
        // DOC: MS Office OLE Compound Document
        MAGIC_NUMBERS.put("doc", new byte[][] {
            {(byte)0xD0, (byte)0xCF, 0x11, (byte)0xE0, (byte)0xA1, (byte)0xB1, 0x1A, (byte)0xE1}
        });
        
        // XLS: MS Office OLE Compound Document
        MAGIC_NUMBERS.put("xls", new byte[][] {
            {(byte)0xD0, (byte)0xCF, 0x11, (byte)0xE0, (byte)0xA1, (byte)0xB1, 0x1A, (byte)0xE1}
        });
    }
    
    /**
     * 验证文件类型是否匹配其声称的扩展名
     * 
     * @param fileContent 文件内容（至少需要前 16 字节）
     * @param fileName 文件名（包含扩展名）
     * @return 验证结果
     */
    public ValidationResult validate(byte[] fileContent, String fileName) {
        if (fileContent == null || fileContent.length < 4) {
            return new ValidationResult(false, "文件内容为空或过短");
        }
        
        if (fileName == null || fileName.isEmpty()) {
            return new ValidationResult(false, "文件名为空");
        }
        
        // 提取扩展名
        String extension = getExtension(fileName).toLowerCase();
        
        // 检查扩展名是否在白名单中
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            return new ValidationResult(false, "不支持的文件类型: " + extension);
        }
        
        // 获取该扩展名对应的 Magic Numbers
        byte[][] expectedMagics = MAGIC_NUMBERS.get(extension);
        
        // 如果没有定义 Magic Number（如某些文本格式），跳过验证
        if (expectedMagics == null) {
            log.debug("No magic number defined for extension: {}", extension);
            return new ValidationResult(true, "无需验证 Magic Number");
        }
        
        // 验证 Magic Number
        for (byte[] magic : expectedMagics) {
            if (matchesMagic(fileContent, magic)) {
                return new ValidationResult(true, "文件类型验证通过");
            }
        }
        
        // 检测实际文件类型
        String detectedType = detectActualType(fileContent);
        
        return new ValidationResult(false, 
            String.format("文件类型不匹配: 声称为 %s，实际检测为 %s", extension, detectedType));
    }
    
    /**
     * 检查是否匹配 Magic Number
     */
    private boolean matchesMagic(byte[] content, byte[] magic) {
        if (content.length < magic.length) {
            return false;
        }
        for (int i = 0; i < magic.length; i++) {
            if (content[i] != magic[i]) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * 检测文件的实际类型
     */
    private String detectActualType(byte[] content) {
        for (Map.Entry<String, byte[][]> entry : MAGIC_NUMBERS.entrySet()) {
            for (byte[] magic : entry.getValue()) {
                if (matchesMagic(content, magic)) {
                    return entry.getKey().toUpperCase();
                }
            }
        }
        return "UNKNOWN";
    }
    
    /**
     * 从文件名中提取扩展名
     */
    private String getExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot < 0 || lastDot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(lastDot + 1);
    }
    
    /**
     * 验证结果类
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String message;
        
        public ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public String getMessage() {
            return message;
        }
    }
}
