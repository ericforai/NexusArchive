// Input: MockMvc、Mockito、PreviewController
// Output: PreviewControllerStandaloneTest 测试用例类
// Pos: 后端测试用例

package com.nexusarchive.controller;

import com.nexusarchive.service.StreamingPreviewService;
import com.nexusarchive.dto.PreviewResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class PreviewControllerStandaloneTest {

    @Mock
    private StreamingPreviewService streamingPreviewService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new PreviewController(streamingPreviewService)).build();
    }

    @Test
    void previewShouldRejectMissingFileIdForFileResource() throws Exception {
        when(streamingPreviewService.streamPreview(eq("file"), isNull(), isNull(), eq("stream"), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
            .thenThrow(new IllegalArgumentException("fileId 不能为空"));

        mockMvc.perform(post("/preview")
                .param("resourceType", "file"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message").value("fileId 不能为空"));
    }

    @Test
    void previewShouldDispatchFileResourceToStreamingService() throws Exception {
        when(streamingPreviewService.streamPreview(eq("file"), isNull(), eq("file-001"), eq("stream"), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
            .thenReturn(new PreviewResponse());

        mockMvc.perform(post("/preview")
                .param("resourceType", "file")
                .param("fileId", "file-001")
                .param("mode", "stream"))
            .andExpect(status().isOk());

        verify(streamingPreviewService)
            .streamPreview(eq("file"), isNull(), eq("file-001"), eq("stream"), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void previewGetShouldAlsoDispatchFileResourceToStreamingService() throws Exception {
        when(streamingPreviewService.streamPreview(eq("file"), isNull(), eq("file-001"), eq("stream"), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
            .thenReturn(new PreviewResponse());

        mockMvc.perform(get("/preview")
                .param("resourceType", "file")
                .param("fileId", "file-001"))
            .andExpect(status().isOk());

        verify(streamingPreviewService)
            .streamPreview(eq("file"), isNull(), eq("file-001"), eq("stream"), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }
}
