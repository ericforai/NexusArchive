// Input: JUnit 5, FieldMapping
// Output: FieldMapping 单元测试
// Pos: 集成模块 - ERP 映射配置测试

package com.nexusarchive.integration.erp.mapping;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * FieldMapping 单元测试
 * 验证 Lombok 生成的 setter/getter 方法
 */
@DisplayName("FieldMapping 单元测试")
@Tag("unit")
public class FieldMappingTest {

    @Test
    public void testFieldMappingHasScriptSetter() {
        // 验证 Lombok 生成了 setter 方法
        FieldMapping mapping = new FieldMapping();
        assertDoesNotThrow(() -> mapping.setScript("test script"));
        assertEquals("test script", mapping.getScript());
    }

    @Test
    public void testFieldMappingIsScriptMethod() {
        FieldMapping mapping = new FieldMapping();
        assertFalse(mapping.hasScript()); // null script

        mapping.setScript("return 1;");
        assertTrue(mapping.hasScript()); // has script

        mapping.setScript("");
        assertFalse(mapping.hasScript()); // blank script

        mapping.setScript("   ");
        assertFalse(mapping.hasScript()); // whitespace only
    }

    @Test
    public void testFieldMappingBuilder() {
        FieldMapping mapping = FieldMapping.builder()
            .field("testField")
            .script("test script")
            .type("string")
            .build();

        assertEquals("testField", mapping.getField());
        assertEquals("test script", mapping.getScript());
        assertEquals("string", mapping.getType());
    }

    @Test
    public void testFieldMappingAllFields() {
        FieldMapping mapping = FieldMapping.builder()
            .field("source.field")
            .script("return ctx['field'] * 2;")
            .type("decimal")
            .format("#,##0.00")
            .build();

        assertEquals("source.field", mapping.getField());
        assertEquals("return ctx['field'] * 2;", mapping.getScript());
        assertEquals("decimal", mapping.getType());
        assertEquals("#,##0.00", mapping.getFormat());
        assertTrue(mapping.hasScript());
    }

    @Test
    public void testFieldMappingSetters() {
        FieldMapping mapping = new FieldMapping();
        mapping.setField("testField");
        mapping.setScript("test script");
        mapping.setType("string");
        mapping.setFormat("#,##0.00");

        assertEquals("testField", mapping.getField());
        assertEquals("test script", mapping.getScript());
        assertEquals("string", mapping.getType());
        assertEquals("#,##0.00", mapping.getFormat());
    }
}
