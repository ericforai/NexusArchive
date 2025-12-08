package com.nexusarchive.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.entity.Destruction;
import com.nexusarchive.mapper.ArchiveMapper;
import com.nexusarchive.mapper.DestructionMapper;
import com.nexusarchive.service.impl.DestructionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * DestructionService 单元测试
 * 
 * 测试覆盖:
 * - 销毁申请创建
 * - 销毁审批
 * - 销毁执行
 * - 销毁保留（Destruction Hold）验证
 * 
 * @author Agent E - 质量保障工程师
 */
@ExtendWith(MockitoExtension.class)
class DestructionServiceTest {

    @Mock
    private DestructionMapper destructionMapper;

    @Mock
    private ArchiveMapper archiveMapper;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private DestructionServiceImpl destructionService;

    private Destruction validDestruction;
    private Archive testArchive1;
    private Archive testArchive2;

    @BeforeEach
    void setUp() throws JsonProcessingException {
        // 创建测试档案
        testArchive1 = new Archive();
        testArchive1.setId("arc-001");
        testArchive1.setArchiveCode("ARC-2023-001");
        testArchive1.setDestructionHold(false);

        testArchive2 = new Archive();
        testArchive2.setId("arc-002");
        testArchive2.setArchiveCode("ARC-2023-002");
        testArchive2.setDestructionHold(false);

        // 创建有效的销毁申请
        validDestruction = new Destruction();
        validDestruction.setApplicantId("user-001");
        validDestruction.setApplicantName("张三");
        validDestruction.setReason("保管期限已满");
        validDestruction.setArchiveCount(2);
        validDestruction.setArchiveIds(objectMapper.writeValueAsString(Arrays.asList("arc-001", "arc-002")));
    }

    // ========== 创建销毁申请测试 ==========

    @Nested
    @DisplayName("创建销毁申请")
    class CreateDestructionTests {

        @Test
        @DisplayName("正常创建销毁申请 - 成功")
        void createDestruction_ValidRequest_Success() {
            // Arrange
            when(archiveMapper.selectBatchIds(anyList())).thenReturn(Arrays.asList(testArchive1, testArchive2));
            when(destructionMapper.insert(any(Destruction.class))).thenReturn(1);

            // Act
            Destruction result = destructionService.createDestruction(validDestruction);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo("PENDING");

            verify(destructionMapper).insert(any(Destruction.class));
        }

        @Test
        @DisplayName("档案处于销毁保留状态 - 抛出异常")
        void createDestruction_ArchiveUnderHold_ThrowsException() {
            // Arrange
            testArchive1.setDestructionHold(true);
            testArchive1.setHoldReason("诉讼相关");
            
            when(archiveMapper.selectBatchIds(anyList())).thenReturn(Arrays.asList(testArchive1, testArchive2));

            // Act & Assert
            assertThatThrownBy(() -> destructionService.createDestruction(validDestruction))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Destruction Hold")
                    .hasMessageContaining("诉讼相关");
        }

        @Test
        @DisplayName("无效的档案ID JSON - 抛出异常")
        void createDestruction_InvalidArchiveIdsJson_ThrowsException() {
            // Arrange
            validDestruction.setArchiveIds("invalid-json");

            // Act & Assert
            assertThatThrownBy(() -> destructionService.createDestruction(validDestruction))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to validate destruction eligibility");
        }
    }

    // ========== 销毁审批测试 ==========

    @Nested
    @DisplayName("销毁审批")
    class ApproveDestructionTests {

        @Test
        @DisplayName("审批通过 - 成功")
        void approveDestruction_Success() {
            // Arrange
            Destruction pendingDestruction = createDestructionWithStatus("PENDING");
            when(destructionMapper.selectById("des-001")).thenReturn(pendingDestruction);
            when(destructionMapper.updateById(any(Destruction.class))).thenReturn(1);

            // Act
            destructionService.approveDestruction("des-001", "approver-001", "同意销毁");

            // Assert
            ArgumentCaptor<Destruction> captor = ArgumentCaptor.forClass(Destruction.class);
            verify(destructionMapper).updateById(captor.capture());

            Destruction updated = captor.getValue();
            assertThat(updated.getStatus()).isEqualTo("APPROVED");
            assertThat(updated.getApproverId()).isEqualTo("approver-001");
            assertThat(updated.getApprovalComment()).isEqualTo("同意销毁");
            assertThat(updated.getApprovalTime()).isNotNull();
        }

        @Test
        @DisplayName("销毁记录不存在 - 抛出异常")
        void approveDestruction_NotFound_ThrowsException() {
            // Arrange
            when(destructionMapper.selectById("non-existent")).thenReturn(null);

            // Act & Assert
            assertThatThrownBy(() -> destructionService.approveDestruction("non-existent", "approver-001", ""))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("not found");
        }
    }

    // ========== 执行销毁测试 ==========

    @Nested
    @DisplayName("执行销毁")
    class ExecuteDestructionTests {

        @Test
        @DisplayName("执行销毁 - 成功")
        void executeDestruction_Success() throws JsonProcessingException {
            // Arrange
            Destruction approvedDestruction = createDestructionWithStatus("APPROVED");
            approvedDestruction.setArchiveIds(objectMapper.writeValueAsString(Arrays.asList("arc-001", "arc-002")));
            
            when(destructionMapper.selectById("des-001")).thenReturn(approvedDestruction);
            when(destructionMapper.updateById(any(Destruction.class))).thenReturn(1);
            when(archiveMapper.deleteById(anyString())).thenReturn(1);

            // Act
            destructionService.executeDestruction("des-001");

            // Assert
            // 验证档案被逻辑删除
            verify(archiveMapper, times(2)).deleteById(anyString());
            
            // 验证销毁记录状态更新
            ArgumentCaptor<Destruction> captor = ArgumentCaptor.forClass(Destruction.class);
            verify(destructionMapper).updateById(captor.capture());

            Destruction updated = captor.getValue();
            assertThat(updated.getStatus()).isEqualTo("EXECUTED");
            assertThat(updated.getExecutionTime()).isNotNull();
        }

        @Test
        @DisplayName("销毁记录不存在 - 抛出异常")
        void executeDestruction_NotFound_ThrowsException() {
            // Arrange
            when(destructionMapper.selectById("non-existent")).thenReturn(null);

            // Act & Assert
            assertThatThrownBy(() -> destructionService.executeDestruction("non-existent"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("not found");
        }

        @Test
        @DisplayName("未审批状态无法执行 - 抛出异常")
        void executeDestruction_NotApproved_ThrowsException() {
            // Arrange
            Destruction pendingDestruction = createDestructionWithStatus("PENDING");
            when(destructionMapper.selectById("des-001")).thenReturn(pendingDestruction);

            // Act & Assert
            assertThatThrownBy(() -> destructionService.executeDestruction("des-001"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Only approved destruction requests can be executed");
        }

        @Test
        @DisplayName("已执行状态无法再次执行 - 抛出异常")
        void executeDestruction_AlreadyExecuted_ThrowsException() {
            // Arrange
            Destruction executedDestruction = createDestructionWithStatus("EXECUTED");
            when(destructionMapper.selectById("des-001")).thenReturn(executedDestruction);

            // Act & Assert
            assertThatThrownBy(() -> destructionService.executeDestruction("des-001"))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    // ========== 查询销毁列表测试 ==========

    @Nested
    @DisplayName("查询销毁列表")
    class GetDestructionsTests {

        @Test
        @DisplayName("带状态筛选的分页查询")
        void getDestructions_WithStatus_Success() {
            // Arrange
            Page<Destruction> mockPage = new Page<>(1, 10);
            mockPage.setTotal(1);
            
            when(destructionMapper.selectPage(any(Page.class), any())).thenReturn(mockPage);

            // Act
            Page<Destruction> result = destructionService.getDestructions(1, 10, "PENDING");

            // Assert
            assertThat(result).isNotNull();
            verify(destructionMapper).selectPage(any(Page.class), any());
        }

        @Test
        @DisplayName("无状态筛选的分页查询")
        void getDestructions_WithoutStatus_Success() {
            // Arrange
            Page<Destruction> mockPage = new Page<>(1, 10);
            when(destructionMapper.selectPage(any(Page.class), any())).thenReturn(mockPage);

            // Act
            Page<Destruction> result = destructionService.getDestructions(1, 10, null);

            // Assert
            assertThat(result).isNotNull();
        }
    }

    // ========== 辅助方法 ==========

    private Destruction createDestructionWithStatus(String status) {
        Destruction destruction = new Destruction();
        destruction.setId("des-001");
        destruction.setApplicantId("user-001");
        destruction.setApplicantName("张三");
        destruction.setReason("保管期限已满");
        destruction.setArchiveCount(2);
        destruction.setStatus(status);
        destruction.setCreatedTime(LocalDateTime.now());
        return destruction;
    }
}
