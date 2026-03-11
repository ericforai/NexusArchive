// Input: MockMvc standalone、Mockito、DocumentWorkflowController
// Output: DocumentWorkflowControllerStandaloneTest 测试用例类
// Pos: 后端测试用例
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusarchive.modules.document.api.dto.DocumentAssignmentDto;
import com.nexusarchive.modules.document.api.dto.DocumentAssignmentRequest;
import com.nexusarchive.modules.document.api.dto.DocumentLockDto;
import com.nexusarchive.modules.document.api.dto.DocumentLockRequest;
import com.nexusarchive.modules.document.api.dto.DocumentReminderDto;
import com.nexusarchive.modules.document.api.dto.DocumentReminderRequest;
import com.nexusarchive.modules.document.api.dto.DocumentSectionDto;
import com.nexusarchive.modules.document.api.dto.DocumentSectionUpdateRequest;
import com.nexusarchive.modules.document.api.dto.DocumentVersionCreateRequest;
import com.nexusarchive.modules.document.api.dto.DocumentVersionDto;
import com.nexusarchive.modules.document.app.DocumentWorkflowService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("unit")
class DocumentWorkflowControllerStandaloneTest {

    private MockMvc mockMvc;
    private DocumentWorkflowService documentWorkflowService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        documentWorkflowService = mock(DocumentWorkflowService.class);
        objectMapper = new ObjectMapper().findAndRegisterModules();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new DocumentWorkflowController(documentWorkflowService))
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void shouldServeDocumentWorkflowEndpoints() throws Exception {
        when(documentWorkflowService.getSection("project-1", "sec-1"))
                .thenReturn(DocumentSectionDto.builder().id("sec-1").projectId("project-1").title("第一章").content("内容").build());

        mockMvc.perform(get("/documents/project-1/editor/sections/sec-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("sec-1"))
                .andExpect(jsonPath("$.data.title").value("第一章"));

        DocumentSectionUpdateRequest updateRequest = new DocumentSectionUpdateRequest();
        updateRequest.setTitle("更新标题");
        updateRequest.setContent("更新内容");
        when(documentWorkflowService.upsertSection(eq("project-1"), eq("sec-1"), any(DocumentSectionUpdateRequest.class), eq("user-1")))
                .thenReturn(DocumentSectionDto.builder().id("sec-1").projectId("project-1").title("更新标题").content("更新内容").build());

        mockMvc.perform(put("/documents/project-1/editor/sections/sec-1")
                        .requestAttr("userId", "user-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("更新标题"));

        DocumentAssignmentRequest assignmentRequest = new DocumentAssignmentRequest();
        assignmentRequest.setSectionId("sec-1");
        assignmentRequest.setAssigneeId("user-2");
        when(documentWorkflowService.createAssignment(eq("project-1"), any(DocumentAssignmentRequest.class), eq("user-1")))
                .thenReturn(DocumentAssignmentDto.builder().id("asg-1").sectionId("sec-1").assigneeId("user-2").build());

        mockMvc.perform(post("/documents/project-1/editor/assignments")
                        .requestAttr("userId", "user-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignmentRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("asg-1"));

        DocumentLockRequest lockRequest = new DocumentLockRequest();
        lockRequest.setSectionId("sec-1");
        lockRequest.setReason("正在编辑");
        when(documentWorkflowService.createLock(eq("project-1"), any(DocumentLockRequest.class), eq("user-1"), eq("未知用户")))
                .thenReturn(DocumentLockDto.builder().id("lock-1").sectionId("sec-1").reason("正在编辑").build());

        mockMvc.perform(post("/documents/project-1/editor/locks")
                        .requestAttr("userId", "user-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(lockRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("lock-1"));

        DocumentReminderRequest reminderRequest = new DocumentReminderRequest();
        reminderRequest.setSectionId("sec-1");
        reminderRequest.setMessage("请处理");
        reminderRequest.setRemindAt(LocalDateTime.parse("2026-03-12T10:00:00"));
        reminderRequest.setRecipientId("user-2");
        when(documentWorkflowService.createReminder(eq("project-1"), any(DocumentReminderRequest.class), eq("user-1")))
                .thenReturn(DocumentReminderDto.builder().id("rem-1").sectionId("sec-1").message("请处理").build());

        mockMvc.perform(post("/documents/project-1/editor/reminders")
                        .requestAttr("userId", "user-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reminderRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("rem-1"));

        DocumentVersionCreateRequest versionRequest = new DocumentVersionCreateRequest();
        versionRequest.setVersionName("v1");
        versionRequest.setDescription("首版");
        when(documentWorkflowService.createVersion(eq("project-1"), any(DocumentVersionCreateRequest.class), eq("user-1")))
                .thenReturn(DocumentVersionDto.builder().id("ver-1").projectId("project-1").versionName("v1").build());
        when(documentWorkflowService.listVersions("project-1"))
                .thenReturn(List.of(DocumentVersionDto.builder().id("ver-1").projectId("project-1").versionName("v1").build()));
        when(documentWorkflowService.rollbackVersion("project-1", "ver-1", "user-1"))
                .thenReturn(DocumentVersionDto.builder().id("ver-1").projectId("project-1").versionName("v1").rolledBackBy("user-1").build());

        mockMvc.perform(post("/documents/project-1/versions")
                        .requestAttr("userId", "user-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(versionRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("ver-1"));

        mockMvc.perform(get("/documents/project-1/versions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value("ver-1"));

        mockMvc.perform(post("/documents/project-1/versions/ver-1/rollback")
                        .requestAttr("userId", "user-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rolledBackBy").value("user-1"));
    }
}
