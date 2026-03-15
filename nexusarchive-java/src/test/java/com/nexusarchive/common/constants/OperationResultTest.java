// Input: JUnit 5、OperationResult 常量类
// Output: OperationResultTest 测试类
// Pos: 测试模块/常量测试

package com.nexusarchive.common.constants;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * OperationResult 常量类单元测试
 *
 * 验证操作结果常量值的正确性和唯一性
 */
@DisplayName("OperationResult 常量测试")
@Tag("unit")
class OperationResultTest {

    @Test
    @DisplayName("SUCCESS 常量值应为 'SUCCESS'")
    void successConstantShouldBeCorrect() {
        assertEquals("SUCCESS", OperationResult.SUCCESS);
    }

    @Test
    @DisplayName("FAIL 常量值应为 'FAIL'")
    void failConstantShouldBeCorrect() {
        assertEquals("FAIL", OperationResult.FAIL);
    }

    @Test
    @DisplayName("PENDING 常量值应为 'PENDING'")
    void pendingConstantShouldBeCorrect() {
        assertEquals("PENDING", OperationResult.PENDING);
    }

    @Test
    @DisplayName("UNKNOWN 常量值应为 'UNKNOWN'")
    void unknownConstantShouldBeCorrect() {
        assertEquals("UNKNOWN", OperationResult.UNKNOWN);
    }

    @Test
    @DisplayName("所有常量值应唯一")
    void allConstantsShouldBeUnique() {
        assertNotEquals(OperationResult.SUCCESS, OperationResult.FAIL);
        assertNotEquals(OperationResult.SUCCESS, OperationResult.PENDING);
        assertNotEquals(OperationResult.SUCCESS, OperationResult.UNKNOWN);
        assertNotEquals(OperationResult.FAIL, OperationResult.PENDING);
        assertNotEquals(OperationResult.FAIL, OperationResult.UNKNOWN);
        assertNotEquals(OperationResult.PENDING, OperationResult.UNKNOWN);
    }

    @Test
    @DisplayName("常量值不应为空")
    void constantsShouldNotBeBlank() {
        assertFalse(OperationResult.SUCCESS.isBlank());
        assertFalse(OperationResult.FAIL.isBlank());
        assertFalse(OperationResult.PENDING.isBlank());
        assertFalse(OperationResult.UNKNOWN.isBlank());
    }

    @Test
    @DisplayName("常量值应为大写")
    void constantsShouldBeUpperCase() {
        assertEquals(OperationResult.SUCCESS, OperationResult.SUCCESS.toUpperCase());
        assertEquals(OperationResult.FAIL, OperationResult.FAIL.toUpperCase());
        assertEquals(OperationResult.PENDING, OperationResult.PENDING.toUpperCase());
        assertEquals(OperationResult.UNKNOWN, OperationResult.UNKNOWN.toUpperCase());
    }
}
