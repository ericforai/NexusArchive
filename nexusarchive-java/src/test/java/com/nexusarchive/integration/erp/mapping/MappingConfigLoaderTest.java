// Input: JUnit 5, AssertJ, Java 标准库
// Output: MappingConfigLoaderTest 类
// Pos: 集成模块 - ERP 映射配置加载器单元测试

package com.nexusarchive.integration.erp.mapping;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.*;

/**
 * MappingConfigLoader 单元测试
 * 测试映射配置加载器的各种场景
 */
@DisplayName("MappingConfigLoader 单元测试")
@Tag("unit")
class MappingConfigLoaderTest {

    private MappingConfigLoader loader;

    @BeforeEach
    void setUp() {
        loader = new MappingConfigLoader();
    }

    @Test
    @DisplayName("应该成功加载YonSuite映射配置")
    void shouldLoadYonSuiteMappingSuccessfully() throws IOException {
        MappingConfig config = loader.loadMapping("yonsuite");

        assertThat(config).isNotNull();
        assertThat(config.getSourceSystem()).isEqualTo("yonsuite");
        assertThat(config.getTargetModel()).isEqualTo("AccountingSipDto");
        assertThat(config.getVersion()).isNotNull();
        assertThat(config.getHeaderMappings()).isNotEmpty();
        assertThat(config.getEntries()).isNotNull();
        assertThat(config.getAttachments()).isNotNull();
    }

    @Test
    @DisplayName("应该正确解析headerMappings")
    void shouldParseHeaderMappingsCorrectly() throws IOException {
        MappingConfig config = loader.loadMapping("yonsuite");

        assertThat(config.getHeaderMappings()).containsKey("voucherNumber");
        assertThat(config.getHeaderMappings()).containsKey("accountPeriod");
        assertThat(config.getHeaderMappings()).containsKey("attachmentCount");
    }

    @Test
    @DisplayName("应该正确解析entries映射")
    void shouldParseEntriesMappingCorrectly() throws IOException {
        MappingConfig config = loader.loadMapping("yonsuite");

        assertThat(config.getEntries()).isNotNull();
        assertThat(config.getEntries().getSource()).isNotNull();
        assertThat(config.getEntries().getItem()).isNotEmpty();
        assertThat(config.getEntries().getItem()).containsKey("lineNo");
        assertThat(config.getEntries().getItem()).containsKey("summary");
    }

    @Test
    @DisplayName("应该正确解析attachments映射")
    void shouldParseAttachmentsMappingCorrectly() throws IOException {
        MappingConfig config = loader.loadMapping("yonsuite");

        assertThat(config.getAttachments()).isNotNull();
        assertThat(config.getAttachments().getSource()).isNotNull();
        assertThat(config.getAttachments().getItem()).isNotEmpty();
    }

    @Test
    @DisplayName("应该正确解析entries的item字段")
    void shouldParseEntriesItemFields() throws IOException {
        MappingConfig config = loader.loadMapping("yonsuite");

        assertThat(config.getEntries().getItem()).containsKey("accountCode");
        assertThat(config.getEntries().getItem()).containsKey("accountName");
        assertThat(config.getEntries().getItem()).containsKey("debit");
        assertThat(config.getEntries().getItem()).containsKey("credit");
    }

    @Test
    @DisplayName("应该正确解析attachments的item字段")
    void shouldParseAttachmentsItemFields() throws IOException {
        MappingConfig config = loader.loadMapping("yonsuite");

        assertThat(config.getAttachments().getItem()).containsKey("attachmentId");
        assertThat(config.getAttachments().getItem()).containsKey("fileName");
        assertThat(config.getAttachments().getItem()).containsKey("fileSize");
        assertThat(config.getAttachments().getItem()).containsKey("downloadUrl");
    }

    @Test
    @DisplayName("应该正确解析FieldMapping的简单字段映射")
    void shouldParseSimpleFieldMapping() throws IOException {
        MappingConfig config = loader.loadMapping("yonsuite");

        FieldMapping voucherNumberMapping = config.getHeaderMappings().get("voucherNumber");
        assertThat(voucherNumberMapping).isNotNull();
        assertThat(voucherNumberMapping.getField()).isNotNull();
        assertThat(voucherNumberMapping.isScript()).isFalse();
    }

    @Test
    @DisplayName("应该正确解析字段类型和格式")
    void shouldParseFieldTypeAndFormat() throws IOException {
        MappingConfig config = loader.loadMapping("yonsuite");

        FieldMapping voucherDateMapping = config.getHeaderMappings().get("voucherDate");
        assertThat(voucherDateMapping).isNotNull();
        assertThat(voucherDateMapping.getType()).isEqualTo("date");
        assertThat(voucherDateMapping.getFormat()).isEqualTo("yyyy-MM-dd");
    }

    @Test
    @DisplayName("应该正确解析entries的source字段")
    void shouldParseEntriesSourceField() throws IOException {
        MappingConfig config = loader.loadMapping("yonsuite");

        assertThat(config.getEntries().getSource()).isEqualTo("body");
    }

    @Test
    @DisplayName("应该正确解析attachments的source字段")
    void shouldParseAttachmentsSourceField() throws IOException {
        MappingConfig config = loader.loadMapping("yonsuite");

        assertThat(config.getAttachments().getSource()).isEqualTo("attachments");
    }

    @Test
    @DisplayName("当配置文件不存在时应该抛出MappingConfigNotFoundException")
    void shouldThrowExceptionWhenConfigNotFound() {
        assertThatThrownBy(() -> loader.loadMapping("non-existent"))
            .isInstanceOf(MappingConfigNotFoundException.class)
            .hasMessageContaining("non-existent");
    }

    @Test
    @DisplayName("应该正确返回配置版本")
    void shouldReturnCorrectVersion() throws IOException {
        MappingConfig config = loader.loadMapping("yonsuite");

        assertThat(config.getVersion()).isEqualTo("1.0.0");
    }

    @Test
    @DisplayName("mappingExists应该对存在的配置返回true")
    void shouldReturnTrueForExistingMapping() {
        boolean exists = loader.mappingExists("yonsuite");

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("mappingExists应该对不存在的配置返回false")
    void shouldReturnFalseForNonExistingMapping() {
        boolean exists = loader.mappingExists("non-existent");

        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("应该正确解析issuer字段的嵌套路径")
    void shouldParseNestedFieldPath() throws IOException {
        MappingConfig config = loader.loadMapping("yonsuite");

        FieldMapping issuerMapping = config.getHeaderMappings().get("issuer");
        assertThat(issuerMapping).isNotNull();
        assertThat(issuerMapping.getField()).isEqualTo("maker.name");
    }

    @Test
    @DisplayName("应该正确解析accountPeriod字段")
    void shouldParseAccountPeriodField() throws IOException {
        MappingConfig config = loader.loadMapping("yonsuite");

        FieldMapping periodMapping = config.getHeaderMappings().get("accountPeriod");
        assertThat(periodMapping).isNotNull();
        assertThat(periodMapping.getField()).isEqualTo("period");
    }

    @Test
    @DisplayName("应该正确解析voucherDate字段")
    void shouldParseVoucherDateField() throws IOException {
        MappingConfig config = loader.loadMapping("yonsuite");

        FieldMapping voucherDateMapping = config.getHeaderMappings().get("voucherDate");
        assertThat(voucherDateMapping).isNotNull();
        assertThat(voucherDateMapping.getField()).isEqualTo("maketime");
        assertThat(voucherDateMapping.getType()).isEqualTo("date");
        assertThat(voucherDateMapping.getFormat()).isEqualTo("yyyy-MM-dd");
    }
}
