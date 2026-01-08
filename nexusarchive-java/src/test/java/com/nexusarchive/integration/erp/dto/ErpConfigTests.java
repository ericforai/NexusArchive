// Input: JUnit 5、Java 测试框架
// Output: ErpConfig 测试类
// Pos: test 目录 - 验证 ERP 配置 DTO
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.integration.erp.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ErpConfig 单元测试
 *
 * <p>测试 ERP 配置 DTO 的基础功能</p>
 *
 * @author Agent D (基础设施工程师)
 */
@Tag("unit")
@DisplayName("ErpConfig 测试")
class ErpConfigTests {

    @Nested
    @DisplayName("resolveAllAccbookCodes() - 合并账套代码")
    class ResolveAllAccbookCodesTests {

        @Test
        @DisplayName("优先使用 accbookCodes 数组")
        void shouldPreferAccbookCodesArray() {
            // Given
            ErpConfig config = ErpConfig.builder()
                    .accbookCode("SINGLE")
                    .accbookCodes(java.util.List.of("MULTI1", "MULTI2"))
                    .build();

            // When
            java.util.List<String> result = config.resolveAllAccbookCodes();

            // Then: 优先使用 accbookCodes
            assertEquals(2, result.size());
            assertEquals("MULTI1", result.get(0));
            assertEquals("MULTI2", result.get(1));
        }

        @Test
        @DisplayName("只有 accbookCode 时返回单个元素列表")
        void shouldReturnSingleAccbookCode() {
            // Given
            ErpConfig config = ErpConfig.builder()
                    .accbookCode("SINGLE")
                    .accbookCodes(null)
                    .build();

            // When
            java.util.List<String> result = config.resolveAllAccbookCodes();

            // Then
            assertEquals(java.util.List.of("SINGLE"), result);
        }

        @Test
        @DisplayName("所有字段为空时返回空列表")
        void shouldReturnEmptyListWhenAllFieldsAreEmpty() {
            // Given
            ErpConfig config = ErpConfig.builder()
                    .accbookCode(null)
                    .accbookCodes(null)
                    .build();

            // When
            java.util.List<String> result = config.resolveAllAccbookCodes();

            // Then
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("accbookCodes 为空列表时返回空列表")
        void shouldReturnEmptyListWhenAccbookCodesIsEmpty() {
            // Given
            ErpConfig config = ErpConfig.builder()
                    .accbookCode(null)
                    .accbookCodes(java.util.List.of())
                    .build();

            // When
            java.util.List<String> result = config.resolveAllAccbookCodes();

            // Then
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("Builder 和 Lombok 注解测试")
    class BuilderTests {

        @Test
        @DisplayName("@Builder 应正常工作")
        void shouldBuildWithAllFields() {
            // When
            ErpConfig config = ErpConfig.builder()
                    .id("test-id")
                    .name("测试配置")
                    .adapterType("yonsuite")
                    .baseUrl("https://api.example.com")
                    .appKey("key123")
                    .appSecret("secret123")
                    .tenantId("tenant-001")
                    .accbookCode("BR01")
                    .accbookCodes(java.util.List.of("BR01", "BR02"))
                    .accbookMapping("{\"BR01\": \"FONDS_A\"}")
                    .extraConfig("{\"timeout\": 30}")
                    .enabled(true)
                    .build();

            // Then
            assertEquals("test-id", config.getId());
            assertEquals("测试配置", config.getName());
            assertEquals("yonsuite", config.getAdapterType());
            assertEquals("BR01", config.getAccbookCode());
            assertEquals(2, config.getAccbookCodes().size());
            assertEquals("{\"BR01\": \"FONDS_A\"}", config.getAccbookMapping());
            assertTrue(config.getEnabled());
        }

        @Test
        @DisplayName("@NoArgsConstructor 应创建空对象")
        void shouldCreateEmptyInstanceWithNoArgsConstructor() {
            // When
            ErpConfig config = new ErpConfig();

            // Then
            assertNotNull(config);
            assertNull(config.getId());
            assertNull(config.getName());
        }

        @Test
        @DisplayName("@AllArgsConstructor 应工作")
        void shouldCreateInstanceWithAllArgsConstructor() {
            // When
            ErpConfig config = new ErpConfig(
                    "id", "name", "adapter", "url",
                    "key", "secret", "tenant", "accbook",
                    java.util.List.of("a", "b"), "{\"a\": \"b\"}",
                    "{}", true
            );

            // Then
            assertEquals("id", config.getId());
            assertEquals("name", config.getName());
            assertEquals("{\"a\": \"b\"}", config.getAccbookMapping());
        }
    }

    @Nested
    @DisplayName("accbookMapping 字段测试")
    class AccbookMappingTests {

        @Test
        @DisplayName("accbookMapping 可设置为 JSON 字符串")
        void shouldSetAccbookMappingAsJsonString() {
            // Given
            String jsonMapping = "{\"BR01\": \"FONDS_A\", \"BR02\": \"FONDS_B\"}";

            // When
            ErpConfig config = ErpConfig.builder()
                    .accbookMapping(jsonMapping)
                    .build();

            // Then
            assertEquals(jsonMapping, config.getAccbookMapping());
        }

        @Test
        @DisplayName("accbookMapping 可为 null")
        void shouldAllowNullAccbookMapping() {
            // When
            ErpConfig config = ErpConfig.builder()
                    .accbookMapping(null)
                    .build();

            // Then
            assertNull(config.getAccbookMapping());
        }
    }
}
