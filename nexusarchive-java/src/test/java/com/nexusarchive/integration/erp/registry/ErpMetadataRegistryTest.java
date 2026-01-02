// Input: Java 测试框架
// Output: ErpMetadataRegistry 测试
// Pos: test 目录

package com.nexusarchive.integration.erp.registry;

import com.nexusarchive.integration.erp.annotation.ErpAdapterAnnotation;
import com.nexusarchive.integration.erp.dto.ErpMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@ErpAdapterAnnotation(
    identifier = "test-registry-adapter",
    name = "测试注册适配器",
    erpType = "TEST",
    supportedScenarios = {"VOUCHER_SYNC"},
    priority = 10
)
class TestAdapterForRegistry {}

class ErpMetadataRegistryTest {

    private ErpMetadataRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new ErpMetadataRegistry();
    }

    @Test
    void shouldRegisterAdapterMetadata() {
        // When
        registry.register(TestAdapterForRegistry.class);

        // Then
        ErpMetadata metadata = registry.getByIdentifier("test-registry-adapter");
        assertNotNull(metadata);
        assertEquals("测试注册适配器", metadata.getName());
        assertEquals("TEST", metadata.getErpType());
        assertEquals(Set.of("VOUCHER_SYNC"), metadata.getSupportedScenarios());
    }

    @Test
    void shouldReturnNullForUnregisteredAdapter() {
        // When
        ErpMetadata metadata = registry.getByIdentifier("non-existent");

        // Then
        assertNull(metadata);
    }

    @Test
    void shouldListAllRegisteredAdapters() {
        // When
        registry.register(TestAdapterForRegistry.class);
        Collection<ErpMetadata> all = registry.getAll();

        // Then
        assertEquals(1, all.size());
        assertTrue(all.stream().anyMatch(m -> m.getIdentifier().equals("test-registry-adapter")));
    }

    @Test
    void shouldCheckRegistrationStatus() {
        // Given
        registry.register(TestAdapterForRegistry.class);

        // Then
        assertTrue(registry.isRegistered("test-registry-adapter"));
        assertFalse(registry.isRegistered("non-existent"));
    }

    @Test
    void shouldGroupByErpType() {
        // When
        registry.register(TestAdapterForRegistry.class);
        List<ErpMetadata> testAdapters = registry.getByErpType("TEST");

        // Then
        assertEquals(1, testAdapters.size());
    }
}
