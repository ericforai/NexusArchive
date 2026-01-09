// Input: JUnit 5、Jackson、Java 标准库
// Output: SAP Journal Entry DTO 测试
// Pos: 单元测试 - SAP OData 响应反序列化
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.integration.erp.dto.sap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SAP Journal Entry DTO 测试
 * 验证 SAP S/4HANA OData 响应的 JSON 反序列化
 */
@Tag("unit")
class SapJournalEntryDtoTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        this.objectMapper = new ObjectMapper();
    }

    @Test
    void testJsonDeserialization() throws JsonProcessingException {
        String json = """
            {
              "JournalEntry": "10000001",
              "CompanyCode": "BR01",
              "FiscalYear": "2024",
              "PostingDate": "2024-01-15",
              "DocumentHeaderText": "测试凭证",
              "CreationDate": "2024-01-15",
              "CreationTime": "14:30:00",
              "CreatedByUser": "TEST_USER",
              "to_JournalEntryItem": [
                {
                  "JournalEntryItem": "1",
                  "GLAccount": "100100",
                  "DebitCreditCode": "S",
                  "AmountInTransactionCurrency": "1000.00",
                  "TransactionCurrency": "CNY",
                  "DocumentItemText": "测试分录"
                }
              ],
              "to_Attachment": [
                {
                  "FileName": "invoice.pdf",
                  "FileSize": "12345",
                  "URL": "https://sap.example.com/attachments/invoice.pdf"
                }
              ]
            }
            """;

        SapJournalEntryDto dto = objectMapper.readValue(json, SapJournalEntryDto.class);

        assertThat(dto.getJournalEntry()).isEqualTo("10000001");
        assertThat(dto.getCompanyCode()).isEqualTo("BR01");
        assertThat(dto.getFiscalYear()).isEqualTo("2024");
        assertThat(dto.getPostingDate()).isEqualTo("2024-01-15");
        assertThat(dto.getDocumentHeaderText()).isEqualTo("测试凭证");
        assertThat(dto.getCreatedByUser()).isEqualTo("TEST_USER");
        assertThat(dto.getItems()).hasSize(1);
        assertThat(dto.getItems().get(0).getJournalEntryItem()).isEqualTo("1");
        assertThat(dto.getItems().get(0).getGlAccount()).isEqualTo("100100");
        assertThat(dto.getItems().get(0).getDebitCreditCode()).isEqualTo("S");
        assertThat(dto.getItems().get(0).getAmountInTransactionCurrency()).isEqualTo("1000.00");
        assertThat(dto.getAttachments()).hasSize(1);
        assertThat(dto.getAttachments().get(0).getFileName()).isEqualTo("invoice.pdf");
    }

    @Test
    void testJsonDeserializationWithEmptyItems() throws JsonProcessingException {
        String json = """
            {
              "JournalEntry": "10000002",
              "CompanyCode": "BR01",
              "FiscalYear": "2024",
              "PostingDate": "2024-01-16",
              "DocumentHeaderText": "无分录凭证",
              "to_JournalEntryItem": [],
              "to_Attachment": []
            }
            """;

        SapJournalEntryDto dto = objectMapper.readValue(json, SapJournalEntryDto.class);

        assertThat(dto.getJournalEntry()).isEqualTo("10000002");
        assertThat(dto.getItems()).isEmpty();
        assertThat(dto.getAttachments()).isEmpty();
    }

    @Test
    void testSapErrorResponseDeserialization() throws JsonProcessingException {
        String json = """
            {
              "error": {
                "code": "500",
                "message": {
                  "lang": "en",
                  "value": "Internal Server Error"
                }
              }
            }
            """;

        SapErrorResponse error = objectMapper.readValue(json, SapErrorResponse.class);

        assertThat(error.getError().getCode()).isEqualTo("500");
        assertThat(error.getError().getMessage().getValue()).isEqualTo("Internal Server Error");
    }
}
