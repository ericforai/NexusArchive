// Input: JUnit 5
// Output: DataMaskingService 单元测试
// Pos: NexusCore test
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core.masking;

import com.nexusarchive.core.masking.DataMaskingProperties.MaskingRule;
import com.nexusarchive.core.masking.DataMaskingProperties.MaskPattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultDataMaskingServiceTests {

    private DefaultDataMaskingService maskingService;
    private DataMaskingProperties properties;

    @BeforeEach
    void setUp() {
        properties = new DataMaskingProperties();
        maskingService = new DefaultDataMaskingService(properties);
    }

    @Test
    void mask_shouldApplyStartEndHideRule() {
        // Arrange
        MaskingRule rule = new MaskingRule();
        rule.setFieldMatch("idCard");
        rule.setPattern(MaskPattern.KEEP_3_4);
        properties.setRules(Collections.singletonList(rule));

        // Act
        String result = maskingService.mask("idCard", "110101199001011234"); // 18位

        // Assert
        // 前3 (110) 后4 (1234)，中间 11位 *
        // 110***********1234
        assertThat(result).isEqualTo("110***********1234");
    }
    
    @Test
    void mask_shouldApplyMiddleHideRule() {
        // Arrange
        MaskingRule rule = new MaskingRule();
        rule.setFieldMatch("phone");
        rule.setPattern(MaskPattern.MIDDLE_4);
        properties.setRules(Collections.singletonList(rule));

        // Act
        String result = maskingService.mask("phone", "13812345678");

        // Assert
        // 138 **** 5678 (11 - 4 = 7, start=3)
        // 138****5678
        assertThat(result).isEqualTo("138****5678");
    }

    @Test
    void maskObject_shouldMaskMatchingFields() {
        // Arrange
        MaskingRule rule = new MaskingRule();
        rule.setFieldMatch("bankAccount");
        rule.setPattern(MaskPattern.FULL);
        properties.setRules(Collections.singletonList(rule));
        
        TestEntity entity = new TestEntity();
        entity.setBankAccount("1234567890");
        entity.setName("John Doe");

        // Act
        maskingService.maskObject(entity);

        // Assert
        assertThat(entity.getBankAccount()).isEqualTo("**********");
        assertThat(entity.getName()).isEqualTo("John Doe"); // Not masked
    }
    
    static class TestEntity {
        private String bankAccount;
        private String name;
        
        public String getBankAccount() { return bankAccount; }
        public void setBankAccount(String bankAccount) { this.bankAccount = bankAccount; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }
}
