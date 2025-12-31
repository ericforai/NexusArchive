// Input: MockMvc
// Output: 测试结果
// Pos: NexusCore integration tests
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusarchive.core.compliance.AuditLogService;
import com.nexusarchive.core.service.StreamingPreviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ArchivePreviewController.class)
public class ArchivePreviewIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private StreamingPreviewService previewService;

    @MockBean
    private AuditLogService auditLogService;

    private String archiveId;
    private String fileId;

    @BeforeEach
    void setUp() {
        archiveId = "ARCH-001";
        fileId = "FILE-001";
    }

    @Test
    @WithMockUser(username = "testUser", roles = "ARCHIVIST")
    void testPreviewStream() throws Exception {
        ArchivePreviewRequest request = new ArchivePreviewRequest();
        request.setArchiveId(archiveId);
        request.setFileId(fileId);
        request.setMode("stream");

        byte[] contentBytes = "PDF-content-simulation".getBytes();
        ResponseEntity<Resource> response = ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .body(new ByteArrayResource(contentBytes));
        when(previewService.preview(eq(archiveId), eq(fileId), eq("stream"),
                isNull(), isNull(), anyString(), anyString()))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/archives/preview")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE))
                .andExpect(content().bytes(contentBytes));
    }

    @Test
    @WithMockUser
    void testPreviewRange() throws Exception {
        ArchivePreviewRequest request = new ArchivePreviewRequest();
        request.setArchiveId(archiveId);
        request.setFileId(fileId);
        request.setMode("stream");

        byte[] contentBytes = "PDF-".getBytes();
        ResponseEntity<Resource> response = ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .contentType(MediaType.APPLICATION_PDF)
                .body(new ByteArrayResource(contentBytes));
        when(previewService.preview(eq(archiveId), eq(fileId), eq("stream"),
                eq(0L), eq(3L), anyString(), anyString()))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/archives/preview")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.RANGE, "bytes=0-3")
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isPartialContent())
                .andExpect(content().bytes(contentBytes));
    }
}
