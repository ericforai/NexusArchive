// Input: Jackson、org.junit、Spring Framework、Java 标准库、等
// Output: AbnormalVoucherControllerIntegrationTest 测试用例类
// Pos: 后端测试用例
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusarchive.common.enums.DirectionType;
import com.nexusarchive.common.enums.VoucherType;
import com.nexusarchive.dto.sip.AccountingSipDto;
import com.nexusarchive.dto.sip.VoucherEntryDto;
import com.nexusarchive.dto.sip.VoucherHeadDto;
import com.nexusarchive.service.AbnormalVoucherService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 覆盖 /v1/abnormal/{id} 的空体/缺字段验证，确保 400。
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
public class AbnormalVoucherControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AbnormalVoucherService abnormalVoucherService;

    @Test
    void updateSip_emptyBody_returns400() throws Exception {
        mockMvc.perform(put("/v1/abnormal/abc")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateSip_emptyJson_returns400() throws Exception {
        mockMvc.perform(put("/v1/abnormal/abc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateSip_missingRequiredFields_returns400() throws Exception {
        // 缺少 header / entries 等必填字段
        AccountingSipDto dto = new AccountingSipDto();
        String payload = objectMapper.writeValueAsString(dto);

        mockMvc.perform(put("/v1/abnormal/abc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateSip_validPayload_returns200() throws Exception {
        AccountingSipDto dto = buildValidSip();
        String payload = objectMapper.writeValueAsString(dto);

        mockMvc.perform(put("/v1/abnormal/abc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());

        verify(abnormalVoucherService).updateSipData(eq("abc"), any(AccountingSipDto.class));
    }

    private AccountingSipDto buildValidSip() {
        VoucherHeadDto head = new VoucherHeadDto();
        head.setFondsCode("F001");
        head.setAccountPeriod("2025-11");
        head.setVoucherType(VoucherType.RECEIPT);
        head.setVoucherNumber("V-001");
        head.setVoucherDate(LocalDate.now());
        head.setTotalAmount(new BigDecimal("100.00"));
        head.setCurrencyCode("CNY");
        head.setAttachmentCount(0);
        head.setIssuer("tester");
        head.setPostingDate(LocalDate.now());

        VoucherEntryDto entry = new VoucherEntryDto();
        entry.setLineNo(1);
        entry.setSummary("测试分录");
        entry.setSubjectCode("1001");
        entry.setDirection(DirectionType.DEBIT);
        entry.setAmount(new BigDecimal("100.00"));

        AccountingSipDto dto = new AccountingSipDto();
        dto.setRequestId("req-1");
        dto.setSourceSystem("test");
        dto.setHeader(head);
        dto.setEntries(List.of(entry));
        dto.setAttachments(Collections.emptyList());
        return dto;
    }
}
