// Input: MockMvc、Mockito、ArchivePreviewController
// Output: ArchivePreviewControllerStandaloneTest 测试用例类
// Pos: 后端测试用例

package com.nexusarchive.controller;

import com.nexusarchive.service.StreamingPreviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class ArchivePreviewControllerStandaloneTest {

    @Mock
    private StreamingPreviewService streamingPreviewService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ArchivePreviewController(streamingPreviewService)).build();
    }

    @Test
    void archivePreviewShouldRejectVirtualNodeIds() throws Exception {
        when(streamingPreviewService.streamPreview(eq("FILE_file-001"), eq("stream"), any(), any()))
            .thenThrow(new IllegalArgumentException("archive/preview 仅支持真实档案主文件预览"));

        mockMvc.perform(post("/archive/preview")
                .param("archiveId", "FILE_file-001")
                .param("mode", "stream"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message").value("archive/preview 仅支持真实档案主文件预览"));
    }
}
