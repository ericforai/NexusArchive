// Input: JUnit 5、Java 标准库
// Output: SapHttpClient 测试类
// Pos: 单元测试 - SAP OData HTTP 客户端
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.integration.erp.adapter.client;

import com.nexusarchive.integration.erp.dto.ErpConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SAP HTTP Client 单元测试
 * 验证 OData URL 构建逻辑
 */
@Tag("unit")
class SapHttpClientTest {

    private SapHttpClient client;

    @BeforeEach
    void setUp() {
        this.client = new SapHttpClient();
    }

    @Test
    void testBuildQueryUrl() {
        ErpConfig config = ErpConfig.builder()
            .baseUrl("https://sap.example.com")
            .tenantId("BR01")
            .appKey("user")
            .appSecret("password")
            .build();

        String requestUrl = client.buildQueryUrl(
            config,
            "2024-01-01",
            "2024-01-31"
        );

        assertThat(requestUrl)
            .contains("https://sap.example.com")
            .contains("$filter=CompanyCode%20eq%20'BR01'")
            .contains("PostingDate%20ge%202024-01-01")
            .contains("PostingDate%20le%202024-01-31");
    }

    @Test
    void testBuildQueryUrlWithSpecialCharactersInBaseUrl() {
        ErpConfig config = ErpConfig.builder()
            .baseUrl("https://sap.example.com:8080")
            .tenantId("BR01")
            .build();

        String requestUrl = client.buildQueryUrl(
            config,
            "2024-01-01",
            "2024-12-31"
        );

        assertThat(requestUrl)
            .contains("https://sap.example.com:8080")
            .contains("BR01");
    }

    @Test
    void testBuildDetailUrl() {
        ErpConfig config = ErpConfig.builder()
            .baseUrl("https://sap.example.com")
            .tenantId("BR01")
            .build();

        String detailUrl = client.buildDetailUrl(
            config,
            "10000001",
            "2024"
        );

        assertThat(detailUrl)
            .contains("https://sap.example.com")
            .contains("JournalEntry='10000001'")
            .contains("FiscalYear='2024'")
            .contains("$expand=to_JournalEntryItem,to_Attachment");
    }

    @Test
    void testMapDebitCreditCode() {
        assertThat(SapHttpClient.mapDebitCreditCode("S")).isEqualTo("DEBIT");
        assertThat(SapHttpClient.mapDebitCreditCode("H")).isEqualTo("CREDIT");
        assertThat(SapHttpClient.mapDebitCreditCode("X")).isNull();
    }
}
