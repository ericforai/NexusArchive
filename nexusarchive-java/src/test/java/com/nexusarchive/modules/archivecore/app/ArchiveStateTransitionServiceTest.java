// Input: JUnit 5、Mockito、ArchiveStatus、Archive
// Output: ArchiveStateTransitionServiceTest 测试类
// Pos: 测试

package com.nexusarchive.modules.archivecore.app;

import com.nexusarchive.common.enums.ArchiveStatus;
import com.nexusarchive.common.exception.BusinessException;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.modules.archivecore.api.dto.ArchiveStatusChangeRequest;
import com.nexusarchive.service.ArchiveReadService;
import com.nexusarchive.service.ArchiveWriteService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import org.mockito.ArgumentCaptor;

/**
 * ArchiveStateTransitionService 测试
 * <p>遵循 TDD 原则：先写测试，定义状态转换行为
 */
@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("ArchiveStateTransitionService 测试")
class ArchiveStateTransitionServiceTest {

    @Mock
    private ArchiveReadService archiveReadService;

    @Mock
    private ArchiveWriteService archiveWriteService;

    @InjectMocks
    private ArchiveStateTransitionService stateTransitionService;

    @Test
    @DisplayName("应该支持 DRAFT -> PENDING 转换")
    void shouldAllowDraftToPendingTransition() {
        // Given
        Archive archive = createArchiveWithStatus(ArchiveStatus.DRAFT);
        when(archiveReadService.getArchiveById("arc-001")).thenReturn(archive);

        ArchiveStatusChangeRequest request = new ArchiveStatusChangeRequest();
        request.setTargetStatus(ArchiveStatus.PENDING);
        request.setReason("提交审核");

        // When
        stateTransitionService.transitionStatus("arc-001", request, "user-001");

        // Then - 使用 ArgumentCaptor 验证传递给 updateArchive 的对象
        ArgumentCaptor<Archive> archiveCaptor = ArgumentCaptor.forClass(Archive.class);
        verify(archiveWriteService).updateArchive(eq("arc-001"), archiveCaptor.capture());
        assertThat(archiveCaptor.getValue().getStatus()).isEqualTo(ArchiveStatus.PENDING.getCode());
    }

    @Test
    @DisplayName("应该支持 PENDING -> ARCHIVED 转换")
    void shouldAllowPendingToArchivedTransition() {
        // Given
        Archive archive = createArchiveWithStatus(ArchiveStatus.PENDING);
        when(archiveReadService.getArchiveById("arc-001")).thenReturn(archive);

        ArchiveStatusChangeRequest request = new ArchiveStatusChangeRequest();
        request.setTargetStatus(ArchiveStatus.ARCHIVED);
        request.setReason("审核通过");

        // When
        stateTransitionService.transitionStatus("arc-001", request, "user-001");

        // Then
        ArgumentCaptor<Archive> archiveCaptor = ArgumentCaptor.forClass(Archive.class);
        verify(archiveWriteService).updateArchive(eq("arc-001"), archiveCaptor.capture());
        assertThat(archiveCaptor.getValue().getStatus()).isEqualTo(ArchiveStatus.ARCHIVED.getCode());
    }

    @Test
    @DisplayName("应该支持 PENDING -> DRAFT 转换（拒绝审核）")
    void shouldAllowPendingToDraftTransition() {
        // Given
        Archive archive = createArchiveWithStatus(ArchiveStatus.PENDING);
        when(archiveReadService.getArchiveById("arc-001")).thenReturn(archive);

        ArchiveStatusChangeRequest request = new ArchiveStatusChangeRequest();
        request.setTargetStatus(ArchiveStatus.DRAFT);
        request.setReason("审核拒绝");

        // When
        stateTransitionService.transitionStatus("arc-001", request, "user-001");

        // Then
        ArgumentCaptor<Archive> archiveCaptor = ArgumentCaptor.forClass(Archive.class);
        verify(archiveWriteService).updateArchive(eq("arc-001"), archiveCaptor.capture());
        assertThat(archiveCaptor.getValue().getStatus()).isEqualTo(ArchiveStatus.DRAFT.getCode());
    }

    @Test
    @DisplayName("应该拒绝 ARCHIVED -> DRAFT 转换（终态不可逆）")
    void shouldRejectArchivedToDraftTransition() {
        // Given
        Archive archive = createArchiveWithStatus(ArchiveStatus.ARCHIVED);
        when(archiveReadService.getArchiveById("arc-001")).thenReturn(archive);

        ArchiveStatusChangeRequest request = new ArchiveStatusChangeRequest();
        request.setTargetStatus(ArchiveStatus.DRAFT);

        // When & Then
        assertThatThrownBy(() ->
            stateTransitionService.transitionStatus("arc-001", request, "user-001")
        ).isInstanceOf(BusinessException.class)
         .hasMessageContaining("非法"); // 简化断言，匹配"非法的状态转换"

        verify(archiveWriteService, never()).updateArchive(any(), any());
    }

    @Test
    @DisplayName("应该拒绝 DRAFT -> ARCHIVED 转换（需先经过 PENDING）")
    void shouldRejectDraftToArchivedTransition() {
        // Given
        Archive archive = createArchiveWithStatus(ArchiveStatus.DRAFT);
        when(archiveReadService.getArchiveById("arc-001")).thenReturn(archive);

        ArchiveStatusChangeRequest request = new ArchiveStatusChangeRequest();
        request.setTargetStatus(ArchiveStatus.ARCHIVED);

        // When & Then
        assertThatThrownBy(() ->
            stateTransitionService.transitionStatus("arc-001", request, "user-001")
        ).isInstanceOf(BusinessException.class)
         .hasMessageContaining("非法"); // 简化断言，匹配"非法的状态转换"

        verify(archiveWriteService, never()).updateArchive(any(), any());
    }

    @Test
    @DisplayName("乐观锁冲突应该抛出异常")
    void shouldThrowOnOptimisticLockConflict() {
        // Given
        Archive archive = createArchiveWithStatus(ArchiveStatus.DRAFT);
        archive.setVersion(5);
        when(archiveReadService.getArchiveById("arc-001")).thenReturn(archive);
        doThrow(new OptimisticLockingFailureException("版本冲突"))
            .when(archiveWriteService).updateArchive(eq("arc-001"), any(Archive.class));

        ArchiveStatusChangeRequest request = new ArchiveStatusChangeRequest();
        request.setTargetStatus(ArchiveStatus.PENDING);

        // When & Then
        assertThatThrownBy(() ->
            stateTransitionService.transitionStatus("arc-001", request, "user-001")
        ).isInstanceOf(BusinessException.class)
         .hasMessageContaining("版本冲突");
    }

    @Test
    @DisplayName("预期版本不匹配应该抛出异常")
    void shouldThrowWhenExpectedVersionMismatch() {
        // Given
        Archive archive = createArchiveWithStatus(ArchiveStatus.DRAFT);
        archive.setVersion(5);
        when(archiveReadService.getArchiveById("arc-001")).thenReturn(archive);

        ArchiveStatusChangeRequest request = new ArchiveStatusChangeRequest();
        request.setTargetStatus(ArchiveStatus.PENDING);
        request.setExpectedVersion(3); // 与实际版本 5 不匹配

        // When & Then
        assertThatThrownBy(() ->
            stateTransitionService.transitionStatus("arc-001", request, "user-001")
        ).isInstanceOf(BusinessException.class)
         .hasMessageContaining("版本冲突");

        verify(archiveWriteService, never()).updateArchive(any(), any());
    }

    @Test
    @DisplayName("null 状态应该默认为 DRAFT")
    void shouldTreatNullStatusAsDraft() {
        // Given
        Archive archive = createArchiveWithStatus(null);
        when(archiveReadService.getArchiveById("arc-001")).thenReturn(archive);

        ArchiveStatusChangeRequest request = new ArchiveStatusChangeRequest();
        request.setTargetStatus(ArchiveStatus.PENDING);

        // When
        stateTransitionService.transitionStatus("arc-001", request, "user-001");

        // Then - 使用 ArgumentCaptor 验证传递给 updateArchive 的对象
        ArgumentCaptor<Archive> archiveCaptor = ArgumentCaptor.forClass(Archive.class);
        verify(archiveWriteService).updateArchive(eq("arc-001"), archiveCaptor.capture());
        assertThat(archiveCaptor.getValue().getStatus()).isEqualTo(ArchiveStatus.PENDING.getCode());
    }

    @Test
    @DisplayName("批量状态转换应该返回成功数量")
    void batchTransitionShouldReturnSuccessCount() {
        // Given
        Archive archive1 = createArchiveWithStatus(ArchiveStatus.DRAFT);
        Archive archive2 = createArchiveWithStatus(ArchiveStatus.DRAFT);
        when(archiveReadService.getArchiveById("arc-001")).thenReturn(archive1);
        when(archiveReadService.getArchiveById("arc-002")).thenReturn(archive2);

        // When
        int count = stateTransitionService.batchTransitionStatus(
            java.util.List.of("arc-001", "arc-002"),
            ArchiveStatus.PENDING,
            "user-001"
        );

        // Then
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("canTransition 应该返回正确的结果")
    void canTransitionShouldReturnCorrectResult() {
        // Given
        Archive draftArchive = createArchiveWithStatus(ArchiveStatus.DRAFT);
        Archive archivedArchive = createArchiveWithStatus(ArchiveStatus.ARCHIVED);
        when(archiveReadService.getArchiveById("arc-001")).thenReturn(draftArchive);
        when(archiveReadService.getArchiveById("arc-002")).thenReturn(archivedArchive);

        // When & Then
        assertThat(stateTransitionService.canTransition("arc-001", ArchiveStatus.PENDING)).isTrue();
        assertThat(stateTransitionService.canTransition("arc-001", ArchiveStatus.ARCHIVED)).isFalse();
        assertThat(stateTransitionService.canTransition("arc-002", ArchiveStatus.DRAFT)).isFalse();
    }

    /**
     * 创建测试用 Archive 对象
     */
    private Archive createArchiveWithStatus(ArchiveStatus status) {
        Archive archive = new Archive();
        archive.setId("arc-001");
        archive.setArchiveCode("ARC-001");
        archive.setTitle("测试档案");
        archive.setFondsNo("F001");
        archive.setFiscalYear("2024");
        archive.setRetentionPeriod("10Y");
        archive.setOrgName("测试单位");
        archive.setAmount(new BigDecimal("1000.00"));
        // Archive 实体的 status 字段是 String 类型，需要转换
        archive.setStatus(status != null ? status.getCode() : null);
        archive.setVersion(0);
        return archive;
    }
}
