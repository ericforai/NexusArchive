package com.nexusarchive.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusarchive.dto.sso.ConsumeTicketResponse;
import com.nexusarchive.dto.sso.ErpLaunchRequest;
import com.nexusarchive.dto.sso.ErpLaunchResponse;
import com.nexusarchive.dto.response.LoginResponse;
import com.nexusarchive.exception.ErpSsoException;
import com.nexusarchive.service.sso.ErpSsoLaunchService;
import com.nexusarchive.service.sso.SsoErrorCodes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ErpSsoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ErpSsoLaunchService erpSsoLaunchService;

    @Test
    @DisplayName("launch should return ticket")
    void launch_should_return_ticket() throws Exception {
        ErpLaunchRequest request = new ErpLaunchRequest();
        request.setAccbookCode("BR01");
        request.setErpUserJobNo("1001");
        request.setVoucherNo("记-8");
        request.setTimestamp(1739230000L);
        request.setNonce("nonce-a");

        when(erpSsoLaunchService.launch(eq("ERP_A"), eq("sig"), any(ErpLaunchRequest.class))).thenReturn(
                ErpLaunchResponse.builder()
                        .launchTicket("ticket-1")
                        .expiresInSeconds(60L)
                        .launchUrl("/system/sso/launch?ticket=ticket-1")
                        .build());

        mockMvc.perform(post("/erp/sso/launch")
                        .header("X-Client-Id", "ERP_A")
                        .header("X-Signature", "sig")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.launchTicket").value("ticket-1"));
    }

    @Test
    @DisplayName("consume should return business error code")
    void consume_should_return_business_error_code() throws Exception {
        when(erpSsoLaunchService.consume("bad")).thenThrow(
                new ErpSsoException(SsoErrorCodes.TICKET_ALREADY_USED, "ticket 已使用", 409));

        mockMvc.perform(post("/erp/sso/consume")
                        .param("ticket", "bad"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(409))
                .andExpect(jsonPath("$.message").value("TICKET_ALREADY_USED: ticket 已使用"));
    }

    @Test
    @DisplayName("consume should return token and voucher")
    void consume_should_return_token_and_voucher() throws Exception {
        LoginResponse.UserInfo user = new LoginResponse.UserInfo();
        user.setId("u1");
        user.setUsername("zhangsan");

        when(erpSsoLaunchService.consume("ok")).thenReturn(
                ConsumeTicketResponse.builder()
                        .token("jwt-token")
                        .user(user)
                        .voucherNo("记-8")
                        .build());

        mockMvc.perform(post("/erp/sso/consume")
                        .param("ticket", "ok"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.token").value("jwt-token"))
                .andExpect(jsonPath("$.data.voucherNo").value("记-8"));
    }
}
