// Input: JUnit、Mockito、Jackson、DocumentWorkflowService
// Output: DocumentWorkflowServiceTest 测试用例类
// Pos: 后端测试用例
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.modules.document.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusarchive.modules.document.api.dto.DocumentAssignmentRequest;
import com.nexusarchive.modules.document.api.dto.DocumentLockRequest;
import com.nexusarchive.modules.document.api.dto.DocumentReminderRequest;
import com.nexusarchive.modules.document.api.dto.DocumentSectionDto;
import com.nexusarchive.modules.document.api.dto.DocumentSectionUpdateRequest;
import com.nexusarchive.modules.document.api.dto.DocumentVersionCreateRequest;
import com.nexusarchive.modules.document.domain.DocumentAssignmentEntity;
import com.nexusarchive.modules.document.domain.DocumentLockEntity;
import com.nexusarchive.modules.document.domain.DocumentReminderEntity;
import com.nexusarchive.modules.document.domain.DocumentSectionEntity;
import com.nexusarchive.modules.document.domain.DocumentVersionEntity;
import com.nexusarchive.modules.document.infra.mapper.DocumentAssignmentMapper;
import com.nexusarchive.modules.document.infra.mapper.DocumentLockMapper;
import com.nexusarchive.modules.document.infra.mapper.DocumentReminderMapper;
import com.nexusarchive.modules.document.infra.mapper.DocumentSectionMapper;
import com.nexusarchive.modules.document.infra.mapper.DocumentVersionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class DocumentWorkflowServiceTest {

    @Mock
    private DocumentSectionMapper sectionMapper;

    @Mock
    private DocumentAssignmentMapper assignmentMapper;

    @Mock
    private DocumentLockMapper lockMapper;

    @Mock
    private DocumentReminderMapper reminderMapper;

    @Mock
    private DocumentVersionMapper versionMapper;

    @InjectMocks
    private DocumentWorkflowService documentWorkflowService;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        documentWorkflowService = new DocumentWorkflowService(
                sectionMapper,
                assignmentMapper,
                lockMapper,
                reminderMapper,
                versionMapper,
                objectMapper
        );
    }

    @Test
    void shouldUpsertSectionAndReturnAggregatedState() {
        DocumentSectionUpdateRequest request = new DocumentSectionUpdateRequest();
        request.setTitle("绪论");
        request.setContent("这是内容");
        request.setSortOrder(1);

        DocumentSectionEntity section = DocumentSectionEntity.builder()
                .id("sec-1")
                .projectId("project-1")
                .title("绪论")
                .content("这是内容")
                .sortOrder(1)
                .createdBy("user-1")
                .updatedBy("user-1")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(sectionMapper.selectOne(any())).thenReturn(null, section);
        doAnswer(invocation -> {
            DocumentSectionEntity entity = invocation.getArgument(0);
            entity.setCreatedAt(LocalDateTime.now());
            entity.setUpdatedAt(LocalDateTime.now());
            return 1;
        }).when(sectionMapper).insert(any(DocumentSectionEntity.class));
        when(assignmentMapper.findLatestBySectionId("project-1", "sec-1")).thenReturn(null);
        when(lockMapper.findLatestBySectionId("project-1", "sec-1")).thenReturn(null);
        when(reminderMapper.findBySectionId("project-1", "sec-1")).thenReturn(List.of());

        DocumentSectionDto result = documentWorkflowService.upsertSection("project-1", "sec-1", request, "user-1");

        assertEquals("sec-1", result.getId());
        assertEquals("绪论", result.getTitle());
        assertEquals("这是内容", result.getContent());
        verify(sectionMapper).insert(any(DocumentSectionEntity.class));
    }

    @Test
    void shouldPersistAssignmentLockReminderAndCreateVersionSnapshot() {
        DocumentSectionEntity section = DocumentSectionEntity.builder()
                .id("sec-1")
                .projectId("project-1")
                .title("第一章")
                .content("内容")
                .sortOrder(1)
                .build();
        when(sectionMapper.selectOne(any())).thenReturn(section);
        when(assignmentMapper.findByProjectId("project-1")).thenReturn(List.of());
        when(lockMapper.findByProjectId("project-1")).thenReturn(List.of());
        when(sectionMapper.findByProjectId("project-1")).thenReturn(List.of(section));
        when(reminderMapper.findByProjectId("project-1")).thenReturn(List.of());

        DocumentAssignmentRequest assignmentRequest = new DocumentAssignmentRequest();
        assignmentRequest.setSectionId("sec-1");
        assignmentRequest.setAssigneeId("user-2");
        assignmentRequest.setAssigneeName("张三");

        DocumentLockRequest lockRequest = new DocumentLockRequest();
        lockRequest.setSectionId("sec-1");
        lockRequest.setReason("编辑中");

        DocumentReminderRequest reminderRequest = new DocumentReminderRequest();
        reminderRequest.setSectionId("sec-1");
        reminderRequest.setMessage("明早提醒");
        reminderRequest.setRemindAt(LocalDateTime.parse("2026-03-12T09:00:00"));
        reminderRequest.setRecipientId("user-2");
        reminderRequest.setRecipientName("张三");

        DocumentVersionCreateRequest versionRequest = new DocumentVersionCreateRequest();
        versionRequest.setVersionName("v1");
        versionRequest.setDescription("初始版本");

        doAnswer(invocation -> {
            DocumentAssignmentEntity entity = invocation.getArgument(0);
            entity.setId("asg-1");
            entity.setCreatedAt(LocalDateTime.now());
            return 1;
        }).when(assignmentMapper).insert(any(DocumentAssignmentEntity.class));
        doAnswer(invocation -> {
            DocumentLockEntity entity = invocation.getArgument(0);
            entity.setId("lock-1");
            entity.setCreatedAt(LocalDateTime.now());
            return 1;
        }).when(lockMapper).insert(any(DocumentLockEntity.class));
        doAnswer(invocation -> {
            DocumentReminderEntity entity = invocation.getArgument(0);
            entity.setId("rem-1");
            entity.setCreatedAt(LocalDateTime.now());
            return 1;
        }).when(reminderMapper).insert(any(DocumentReminderEntity.class));
        doAnswer(invocation -> {
            DocumentVersionEntity entity = invocation.getArgument(0);
            entity.setId("ver-1");
            entity.setCreatedAt(LocalDateTime.now());
            return 1;
        }).when(versionMapper).insert(any(DocumentVersionEntity.class));

        documentWorkflowService.createAssignment("project-1", assignmentRequest, "user-1");
        documentWorkflowService.createLock("project-1", lockRequest, "user-1", "管理员");
        documentWorkflowService.createReminder("project-1", reminderRequest, "user-1");
        var version = documentWorkflowService.createVersion("project-1", versionRequest, "user-1");

        assertEquals("ver-1", version.getId());
        ArgumentCaptor<DocumentVersionEntity> captor = ArgumentCaptor.forClass(DocumentVersionEntity.class);
        verify(versionMapper).insert(captor.capture());
        assertNotNull(captor.getValue().getSnapshotPayload());
        assertEquals("v1", captor.getValue().getVersionName());
    }

    @Test
    void shouldRollbackVersionSnapshot() {
        DocumentVersionEntity version = DocumentVersionEntity.builder()
                .id("ver-1")
                .projectId("project-1")
                .versionName("v1")
                .snapshotPayload("""
                        {
                          "sections":[{"id":"sec-1","projectId":"project-1","title":"第一章","content":"回滚内容","sortOrder":1,"createdBy":"user-1","updatedBy":"user-1"}],
                          "assignments":[{"id":"asg-1","projectId":"project-1","sectionId":"sec-1","assigneeId":"user-2","assigneeName":"张三","assignedBy":"user-1","active":true}],
                          "locks":[{"id":"lock-1","projectId":"project-1","sectionId":"sec-1","lockedBy":"user-1","lockedByName":"管理员","reason":"编辑中","active":true}],
                          "reminders":[{"id":"rem-1","projectId":"project-1","sectionId":"sec-1","message":"回滚提醒","remindAt":"2026-03-12T09:00:00","recipientId":"user-2","recipientName":"张三","createdBy":"user-1","delivered":false}]
                        }
                        """)
                .build();
        when(versionMapper.selectById("ver-1")).thenReturn(version);

        var result = documentWorkflowService.rollbackVersion("project-1", "ver-1", "user-9");

        assertEquals("user-9", result.getRolledBackBy());
        verify(reminderMapper).deleteByProjectId("project-1");
        verify(lockMapper).deleteByProjectId("project-1");
        verify(assignmentMapper).deleteByProjectId("project-1");
        verify(sectionMapper).deleteByProjectId("project-1");
        verify(sectionMapper, times(1)).insert(any(DocumentSectionEntity.class));
        verify(assignmentMapper, times(1)).insert(any(DocumentAssignmentEntity.class));
        verify(lockMapper, times(1)).insert(any(DocumentLockEntity.class));
        verify(reminderMapper, times(1)).insert(any(DocumentReminderEntity.class));
        verify(versionMapper).updateById(any(DocumentVersionEntity.class));
    }
}
