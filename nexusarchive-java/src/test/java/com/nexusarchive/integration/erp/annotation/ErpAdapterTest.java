// Input: Java 测试框架
// Output: @ErpAdapter 注解测试
// Pos: test 目录

package com.nexusarchive.integration.erp.annotation;

import com.nexusarchive.integration.erp.annotation.ErpAdapterAnnotation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ErpAdapterAnnotation(
    identifier = "test-adapter",
    name = "测试适配器",
    description = "用于测试的适配器",
    version = "1.0.0",
    erpType = "test",
    supportedScenarios = {"VOUCHER_SYNC"},
    supportsWebhook = true,
    priority = 10
)
class TestAdapterForAnnotation {
    // 测试用类
}

class ErpAdapterTest {

    @Test
    void shouldExtractAnnotationMetadata() {
        // Given
        Class<?> testClass = TestAdapterForAnnotation.class;

        // When
        ErpAdapterAnnotation annotation = testClass.getAnnotation(ErpAdapterAnnotation.class);

        // Then
        assertEquals("test-adapter", annotation.identifier());
        assertEquals("测试适配器", annotation.name());
        assertEquals("用于测试的适配器", annotation.description());
        assertEquals("1.0.0", annotation.version());
        assertEquals("test", annotation.erpType());
        assertEquals(1, annotation.supportedScenarios().length);
        assertEquals("VOUCHER_SYNC", annotation.supportedScenarios()[0]);
        assertEquals(true, annotation.supportsWebhook());
        assertEquals(10, annotation.priority());
    }
}
