// Input: JUnit 5, Mockito, Java 标准库
// Output: DestructionApprovalServiceImplTest 测试用例类
// Pos: 后端测试用例
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nexusarchive.common.constants.OperationResult;
import com.nexusarchive.dto.ApprovalChain;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.entity.Destruction;
import com.nexusarchive.mapper.ArchiveMapper;
import com.nexusarchive.mapper.DestructionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 销毁审批服务实现类单元测试
 *
 * 测试范围:
 * 1. firstApproval - 初审流程
 * 2. secondApproval - 复审流程
 * 3. getApprovalChain - 获取审批链
 * 4. 状态流转逻辑
 * 5. 异常情况处理
 */
@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("销毁审批服务测试")
class DestructionApprovalServiceImplTest {

    @Mock
    private DestructionMapper destructionMapper;

    @Mock
    private ArchiveMapper archiveMapper;

    private ObjectMapper objectMapper;

    private DestructionApprovalServiceImpl destructionApprovalService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        destructionApprovalService = new DestructionApprovalServiceImpl(
                destructionMapper,
                archiveMapper,
                objectMapper
        );
    }

    private Destruction createTestDestruction(String id, String status, String archiveIdsJson) {
        Destruction destruction = new Destruction();
        destruction.setId(id);
        destruction.setStatus(status);
        destruction.setArchiveIds(archiveIdsJson);
        destruction.setApplicantId("applicant-1");
        destruction.setApplicantName("申请人");
        destruction.setReason("保管期限已到");
        destruction.setArchiveCount(1);
        return destruction;
    }

    private Archive createTestArchive(String id, String destructionStatus) {
        Archive archive = new Archive();
        archive.setId(id);
        archive.setArchiveCode("ARC-" + id);
        archive.setDestructionStatus(destructionStatus);
        return archive;
    }

    @Nested
    @DisplayName("初审流程测试")
    class FirstApprovalTests {

        @Test
        @DisplayName("应该成功完成初审通过")
        void shouldSuccessfullyCompleteFirstApproval() throws Exception {
            // Given
            String destructionId = "destruction-1";
            String approverId = "approver-1";
            String approverName = "初审人";
            String comment = "同意销毁";

            Destruction destruction = createTestDestruction(
                    destructionId,
                    OperationResult.PENDING,
                    "[\"archive-1\"]"
            );
            when(destructionMapper.selectById(destructionId)).thenReturn(destruction);



            // When
            destructionApprovalService.firstApproval(
                    destructionId, approverId, approverName, comment, true
            );

            // Then
            assertThat(destruction.getStatus()).isEqualTo("FIRST_APPROVED");
            assertThat(destruction.getApproverId()).isEqualTo(approverId);
            assertThat(destruction.getApproverName()).isEqualTo(approverName);
            assertThat(destruction.getApprovalComment()).isEqualTo(comment);
            assertThat(destruction.getApprovalSnapshot()).isNotNull();

            // 验证档案状态更新 - 修正为 eq(null) 和 UpdateWrapper
            verify(archiveMapper, times(1)).update(eq(null), any(com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper.class));

            verify(destructionMapper).updateById(destruction);
        }

        @Test
        @DisplayName("初审拒绝应该回退状态")
        void shouldRollbackStatusOnFirstApprovalReject() throws Exception {
            // Given
            String destructionId = "destruction-1";
            String approverId = "approver-1";
            String approverName = "初审人";
            String comment = "不符合销毁条件";

            Destruction destruction = createTestDestruction(
                    destructionId,
                    "APPRAISING",
                    "[\"archive-1\"]"
            );
            when(destructionMapper.selectById(destructionId)).thenReturn(destruction);

            Archive archive = createTestArchive("archive-1", "APPRAISING");
            when(archiveMapper.selectBatchIds(any())).thenReturn(List.of(archive));

            // When
            destructionApprovalService.firstApproval(
                    destructionId, approverId, approverName, comment, false
            );

            // Then
            assertThat(destruction.getStatus()).isEqualTo("APPRAISING");
            assertThat(destruction.getApproverId()).isEqualTo(approverId);
            assertThat(destruction.getApprovalComment()).isEqualTo(comment);

            // 验证审批链记录了拒绝
            ApprovalChain approvalChain = objectMapper.readValue(
                    destruction.getApprovalSnapshot(), ApprovalChain.class
            );
            assertThat(approvalChain.getFirstApproval()).isNotNull();
            assertThat(approvalChain.getFirstApproval().getApproved()).isFalse();
        }

        @Test
        @DisplayName("初审时销毁申请不存在应该抛出异常")
        void shouldThrowExceptionWhenDestructionNotFoundForFirstApproval() {
            // Given
            String destructionId = "nonexistent";
            when(destructionMapper.selectById(destructionId)).thenReturn(null);

            // When & Then
            assertThatThrownBy(() ->
                    destructionApprovalService.firstApproval(
                            destructionId, "approver-1", "初审人", "comment", true
                    )
            ).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("销毁申请不存在");
        }

        @Test
        @DisplayName("初审时状态不正确应该抛出异常")
        void shouldThrowExceptionWhenInvalidStatusForFirstApproval() {
            // Given
            String destructionId = "destruction-1";
            Destruction destruction = createTestDestruction(
                    destructionId,
                    "FIRST_APPROVED",
                    "[\"archive-1\"]"
            );
            when(destructionMapper.selectById(destructionId)).thenReturn(destruction);

            // When & Then
            assertThatThrownBy(() ->
                    destructionApprovalService.firstApproval(
                            destructionId, "approver-1", "初审人", "comment", true
                    )
            ).isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("无法进行初审");
        }

        @Test
        @DisplayName("应该正确保存审批链快照")
        void shouldCorrectlySaveApprovalSnapshot() throws Exception {
            // Given
            String destructionId = "destruction-1";
            Destruction destruction = createTestDestruction(
                    destructionId,
                    OperationResult.PENDING,
                    "[\"archive-1\"]"
            );
            when(destructionMapper.selectById(destructionId)).thenReturn(destruction);


            // When
            destructionApprovalService.firstApproval(
                    destructionId, "approver-1", "初审人", "同意", true
            );

            // Then
            ApprovalChain chain = objectMapper.readValue(
                    destruction.getApprovalSnapshot(), ApprovalChain.class
            );
            assertThat(chain.getFirstApproval()).isNotNull();
            assertThat(chain.getFirstApproval().getApproverId()).isEqualTo("approver-1");
            assertThat(chain.getFirstApproval().getApproverName()).isEqualTo("初审人");
            assertThat(chain.getFirstApproval().getComment()).isEqualTo("同意");
            assertThat(chain.getFirstApproval().getApproved()).isTrue();
            assertThat(chain.getFirstApproval().getTimestamp()).isNotNull();
        }

        @Test
        @DisplayName("应该支持从 APPRAISING 状态进行初审")
        void shouldAllowFirstApprovalFromAppraisingStatus() {
            // Given
            String destructionId = "destruction-1";
            Destruction destruction = createTestDestruction(
                    destructionId,
                    "APPRAISING",
                    "[\"archive-1\"]"
            );
            when(destructionMapper.selectById(destructionId)).thenReturn(destruction);


            // When
            destructionApprovalService.firstApproval(
                    destructionId, "approver-1", "初审人", "同意", true
            );

            // Then
            assertThat(destruction.getStatus()).isEqualTo("FIRST_APPROVED");
        }
    }

    @Nested
    @DisplayName("复审流程测试")
    class SecondApprovalTests {

        @Test
        @DisplayName("应该成功完成复审通过")
        void shouldSuccessfullyCompleteSecondApproval() throws Exception {
            // Given
            String destructionId = "destruction-1";
            String approverId = "approver-2";
            String approverName = "复审人";

            Destruction destruction = createTestDestruction(
                    destructionId,
                    "FIRST_APPROVED",
                    "[\"archive-1\"]"
            );

            // 设置已有初审记录
            ApprovalChain existingChain = new ApprovalChain();
            ApprovalChain.ApprovalInfo firstApproval = new ApprovalChain.ApprovalInfo();
            firstApproval.setApproverId("approver-1");
            firstApproval.setApproverName("初审人");
            firstApproval.setApproved(true);
            firstApproval.setTimestamp(LocalDateTime.now());
            existingChain.setFirstApproval(firstApproval);
            destruction.setApprovalSnapshot(objectMapper.writeValueAsString(existingChain));

            when(destructionMapper.selectById(destructionId)).thenReturn(destruction);



            // When
            destructionApprovalService.secondApproval(
                    destructionId, approverId, approverName, "复核通过", true
            );

            // Then
            assertThat(destruction.getStatus()).isEqualTo("DESTRUCTION_APPROVED");
            assertThat(destruction.getApproverId()).isEqualTo(approverId);
            assertThat(destruction.getApproverName()).isEqualTo(approverName);

            ApprovalChain chain = objectMapper.readValue(
                    destruction.getApprovalSnapshot(), ApprovalChain.class
            );
            assertThat(chain.getSecondApproval()).isNotNull();
            assertThat(chain.getSecondApproval().getApproved()).isTrue();
        }

        @Test
        @DisplayName("复审拒绝应该回退状态")
        void shouldRollbackStatusOnSecondApprovalReject() throws Exception {
            // Given
            String destructionId = "destruction-1";
            Destruction destruction = createTestDestruction(
                    destructionId,
                    "FIRST_APPROVED",
                    "[\"archive-1\"]"
            );

            ApprovalChain existingChain = new ApprovalChain();
            ApprovalChain.ApprovalInfo firstApproval = new ApprovalChain.ApprovalInfo();
            firstApproval.setApproved(true);
            firstApproval.setTimestamp(LocalDateTime.now());
            existingChain.setFirstApproval(firstApproval);
            try {
                destruction.setApprovalSnapshot(objectMapper.writeValueAsString(existingChain));
            } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                throw new RuntimeException(e);
            }

            when(destructionMapper.selectById(destructionId)).thenReturn(destruction);


            // When
            destructionApprovalService.secondApproval(
                    destructionId, "approver-2", "复审人", "不符合条件", false
            );

            // Then
            assertThat(destruction.getStatus()).isEqualTo("APPRAISING");

            ApprovalChain chain = objectMapper.readValue(
                    destruction.getApprovalSnapshot(), ApprovalChain.class
            );
            assertThat(chain.getSecondApproval().getApproved()).isFalse();
        }

        @Test
        @DisplayName("复审时销毁申请不存在应该抛出异常")
        void shouldThrowExceptionWhenDestructionNotFoundForSecondApproval() {
            // Given
            String destructionId = "nonexistent";
            when(destructionMapper.selectById(destructionId)).thenReturn(null);

            // When & Then
            assertThatThrownBy(() ->
                    destructionApprovalService.secondApproval(
                            destructionId, "approver-2", "复审人", "comment", true
                    )
            ).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("销毁申请不存在");
        }

        @Test
        @DisplayName("复审时状态不是 FIRST_APPROVED 应该抛出异常")
        void shouldThrowExceptionWhenNotFirstApprovedForSecondApproval() {
            // Given
            String destructionId = "destruction-1";
            Destruction destruction = createTestDestruction(
                    destructionId,
                    OperationResult.PENDING,
                    "[\"archive-1\"]"
            );
            when(destructionMapper.selectById(destructionId)).thenReturn(destruction);

            // When & Then
            assertThatThrownBy(() ->
                    destructionApprovalService.secondApproval(
                            destructionId, "approver-2", "复审人", "comment", true
                    )
            ).isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("无法进行复核");
        }

        @Test
        @DisplayName("初审未通过时应该禁止复审")
        void shouldRejectSecondApprovalWhenFirstNotApproved() throws Exception {
            // Given
            String destructionId = "destruction-1";
            Destruction destruction = createTestDestruction(
                    destructionId,
                    "FIRST_APPROVED",
                    "[\"archive-1\"]"
            );

            ApprovalChain existingChain = new ApprovalChain();
            ApprovalChain.ApprovalInfo firstApproval = new ApprovalChain.ApprovalInfo();
            firstApproval.setApproved(false); // 初审未通过
            firstApproval.setTimestamp(LocalDateTime.now());
            existingChain.setFirstApproval(firstApproval);
            destruction.setApprovalSnapshot(objectMapper.writeValueAsString(existingChain));

            when(destructionMapper.selectById(destructionId)).thenReturn(destruction);

            // When & Then
            assertThatThrownBy(() ->
                    destructionApprovalService.secondApproval(
                            destructionId, "approver-2", "复审人", "comment", true
                    )
            ).isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("初审未通过");
        }

        @Test
        @DisplayName("复审通过应该更新档案状态为 DESTRUCTION_APPROVED")
        void shouldUpdateArchiveStatusToDestructionApprovedOnSecondApprovalPass() {
            // Given
            String destructionId = "destruction-1";
            Destruction destruction = createTestDestruction(
                    destructionId,
                    "FIRST_APPROVED",
                    "[\"archive-1\"]"
            );

            ApprovalChain existingChain = new ApprovalChain();
            ApprovalChain.ApprovalInfo firstApproval = new ApprovalChain.ApprovalInfo();
            firstApproval.setApproved(true);
            firstApproval.setTimestamp(LocalDateTime.now());
            existingChain.setFirstApproval(firstApproval);
            try {
                destruction.setApprovalSnapshot(objectMapper.writeValueAsString(existingChain));
            } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                throw new RuntimeException(e);
            }

            when(destructionMapper.selectById(destructionId)).thenReturn(destruction);


            // When
            destructionApprovalService.secondApproval(
                    destructionId, "approver-2", "复审人", "同意", true
            );

            // Then
            verify(archiveMapper, times(1)).update(eq(null), any(com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper.class)); // 验证档案状态被更新
        }
    }

    @Nested
    @DisplayName("审批链查询测试")
    class ApprovalChainTests {

        @Test
        @DisplayName("应该正确获取审批链")
        void shouldCorrectlyGetApprovalChain() throws Exception {
            // Given
            String destructionId = "destruction-1";
            Destruction destruction = createTestDestruction(
                    destructionId,
                    "FIRST_APPROVED",
                    "[\"archive-1\"]"
            );

            ApprovalChain chain = new ApprovalChain();
            ApprovalChain.ApprovalInfo firstApproval = new ApprovalChain.ApprovalInfo();
            firstApproval.setApproverId("approver-1");
            firstApproval.setApproverName("初审人");
            firstApproval.setApproved(true);
            firstApproval.setTimestamp(LocalDateTime.now());
            chain.setFirstApproval(firstApproval);
            destruction.setApprovalSnapshot(objectMapper.writeValueAsString(chain));

            when(destructionMapper.selectById(destructionId)).thenReturn(destruction);

            // When
            ApprovalChain result = destructionApprovalService.getApprovalChain(destructionId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getFirstApproval()).isNotNull();
            assertThat(result.getFirstApproval().getApproverId()).isEqualTo("approver-1");
            assertThat(result.getFirstApproval().getApproverName()).isEqualTo("初审人");
            assertThat(result.getSecondApproval()).isNull();
        }

        @Test
        @DisplayName("销毁申请不存在时获取审批链应该抛出异常")
        void shouldThrowExceptionWhenDestructionNotFoundForGettingChain() {
            // Given
            String destructionId = "nonexistent";
            when(destructionMapper.selectById(destructionId)).thenReturn(null);

            // When & Then
            assertThatThrownBy(() ->
                    destructionApprovalService.getApprovalChain(destructionId)
            ).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("销毁申请不存在");
        }

        @Test
        @DisplayName("无审批记录时应该返回空的审批链")
        void shouldReturnEmptyChainWhenNoApprovalExists() {
            // Given
            String destructionId = "destruction-1";
            Destruction destruction = createTestDestruction(
                    destructionId,
                    OperationResult.PENDING,
                    "[\"archive-1\"]"
            );
            destruction.setApprovalSnapshot(null);
            when(destructionMapper.selectById(destructionId)).thenReturn(destruction);

            // When
            ApprovalChain result = destructionApprovalService.getApprovalChain(destructionId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getFirstApproval()).isNull();
            assertThat(result.getSecondApproval()).isNull();
        }

        @Test
        @DisplayName("应该解析损坏的审批链快照时返回新链")
        void shouldReturnNewChainWhenSnapshotIsCorrupted() {
            // Given
            String destructionId = "destruction-1";
            Destruction destruction = createTestDestruction(
                    destructionId,
                    OperationResult.PENDING,
                    "[\"archive-1\"]"
            );
            destruction.setApprovalSnapshot("invalid-json");
            when(destructionMapper.selectById(destructionId)).thenReturn(destruction);

            // When
            ApprovalChain result = destructionApprovalService.getApprovalChain(destructionId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getFirstApproval()).isNull();
            assertThat(result.getSecondApproval()).isNull();
        }
    }

    @Nested
    @DisplayName("档案状态更新测试")
    class ArchiveStatusUpdateTests {

        @Test
        @DisplayName("初审通过应该更新档案状态为 APPRAISING")
        void shouldUpdateArchiveStatusToAppraisingOnFirstApprovalPass() {
            // Given
            String destructionId = "destruction-1";
            Destruction destruction = createTestDestruction(
                    destructionId,
                    OperationResult.PENDING,
                    "[\"archive-1\", \"archive-2\"]"
            );
            when(destructionMapper.selectById(destructionId)).thenReturn(destruction);



            // When
            destructionApprovalService.firstApproval(
                    destructionId, "approver-1", "初审人", "同意", true
            );

            // Then
            verify(archiveMapper, times(2)).update(eq(null), any(com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper.class));
        }

        @Test
        @DisplayName("复审通过应该更新档案状态为 DESTRUCTION_APPROVED")
        void shouldUpdateArchiveStatusToDestructionApprovedOnSecondApprovalPass() throws Exception {
            // Given
            String destructionId = "destruction-1";
            Destruction destruction = createTestDestruction(
                    destructionId,
                    "FIRST_APPROVED",
                    "[\"archive-1\"]"
            );

            ApprovalChain existingChain = new ApprovalChain();
            ApprovalChain.ApprovalInfo firstApproval = new ApprovalChain.ApprovalInfo();
            firstApproval.setApproved(true);
            firstApproval.setTimestamp(LocalDateTime.now());
            existingChain.setFirstApproval(firstApproval);
            try {
                destruction.setApprovalSnapshot(objectMapper.writeValueAsString(existingChain));
            } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                throw new RuntimeException(e);
            }

            when(destructionMapper.selectById(destructionId)).thenReturn(destruction);



            // When
            destructionApprovalService.secondApproval(
                    destructionId, "approver-2", "复审人", "同意", true
            );

            // Then
            verify(archiveMapper, times(1)).update(eq(null), any(com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper.class));
        }

        @Test
        @DisplayName("应该根据档案状态决定回退状态")
        void shouldDetermineRollbackStatusBasedOnArchiveStatus() {
            // Given
            String destructionId = "destruction-1";
            Destruction destruction = createTestDestruction(
                    destructionId,
                    "APPRAISING",
                    "[\"archive-1\"]"
            );
            when(destructionMapper.selectById(destructionId)).thenReturn(destruction);

            Archive archive = createTestArchive("archive-1", "APPRAISING");
            when(archiveMapper.selectBatchIds(any())).thenReturn(List.of(archive));

            // When
            destructionApprovalService.firstApproval(
                    destructionId, "approver-1", "初审人", "拒绝", false
            );

            // Then
            assertThat(destruction.getStatus()).isEqualTo("APPRAISING");
        }

        @Test
        @DisplayName("档案不是 APPRAISING 状态应该回退到 EXPIRED")
        void shouldRollbackToExpiredWhenArchiveNotAppraising() {
            // Given
            String destructionId = "destruction-1";
            Destruction destruction = createTestDestruction(
                    destructionId,
                    "APPRAISING",
                    "[\"archive-1\"]"
            );
            when(destructionMapper.selectById(destructionId)).thenReturn(destruction);

            Archive archive = createTestArchive("archive-1", "SOME_OTHER_STATUS");
            when(archiveMapper.selectBatchIds(any())).thenReturn(List.of(archive));

            // When
            destructionApprovalService.firstApproval(
                    destructionId, "approver-1", "初审人", "拒绝", false
            );

            // Then
            assertThat(destruction.getStatus()).isEqualTo("EXPIRED");
        }
    }
}
