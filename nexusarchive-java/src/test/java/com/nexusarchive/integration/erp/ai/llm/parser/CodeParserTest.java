// nexusarchive-java/src/test/java/com/nexusarchive/integration/erp/ai/llm/parser/CodeParserTest.java
package com.nexusarchive.integration.erp.ai.llm.parser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CodeParserTest {

    private CodeParser parser;

    @BeforeEach
    void setUp() {
        parser = new CodeParser();
    }

    @Test
    void testExtractJavaCode() throws CodeValidationException {
        String aiResponse = """
            Here's the generated code:

            ```java
            package com.example;

            public class TestAdapter {
                public void test() {}
            }
            ```

            Let me know if you need changes.
        """;

        String code = parser.extractJavaCode(aiResponse);

        assertTrue(code.contains("package com.example"));
        assertTrue(code.contains("class TestAdapter"));
        assertFalse(code.contains("```"));
    }

    @Test
    void testParseMetadata() throws CodeValidationException {
        String javaCode = """
            package com.nexusarchive.adapter;

            public class YonsuiteAdapter {
            }
        """;

        CodeParser.ParsedCodeMetadata metadata = parser.parseMetadata(javaCode);

        assertEquals("com.nexusarchive.adapter", metadata.getPackageName());
        assertEquals("YonsuiteAdapter", metadata.getClassName());
    }

    @Test
    void testInvalidCode() {
        String invalidCode = "This is not Java code at all.";

        assertThrows(CodeValidationException.class, () -> parser.extractJavaCode(invalidCode));
    }
}
