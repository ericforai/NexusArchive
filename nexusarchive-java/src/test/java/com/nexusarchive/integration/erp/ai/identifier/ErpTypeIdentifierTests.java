// nexusarchive-java/src/test/java/com/nexusarchive/integration/erp/ai/identifier/ErpTypeIdentifierTests.java
// Input: JUnit 5, testing framework
// Output: Unit tests for ErpTypeIdentifier
// Pos: AI 模块 - ERP 类型识别器测试

package com.nexusarchive.integration.erp.ai.identifier;

import com.nexusarchive.integration.erp.ai.parser.OpenApiDefinition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.nexusarchive.integration.erp.ai.identifier.ErpTypeIdentifier.ErpType;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * ERP 类型识别器测试
 * <p>
 * 测试从文件名和 OpenAPI 文档内容识别 ERP 类型的功能
 * </p>
 */
@DisplayName("ERP 类型识别器测试")
class ErpTypeIdentifierTests {

    private final ErpTypeIdentifier identifier = new ErpTypeIdentifier();

    @Test
    @DisplayName("通过文件名识别 YonSuite")
    void testIdentifyYonSuiteByFileName() {
        // Arrange
        String fileName = "yonsuite-salesout-api.json";
        OpenApiDefinition document = OpenApiDefinition.builder()
                .path("/api/v1/test")
                .method("GET")
                .build();

        // Act
        ErpType result = identifier.identify(fileName, document);

        // Assert
        assertEquals(ErpType.YONSUITE, result,
                "Should identify YonSuite from filename containing 'yonsuite'");
    }

    @Test
    @DisplayName("通过文件名识别 Kingdee")
    void testIdentifyKingdeeByFileName() {
        // Arrange
        String fileName = "kingdee-api.json";
        OpenApiDefinition document = OpenApiDefinition.builder()
                .path("/api/v1/test")
                .method("GET")
                .build();

        // Act
        ErpType result = identifier.identify(fileName, document);

        // Assert
        assertEquals(ErpType.KINGDEE, result,
                "Should identify Kingdee from filename containing 'kingdee'");
    }

    @Test
    @DisplayName("通过 API 路径识别 YonSuite")
    void testIdentifyYonSuiteByPath() {
        // Arrange
        String fileName = "unknown-api.json";
        OpenApiDefinition document = OpenApiDefinition.builder()
                .path("/yonbip/digitalModel/salesout/doc/query")
                .method("POST")
                .build();

        // Act
        ErpType result = identifier.identify(fileName, document);

        // Assert
        assertEquals(ErpType.YONSUITE, result,
                "Should identify YonSuite from API path containing '/yonbip/'");
    }

    @Test
    @DisplayName("通过 API 路径识别 Kingdee")
    void testIdentifyKingdeeByPath() {
        // Arrange
        String fileName = "unknown-api.json";
        OpenApiDefinition document = OpenApiDefinition.builder()
                .path("/k3cloud/K3Cloud/EXECUTION")
                .method("POST")
                .build();

        // Act
        ErpType result = identifier.identify(fileName, document);

        // Assert
        assertEquals(ErpType.KINGDEE, result,
                "Should identify Kingdee from API path containing '/k3cloud/'");
    }

    @Test
    @DisplayName("默认识别为通用类型")
    void testDefaultToGeneric() {
        // Arrange
        String fileName = "unknown-api.json";
        OpenApiDefinition document = OpenApiDefinition.builder()
                .path("/api/v1/test")
                .method("GET")
                .build();

        // Act
        ErpType result = identifier.identify(fileName, document);

        // Assert
        assertEquals(ErpType.GENERIC, result,
                "Should default to GENERIC when no known patterns are found");
    }

    @Test
    @DisplayName("通过文件名识别泛微 OA (weaver)")
    void testIdentifyWeaverByFileName() {
        // Arrange
        String fileName = "weaver-ecology-api.json";
        OpenApiDefinition document = OpenApiDefinition.builder()
                .path("/api/v1/test")
                .method("GET")
                .build();

        // Act
        ErpType result = identifier.identify(fileName, document);

        // Assert
        assertEquals(ErpType.WEAVER, result,
                "Should identify Weaver from filename containing 'weaver'");
    }

    @Test
    @DisplayName("通过文件名识别 YonSuite (yonbip)")
    void testIdentifyYonSuiteByYonbipKeyword() {
        // Arrange
        String fileName = "yonbip-finance-api.json";
        OpenApiDefinition document = OpenApiDefinition.builder()
                .path("/api/v1/test")
                .method("GET")
                .build();

        // Act
        ErpType result = identifier.identify(fileName, document);

        // Assert
        assertEquals(ErpType.YONSUITE, result,
                "Should identify YonSuite from filename containing 'yonbip'");
    }

    @Test
    @DisplayName("通过文件名识别 YonSuite (yonyou)")
    void testIdentifyYonSuiteByYonyouKeyword() {
        // Arrange
        String fileName = "yonyou-api-spec.json";
        OpenApiDefinition document = OpenApiDefinition.builder()
                .path("/api/v1/test")
                .method("GET")
                .build();

        // Act
        ErpType result = identifier.identify(fileName, document);

        // Assert
        assertEquals(ErpType.YONSUITE, result,
                "Should identify YonSuite from filename containing 'yonyou'");
    }

    @Test
    @DisplayName("通过文件名识别 Kingdee (k3cloud)")
    void testIdentifyKingdeeByK3cloudKeyword() {
        // Arrange
        String fileName = "k3cloud-api.json";
        OpenApiDefinition document = OpenApiDefinition.builder()
                .path("/api/v1/test")
                .method("GET")
                .build();

        // Act
        ErpType result = identifier.identify(fileName, document);

        // Assert
        assertEquals(ErpType.KINGDEE, result,
                "Should identify Kingdee from filename containing 'k3cloud'");
    }

    @Test
    @DisplayName("通过文件名识别泛微 OA (ecology)")
    void testIdentifyWeaverByEcologyKeyword() {
        // Arrange
        String fileName = "ecology-workflow-api.json";
        OpenApiDefinition document = OpenApiDefinition.builder()
                .path("/api/v1/test")
                .method("GET")
                .build();

        // Act
        ErpType result = identifier.identify(fileName, document);

        // Assert
        assertEquals(ErpType.WEAVER, result,
                "Should identify Weaver from filename containing 'ecology'");
    }

    @Test
    @DisplayName("文件名匹配优先级高于路径")
    void testFileNameTakesPrecedenceOverPath() {
        // Arrange
        String fileName = "kingdee-api.json";
        OpenApiDefinition document = OpenApiDefinition.builder()
                .path("/yonbip/test")  // Path suggests YonSuite
                .method("GET")
                .build();

        // Act
        ErpType result = identifier.identify(fileName, document);

        // Assert
        assertEquals(ErpType.KINGDEE, result,
                "Filename should take precedence over path when both match different ERPs");
    }

    @Test
    @DisplayName("处理 null 路径")
    void testHandleNullPath() {
        // Arrange
        String fileName = "unknown-api.json";
        OpenApiDefinition document = OpenApiDefinition.builder()
                .path(null)
                .method("GET")
                .build();

        // Act
        ErpType result = identifier.identify(fileName, document);

        // Assert
        assertEquals(ErpType.GENERIC, result,
                "Should handle null path gracefully");
    }

    @Test
    @DisplayName("处理空文件名")
    void testHandleEmptyFileName() {
        // Arrange
        String fileName = "";
        OpenApiDefinition document = OpenApiDefinition.builder()
                .path("/yonbip/test")
                .method("GET")
                .build();

        // Act
        ErpType result = identifier.identify(fileName, document);

        // Assert
        assertEquals(ErpType.YONSUITE, result,
                "Should identify from path even with empty filename");
    }

    @Test
    @DisplayName("验证枚举值")
    void testErpTypeEnumValues() {
        // Assert
        assertEquals("YonSuite", ErpType.YONSUITE.getDisplayName());
        assertEquals("yonsuite", ErpType.YONSUITE.getCode());

        assertEquals("Kingdee", ErpType.KINGDEE.getDisplayName());
        assertEquals("kingdee", ErpType.KINGDEE.getCode());

        assertEquals("泛微OA", ErpType.WEAVER.getDisplayName());
        assertEquals("weaver", ErpType.WEAVER.getCode());

        assertEquals("通用", ErpType.GENERIC.getDisplayName());
        assertEquals("generic", ErpType.GENERIC.getCode());
    }
}
