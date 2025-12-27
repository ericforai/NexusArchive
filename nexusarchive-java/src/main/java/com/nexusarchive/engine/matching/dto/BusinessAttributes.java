// Input: Lombok
// Output: BusinessAttributes DTO
// Pos: 匹配引擎/DTO
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.engine.matching.dto;

import com.nexusarchive.engine.matching.enums.BusinessScene;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 业务属性识别结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessAttributes {
    
    private BusinessScene scene;
    private String templateId;
    private BigDecimal confidence;
    private List<String> reasons;
}
