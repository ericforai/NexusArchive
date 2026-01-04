// nexusarchive-java/src/test/java/com/nexusarchive/integration/erp/ai/llm/ClaudeApiClientTest.java
package com.nexusarchive.integration.erp.ai.llm;

import com.nexusarchive.config.AiProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named = "CLAUDE_API_KEY", matches = ".+")
class ClaudeApiClientTest {

    private ClaudeApiClient client;
    private AiProperties properties;

    @BeforeEach
    void setUp() {
        properties = new AiProperties();
        properties.setApiKey(System.getProperty("CLAUDE_API_KEY"));
        properties.setModel("claude-3-5-sonnet-20241022");
        properties.setEnabled(true);

        client = new ClaudeApiClient(properties);
    }

    @Test
    void testComplete() {
        String prompt = "Say 'Hello, World!' in Chinese.";
        String response = client.complete(prompt);

        assertNotNull(response);
        assertTrue(response.contains("你好") || response.contains("Hello"));
    }
}
