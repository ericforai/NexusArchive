// Input: Java 测试框架
// Output: ErpMetadata DTO 测试
// Pos: test 目录

package com.nexusarchive.integration.erp.dto;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ErpMetadataTest {

    @Test
    void shouldBuildMetadataWithAllFields() {
        // Given
        Set<String> scenarios = Set.of("VOUCHER_SYNC", "ATTACHMENT_SYNC");

        // When
        ErpMetadata metadata = ErpMetadata.builder()
            .identifier("yonsuite")
            .name("用友YonSuite")
            .description("用友新一代企业云服务平台")
            .version("1.0.0")
            .erpType("YONSUITE")
            .supportedScenarios(scenarios)
            .supportsWebhook(true)
            .priority(10)
            .implementationClass("com.nexusarchive.integration.erp.adapter.impl.YonSuiteErpAdapter")
            .registeredAt(LocalDateTime.now())
            .enabled(true)
            .build();

        // Then
        assertEquals("yonsuite", metadata.getIdentifier());
        assertEquals("用友YonSuite", metadata.getName());
        assertEquals(2, metadata.getSupportedScenarios().size());
        assertTrue(metadata.isSupportsWebhook());
        assertEquals(10, metadata.getPriority());
        assertTrue(metadata.isEnabled());
    }

    @Test
    void shouldSupportEmptyConstructor() {
        // When
        ErpMetadata metadata = new ErpMetadata();

        // Then
        assertNotNull(metadata);
        assertNull(metadata.getIdentifier());
    }
}
