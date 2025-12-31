// Input: Java 标准库
// Output: ImportValidationService 接口
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import com.nexusarchive.dto.request.ImportError;
import com.nexusarchive.dto.request.ImportRow;

import java.util.List;

/**
 * 导入数据验证服务
 * 
 * OpenSpec 来源: openspec-legacy-data-import.md
 */
public interface ImportValidationService {
    
    /**
     * 验证单行数据
     * 
     * @param row 导入行数据
     * @param rowNumber 行号
     * @param context 验证上下文
     * @return 验证结果（包含错误列表）
     */
    ValidationResult validateRow(ImportRow row, int rowNumber, ValidationContext context);
    
    /**
     * 验证全宗号格式
     * 
     * @param fondsNo 全宗号
     * @return 是否有效
     */
    boolean validateFondsNo(String fondsNo);
    
    /**
     * 验证归档年度
     * 
     * @param archiveYear 归档年度
     * @return 是否有效
     */
    boolean validateArchiveYear(Integer archiveYear);
    
    /**
     * 解析保管期限名称到系统值
     * 
     * @param retentionPolicyName 保管期限名称（如：永久、30年、10年）
     * @return 系统保管期限值（如：PERMANENT、30、10）
     */
    String resolveRetentionPeriod(String retentionPolicyName);
    
    /**
     * 验证结果
     */
    class ValidationResult {
        private boolean valid;
        private List<ImportError> errors;
        
        public ValidationResult(boolean valid, List<ImportError> errors) {
            this.valid = valid;
            this.errors = errors;
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public List<ImportError> getErrors() {
            return errors;
        }
    }
    
    /**
     * 验证上下文
     */
    class ValidationContext {
        private String currentFondsNo; // 当前批次的全宗号（用于一致性校验）
        
        public String getCurrentFondsNo() {
            return currentFondsNo;
        }
        
        public void setCurrentFondsNo(String currentFondsNo) {
            this.currentFondsNo = currentFondsNo;
        }
    }
}

