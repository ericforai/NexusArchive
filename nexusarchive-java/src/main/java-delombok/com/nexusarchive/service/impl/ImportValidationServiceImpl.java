// Input: ImportValidationService、Java 标准库、Lombok、Spring Framework
// Output: ImportValidationServiceImpl 类
// Pos: 业务服务实现层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl;

import com.nexusarchive.dto.request.ImportError;
import com.nexusarchive.dto.request.ImportRow;
import com.nexusarchive.service.ImportValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 导入数据验证服务实现
 * 
 * OpenSpec 来源: openspec-legacy-data-import.md
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImportValidationServiceImpl implements ImportValidationService {
    
    // 全宗号格式：字母数字下划线，长度 1-50
    private static final Pattern FONDS_NO_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{1,50}$");
    
    // 允许的档案类型
    private static final List<String> ALLOWED_DOC_TYPES = Arrays.asList(
        "凭证", "报表", "账簿", "其他"
    );
    
    // 保管期限名称映射
    private static final java.util.Map<String, String> RETENTION_PERIOD_MAP = java.util.Map.of(
        "永久", "PERMANENT",
        "30年", "30",
        "10年", "10",
        "5年", "5",
        "3年", "3"
    );
    
    @Override
    public ValidationResult validateRow(ImportRow row, int rowNumber, ValidationContext context) {
        List<ImportError> errors = new ArrayList<>();
        
        // 验证必需字段
        if (!StringUtils.hasText(row.getFondsNo())) {
            errors.add(ImportError.builder()
                .rowNumber(rowNumber)
                .fieldName("fonds_no")
                .errorCode("REQUIRED_FIELD_MISSING")
                .errorMessage("全宗号不能为空")
                .build());
        } else if (!validateFondsNo(row.getFondsNo())) {
            errors.add(ImportError.builder()
                .rowNumber(rowNumber)
                .fieldName("fonds_no")
                .errorCode("INVALID_FORMAT")
                .errorMessage("全宗号格式不正确（只能包含字母、数字、下划线、横线，长度1-50）")
                .build());
        } else if (context != null && context.getCurrentFondsNo() != null 
                   && !row.getFondsNo().equals(context.getCurrentFondsNo())) {
            errors.add(ImportError.builder()
                .rowNumber(rowNumber)
                .fieldName("fonds_no")
                .errorCode("FONDS_NO_MISMATCH")
                .errorMessage("同一批导入的全宗号必须一致")
                .build());
        }
        
        if (!StringUtils.hasText(row.getFondsName())) {
            errors.add(ImportError.builder()
                .rowNumber(rowNumber)
                .fieldName("fonds_name")
                .errorCode("REQUIRED_FIELD_MISSING")
                .errorMessage("全宗名称不能为空")
                .build());
        }
        
        if (row.getArchiveYear() == null) {
            errors.add(ImportError.builder()
                .rowNumber(rowNumber)
                .fieldName("archive_year")
                .errorCode("REQUIRED_FIELD_MISSING")
                .errorMessage("归档年度不能为空")
                .build());
        } else if (!validateArchiveYear(row.getArchiveYear())) {
            errors.add(ImportError.builder()
                .rowNumber(rowNumber)
                .fieldName("archive_year")
                .errorCode("INVALID_FORMAT")
                .errorMessage("归档年度必须是有效年份（1900-2100）")
                .build());
        }
        
        if (!StringUtils.hasText(row.getDocType())) {
            errors.add(ImportError.builder()
                .rowNumber(rowNumber)
                .fieldName("doc_type")
                .errorCode("REQUIRED_FIELD_MISSING")
                .errorMessage("档案类型不能为空")
                .build());
        } else if (!ALLOWED_DOC_TYPES.contains(row.getDocType())) {
            errors.add(ImportError.builder()
                .rowNumber(rowNumber)
                .fieldName("doc_type")
                .errorCode("INVALID_VALUE")
                .errorMessage("档案类型不在允许列表中，允许的值：" + ALLOWED_DOC_TYPES)
                .build());
        }
        
        if (!StringUtils.hasText(row.getTitle())) {
            errors.add(ImportError.builder()
                .rowNumber(rowNumber)
                .fieldName("title")
                .errorCode("REQUIRED_FIELD_MISSING")
                .errorMessage("档案标题不能为空")
                .build());
        } else if (row.getTitle().length() > 255) {
            errors.add(ImportError.builder()
                .rowNumber(rowNumber)
                .fieldName("title")
                .errorCode("INVALID_LENGTH")
                .errorMessage("档案标题长度不能超过255")
                .build());
        }
        
        if (!StringUtils.hasText(row.getRetentionPolicyName())) {
            errors.add(ImportError.builder()
                .rowNumber(rowNumber)
                .fieldName("retention_policy_name")
                .errorCode("REQUIRED_FIELD_MISSING")
                .errorMessage("保管期限不能为空")
                .build());
        } else if (resolveRetentionPeriod(row.getRetentionPolicyName()) == null) {
            errors.add(ImportError.builder()
                .rowNumber(rowNumber)
                .fieldName("retention_policy_name")
                .errorCode("INVALID_VALUE")
                .errorMessage("保管期限名称无效，允许的值：永久、30年、10年、5年、3年")
                .build());
        }
        
        // 验证可选字段格式
        if (row.getDocDate() != null && row.getArchiveYear() != null) {
            int docYear = row.getDocDate().getYear();
            if (docYear != row.getArchiveYear()) {
                errors.add(ImportError.builder()
                    .rowNumber(rowNumber)
                    .fieldName("doc_date")
                    .errorCode("YEAR_MISMATCH")
                    .errorMessage("形成日期年份与归档年度不一致")
                    .build());
            }
        }
        
        if (row.getAmount() != null && row.getAmount().compareTo(BigDecimal.ZERO) < 0) {
            errors.add(ImportError.builder()
                .rowNumber(rowNumber)
                .fieldName("amount")
                .errorCode("INVALID_VALUE")
                .errorMessage("金额不能为负数")
                .build());
        }
        
        return new ValidationResult(errors.isEmpty(), errors);
    }
    
    @Override
    public boolean validateFondsNo(String fondsNo) {
        if (!StringUtils.hasText(fondsNo)) {
            return false;
        }
        return FONDS_NO_PATTERN.matcher(fondsNo).matches();
    }
    
    @Override
    public boolean validateArchiveYear(Integer archiveYear) {
        if (archiveYear == null) {
            return false;
        }
        return archiveYear >= 1900 && archiveYear <= 2100;
    }
    
    @Override
    public String resolveRetentionPeriod(String retentionPolicyName) {
        if (!StringUtils.hasText(retentionPolicyName)) {
            return null;
        }
        // 直接匹配
        String period = RETENTION_PERIOD_MAP.get(retentionPolicyName);
        if (period != null) {
            return period;
        }
        // 尝试去除空格后匹配
        String trimmed = retentionPolicyName.trim();
        return RETENTION_PERIOD_MAP.get(trimmed);
    }
}





