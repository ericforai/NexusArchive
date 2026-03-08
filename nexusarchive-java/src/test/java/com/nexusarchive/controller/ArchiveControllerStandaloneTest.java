// Input: MockMvc、Mockito、ArchiveController
// Output: ArchiveControllerStandaloneTest 测试用例类
// Pos: 后端测试用例
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusarchive.dto.mapper.DtoMapper;
import com.nexusarchive.modules.archivecore.api.dto.ArchiveUpdateRequest;
import com.nexusarchive.modules.archivecore.app.ArchiveFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class ArchiveControllerStandaloneTest {

    @Mock
    private ArchiveFacade archiveFacade;

    @Mock
    private DtoMapper dtoMapper;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(new ArchiveController(archiveFacade, dtoMapper)).build();
    }

    @Test
    void updateShouldAcceptPartialPayload() throws Exception {
        String archiveId = "arc-001";
        ArchiveUpdateRequest request = new ArchiveUpdateRequest();
        request.setTitle("Partial Update");
        request.setSummary("summary only");
        request.setStatus("PENDING_ARCHIVE");

        doNothing().when(archiveFacade).updateArchive(eq(archiveId), any(ArchiveUpdateRequest.class));

        mockMvc.perform(put("/archives/{id}", archiveId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(archiveFacade).updateArchive(eq(archiveId), any(ArchiveUpdateRequest.class));
    }
}
