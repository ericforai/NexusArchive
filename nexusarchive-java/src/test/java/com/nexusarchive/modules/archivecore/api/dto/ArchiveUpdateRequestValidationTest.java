// Input: Jakarta Validation、JUnit
// Output: ArchiveUpdateRequestValidationTest 测试用例类
// Pos: 后端测试用例
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.modules.archivecore.api.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class ArchiveUpdateRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    @DisplayName("更新请求应允许仅提交局部字段")
    void partialUpdateShouldNotRequireCreateFields() {
        ArchiveUpdateRequest request = new ArchiveUpdateRequest();
        request.setTitle("局部更新");
        request.setSummary("仅更新摘要");
        request.setStatus("PENDING_ARCHIVE");

        Set<ConstraintViolation<ArchiveUpdateRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }
}
