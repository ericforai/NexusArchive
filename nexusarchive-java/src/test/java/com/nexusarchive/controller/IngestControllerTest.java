package com.nexusarchive.controller;

import com.nexusarchive.common.result.Result;
import com.nexusarchive.dto.ArchiveRequest;
import com.nexusarchive.dto.mapper.DtoMapper;
import com.nexusarchive.dto.sip.AccountingSipDto;
import com.nexusarchive.dto.sip.IngestResponse;
import com.nexusarchive.dto.sip.VoucherHeadDto;
import com.nexusarchive.mapper.IngestRequestStatusMapper;
import com.nexusarchive.service.IngestService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(IngestController.class)
@Tag("unit")
@org.junit.jupiter.api.Disabled("Disabled due to complex security dependency chain. Needs dedicated security test config.")
public class IngestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IngestService ingestService;

    @MockBean
    private IngestRequestStatusMapper statusMapper;

    @MockBean
    private DtoMapper dtoMapper;

    @MockBean
    private com.nexusarchive.modules.borrowing.app.BorrowingFacade borrowingFacade;

    @MockBean
    private com.nexusarchive.service.AuthTicketValidationService authTicketValidationService;

    @MockBean
    private com.nexusarchive.service.AuditLogService auditLogService;

    @MockBean
    private com.nexusarchive.service.FondsScopeService fondsScopeService;

    @MockBean
    private com.nexusarchive.util.JwtUtil jwtUtil;

    @MockBean
    private com.nexusarchive.service.TokenBlacklistService tokenBlacklistService;

    @Test
    @DisplayName("普通用户（无管理权限）应该被拒绝访问 SIP 接收接口")
    @WithMockUser(authorities = "archive:view")
    void ingestSip_NormalUser_ShouldBeForbidden() throws Exception {
        mockMvc.perform(post("/v1/archive/sip/ingest")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"requestId\":\"test-123\", \"header\":{\"fondsCode\":\"F001\"}}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("档案管理员应该允许访问 SIP 接收接口")
    @WithMockUser(authorities = "archive:manage")
    void ingestSip_AdminUser_ShouldBeAllowed() throws Exception {
        when(ingestService.ingestSip(any())).thenReturn(IngestResponse.builder().status("RECEIVED").build());

        mockMvc.perform(post("/v1/archive/sip/ingest")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"requestId\":\"test-123\", \"header\":{\"fondsCode\":\"F001\"}}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("验证归档接口连通性（带管理权限）")
    @WithMockUser(authorities = "archive:manage")
    void archivePoolItems_BasicCheck() throws Exception {
        mockMvc.perform(post("/v1/archive/sip/archive")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"poolItemIds\":[\"123\"]}"))
                .andExpect(status().isOk());
    }
}
