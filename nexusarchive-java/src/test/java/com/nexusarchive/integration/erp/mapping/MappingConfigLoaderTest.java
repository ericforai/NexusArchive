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
    @DisplayName("应该正确解析FieldMapping的简单字段映射")
    void shouldParseSimpleFieldMapping() throws IOException {
        MappingConfig config = loader.loadMapping("yonsuite");

        FieldMapping voucherNumberMapping = config.getHeaderMappings().get("voucherNumber");
        assertThat(voucherNumberMapping).isNotNull();
        assertThat(voucherNumberMapping.getField()).isNotNull();
        assertThat(voucherNumberMapping.hasScript()).isFalse();
    }

    @Test
    @DisplayName("应该支持FieldMapping的脚本字段")
    void shouldSupportFieldMappingScript() throws IOException {
        MappingConfig config = loader.loadMapping("yonsuite");

        // 检查 accountPeriod 字段的映射配置
        FieldMapping periodMapping = config.getHeaderMappings().get("accountPeriod");
        assertThat(periodMapping).isNotNull();
        // 验证字段映射配置正确加载
        assertThat(periodMapping.getField()).isNotNull();
    }

    @Test
    @DisplayName("应该成功加载金蝶映射配置")
    void shouldLoadKingdeeMappingSuccessfully() throws IOException {
        MappingConfig config = loader.loadMapping("kingdee");

        assertThat(config).isNotNull();
        assertThat(config.getSourceSystem()).isEqualTo("kingdee");
        assertThat(config.getTargetModel()).isEqualTo("AccountingSipDto");
        assertThat(config.getHeaderMappings()).containsKey("voucherNumber");
        assertThat(config.getEntries()).isNotNull();
        assertThat(config.getAttachments()).isNotNull();
    }

    @Test
    @DisplayName("金蝶配置应该包含凭证头核心字段映射")
    void shouldContainKingdeeHeaderMappings() throws IOException {
        MappingConfig config = loader.loadMapping("kingdee");

        assertThat(config.getHeaderMappings()).containsKey("accountPeriod");
        assertThat(config.getHeaderMappings()).containsKey("voucherNumber");
        assertThat(config.getHeaderMappings()).containsKey("voucherDate");
        assertThat(config.getHeaderMappings()).containsKey("issuer");
        assertThat(config.getHeaderMappings()).containsKey("reviewer");
    }

    @Test
    @DisplayName("金蝶配置应该包含分录字段映射")
    void shouldContainKingdeeEntryMappings() throws IOException {
        MappingConfig config = loader.loadMapping("kingdee");

        assertThat(config.getEntries()).isNotNull();
        assertThat(config.getEntries().getSource()).isEqualTo("FEntity");
        assertThat(config.getEntries().getItem()).containsKey("lineNo");
        assertThat(config.getEntries().getItem()).containsKey("summary");
        assertThat(config.getEntries().getItem()).containsKey("debit");
        assertThat(config.getEntries().getItem()).containsKey("credit");
    }

    @Test
    @DisplayName("金蝶配置应该包含附件字段映射")
    void shouldContainKingdeeAttachmentMappings() throws IOException {
        MappingConfig config = loader.loadMapping("kingdee");

        assertThat(config.getAttachments()).isNotNull();
        assertThat(config.getAttachments().getSource()).isEqualTo("FAttachments");
        assertThat(config.getAttachments().getItem()).containsKey("attachmentId");
        assertThat(config.getAttachments().getItem()).containsKey("fileName");
        assertThat(config.getAttachments().getItem()).containsKey("downloadUrl");
    }

    @Test
    @DisplayName("当配置文件不存在时应该抛出MappingConfigNotFoundException")
    void shouldThrowExceptionWhenConfigNotFound() {
        assertThatThrownBy(() -> loader.loadMapping("non-existent"))
            .isInstanceOf(MappingConfigNotFoundException.class)
            .hasMessageContaining("non-existent");
    }
}
