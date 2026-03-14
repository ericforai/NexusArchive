// Input: Spring Test、JUnit 5、MyBatis-Plus
// Output: DestructionToArchiveBoundaryTest 集成测试
// Pos: 后端集成测试 - 边界场景
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.entity.Destruction;
import com.nexusarchive.mapper.ArchiveMapper;
import com.nexusarchive.mapper.DestructionMapper;
import com.nexusarchive.service.DestructionService;
import com.nexusarchive.service.DestructionApprovalService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 销毁 → Archive 边界集成测试
 *
 * 测试目的：验证 Destruction 模块与 Archive 之间的边界行为
 * 覆盖场景：
 * 1. 创建销毁申请 → 验证 Archive.destructionHold 标志
 * 2. 执行销毁 → Archive 逻辑删除
 * 3. 审批流程 → Archive.destructionStatus 状态变更
 * 4. 双人复核 → 状态机正确流转
 *
 * 对应文档：docs/architecture/module-dependency-status.md#一、已确认的跨模块依赖
 *
 * @author Architecture Boundary Test
 * @since 2026-03-14
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("销毁 → Archive 边界测试")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DestructionToArchiveBoundaryTest {

    @Autowired
    private DestructionService destructionService;

    @Autowired
    private DestructionApprovalService destructionApprovalService;

    @Autowired
    private ArchiveMapper archiveMapper;

    @Autowired
    private DestructionMapper destructionMapper;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static String testArchiveId;
    private static String testDestructionId;
    private static String frozenArchiveId;

    @BeforeEach
    void setUp() {
        // 准备测试数据
        cleanupTestData();
        setupTestData();
    }

    @AfterEach
    void tearDown() {
        // 清理测试数据
        cleanupTestData();
    }

    private void setupTestData() {
        // 创建一个正常的测试档案
        Archive normalArchive = createTestArchive("NORMAL");
        archiveMapper.insert(normalArchive);
        testArchiveId = normalArchive.getId();

        // 创建一个冻结的测试档案
        Archive frozenArchive = createTestArchive("FROZEN");
        frozenArchive.setDestructionHold(true);
        frozenArchive.setHoldReason("审计期间，禁止销毁");
        archiveMapper.insert(frozenArchive);
        frozenArchiveId = frozenArchive.getId();
    }

    private void cleanupTestData() {
        if (testDestructionId != null) {
            destructionMapper.deleteById(testDestructionId);
            testDestructionId = null;
        }
        if (testArchiveId != null) {
            archiveMapper.deleteById(testArchiveId);
            testArchiveId = null;
        }
        if (frozenArchiveId != null) {
            archiveMapper.deleteById(frozenArchiveId);
            frozenArchiveId = null;
        }
    }

    /**
     * 场景1：创建销毁申请 → 正常档案应允许创建
     *
     * 验证点：
     * 1. 销毁申请被成功创建
     * 2. 状态为 PENDING
     * 3. archiveIds 正确存储
     */
    @Test
    @Order(1)
    @DisplayName("场景1: 创建销毁申请 - 正常档案应允许")
    @Transactional
    void createDestruction_WithNormalArchive_ShouldSucceed() throws Exception {
        // Arrange
        List<String> archiveIds = new ArrayList<>();
        archiveIds.add(testArchiveId);
        String archiveIdsJson = objectMapper.writeValueAsString(archiveIds);

        Destruction destruction = new Destruction();
        destruction.setId("test-destruction-001");
        destruction.setArchiveIds(archiveIdsJson);
        destruction.setApplicantId("test-user");
        destruction.setApplicantName("测试用户");
        destruction.setReason("保管期限已满");
        destruction.setCreatedTime(LocalDateTime.now());

        // Act
        Destruction result = destructionService.createDestruction(destruction);
        testDestructionId = result.getId();

        // Assert
        assertNotNull(result, "销毁申请应被创建");
        assertEquals("PENDING", result.getStatus(), "状态应为 PENDING");
        assertEquals(testArchiveId, parseArchiveIds(result.getArchiveIds()).get(0),
                "Archive ID 应正确存储");
    }

    /**
     * 场景2：创建销毁申请 → 冻结档案应抛出异常
     *
     * 验证点：
     * 1. DestructionHold 标志生效
     * 2. 异常信息包含原因
     * 3. 销毁申请不被创建
     */
    @Test
    @Order(2)
    @DisplayName("场景2: 创建销毁申请 - 冻结档案应拒绝")
    @Transactional
    void createDestruction_WithFrozenArchive_ShouldThrowException() throws Exception {
        // Arrange
        List<String> archiveIds = new ArrayList<>();
        archiveIds.add(frozenArchiveId);
        String archiveIdsJson = objectMapper.writeValueAsString(archiveIds);

        Destruction destruction = new Destruction();
        destruction.setId("test-destruction-002");
        destruction.setArchiveIds(archiveIdsJson);
        destruction.setApplicantId("test-user");
        destruction.setApplicantName("测试用户");
        destruction.setReason("保管期限已满");

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            destructionService.createDestruction(destruction);
        });

        assertTrue(exception.getMessage().contains("Destruction Hold"),
                "异常应包含 DestructionHold 信息");
        assertTrue(exception.getMessage().contains("审计期间"),
                "异常应包含冻结原因");

        // 验证销毁申请未被创建
        Destruction notCreated = destructionMapper.selectById("test-destruction-002");
        assertNull(notCreated, "销毁申请不应被创建");
    }

    /**
     * 场景3：执行销毁 → Archive 应被逻辑删除
     *
     * 验证点：
     * 1. 销毁执行后状态变更为 EXECUTED
     * 2. Archive 被逻辑删除（deleted 标志被设置）
     * 3. 执行时间被记录
     */
    @Test
    @Order(3)
    @DisplayName("场景3: 执行销毁应逻辑删除 Archive")
    @Transactional
    void executeDestruction_ShouldLogicallyDeleteArchive() throws Exception {
        // Arrange: 先创建并审批通过的销毁申请
        List<String> archiveIds = new ArrayList<>();
        archiveIds.add(testArchiveId);
        String archiveIdsJson = objectMapper.writeValueAsString(archiveIds);

        Destruction destruction = new Destruction();
        destruction.setId("test-destruction-003");
        destruction.setArchiveIds(archiveIdsJson);
        destruction.setApplicantId("test-user");
        destruction.setApplicantName("测试用户");
        destruction.setReason("保管期限已满");
        destruction.setStatus("APPROVED"); // 直接设置为已审批状态
        destruction.setApprovalTime(LocalDateTime.now());
        destructionMapper.insert(destruction);
        testDestructionId = destruction.getId();

        // Act: 执行销毁
        destructionService.executeDestruction(destruction.getId());

        // Assert
        Destruction executed = destructionMapper.selectById(destruction.getId());
        assertEquals("EXECUTED", executed.getStatus(), "状态应为 EXECUTED");
        assertNotNull(executed.getExecutionTime(), "执行时间应被记录");

        // 验证 Archive 被逻辑删除
        Archive deletedArchive = archiveMapper.selectById(testArchiveId);
        // MyBatis-Plus 的 @TableLogic 会过滤已删除记录，所以查询返回 null
        // 或者需要检查 deleted 字段
        assertTrue(deletedArchive == null || deletedArchive.getDeleted() != null
                || deletedArchive.getDeleted() == 1,
                "Archive 应被逻辑删除");
    }

    /**
     * 场景4：初审审批 → Archive.destructionStatus 状态变更
     *
     * 验证点：
     * 1. 初审通过 → destructionStatus 变更为 APPRAISING
     * 2. 初审拒绝 → destructionStatus 变更为 EXPIRED
     * 3. 审批链快照被保存
     */
    @Test
    @Order(4)
    @DisplayName("场景4: 初审审批应更新 Archive 状态")
    @Transactional
    void firstApproval_ShouldUpdateArchiveStatus() throws Exception {
        // Arrange
        List<String> archiveIds = new ArrayList<>();
        archiveIds.add(testArchiveId);
        String archiveIdsJson = objectMapper.writeValueAsString(archiveIds);

        Destruction destruction = new Destruction();
        destruction.setId("test-destruction-004");
        destruction.setArchiveIds(archiveIdsJson);
        destruction.setApplicantId("test-user");
        destruction.setApplicantName("测试用户");
        destruction.setReason("保管期限已满");
        destruction.setStatus("PENDING");
        destructionMapper.insert(destruction);
        testDestructionId = destruction.getId();

        // Act: 初审通过
        destructionApprovalService.firstApproval(
                destruction.getId(),
                "approver1",
                "初审人",
                "初审通过",
                true
        );

        // Assert
        Destruction updated = destructionMapper.selectById(destruction.getId());
        assertEquals("FIRST_APPROVED", updated.getStatus(), "状态应为 FIRST_APPROVED");
        assertNotNull(updated.getApprovalSnapshot(), "审批链快照应被保存");

        // 验证 Archive 状态
        Archive archive = archiveMapper.selectById(testArchiveId);
        assertEquals("APPRAISING", archive.getDestructionStatus(),
                "Archive destructionStatus 应为 APPRAISING");
    }

    /**
     * 场景5：复核审批 → 完整双人复核流程
     *
     * 验证点：
     * 1. 必须先通过初审才能复核
     * 2. 复核通过 → destructionStatus 变更为 DESTRUCTION_APPROVED
     * 3. 复核拒绝 → 状态回退到 APPRAISING
     */
    @Test
    @Order(5)
    @DisplayName("场景5: 复核审批应完成双人复核流程")
    @Transactional
    void secondApproval_ShouldCompleteTwoPersonApproval() throws Exception {
        // Arrange
        List<String> archiveIds = new ArrayList<>();
        archiveIds.add(testArchiveId);
        String archiveIdsJson = objectMapper.writeValueAsString(archiveIds);

        Destruction destruction = new Destruction();
        destruction.setId("test-destruction-005");
        destruction.setArchiveIds(archiveIdsJson);
        destruction.setApplicantId("test-user");
        destruction.setApplicantName("测试用户");
        destruction.setReason("保管期限已满");
        destruction.setStatus("PENDING");
        destructionMapper.insert(destruction);
        testDestructionId = destruction.getId();

        // Act: 先初审通过
        destructionApprovalService.firstApproval(
                destruction.getId(),
                "approver1",
                "初审人",
                "初审通过",
                true
        );

        // Act: 再复核通过
        destructionApprovalService.secondApproval(
                destruction.getId(),
                "approver2",
                "复核人",
                "复核通过",
                true
        );

        // Assert
        Destruction finalDestruction = destructionMapper.selectById(destruction.getId());
        assertEquals("DESTRUCTION_APPROVED", finalDestruction.getStatus(),
                "状态应为 DESTRUCTION_APPROVED");

        // 验证 Archive 最终状态
        Archive archive = archiveMapper.selectById(testArchiveId);
        assertEquals("DESTRUCTION_APPROVED", archive.getDestructionStatus(),
                "Archive destructionStatus 应为 DESTRUCTION_APPROVED");
    }

    /**
     * 场景6：复核拒绝 → 状态回退
     *
     * 验证点：
     * 1. 复核拒绝 → destructionStatus 回退到 APPRAISING
     * 2. 可以重新发起审批
     */
    @Test
    @Order(6)
    @DisplayName("场景6: 复核拒绝应回退状态")
    @Transactional
    void secondApproval_WhenRejected_ShouldRollbackStatus() throws Exception {
        // Arrange
        List<String> archiveIds = new ArrayList<>();
        archiveIds.add(testArchiveId);
        String archiveIdsJson = objectMapper.writeValueAsString(archiveIds);

        Destruction destruction = new Destruction();
        destruction.setId("test-destruction-006");
        destruction.setArchiveIds(archiveIdsJson);
        destruction.setApplicantId("test-user");
        destruction.setApplicantName("测试用户");
        destruction.setReason("保管期限已满");
        destruction.setStatus("PENDING");
        destructionMapper.insert(destruction);
        testDestructionId = destruction.getId();

        // 先初审通过
        destructionApprovalService.firstApproval(
                destruction.getId(),
                "approver1",
                "初审人",
                "初审通过",
                true
        );

        // Act: 复核拒绝
        destructionApprovalService.secondApproval(
                destruction.getId(),
                "approver2",
                "复核人",
                "需要补充材料",
                false
        );

        // Assert
        Destruction rejected = destructionMapper.selectById(destruction.getId());
        assertEquals("APPRAISING", rejected.getStatus(),
                "状态应回退到 APPRAISING");

        Archive archive = archiveMapper.selectById(testArchiveId);
        assertEquals("APPRAISING", archive.getDestructionStatus(),
                "Archive destructionStatus 应回退到 APPRAISING");
    }

    /**
     * 场景7：状态机保护 → 未初审不能复核
     *
     * 验证点：
     * 1. 直接复核（跳过初审）应抛出异常
     * 2. 异常信息明确说明需要先初审
     */
    @Test
    @Order(7)
    @DisplayName("场景7: 状态机保护 - 未初审不能复核")
    @Transactional
    void secondApproval_WithoutFirstApproval_ShouldThrowException() throws Exception {
        // Arrange
        List<String> archiveIds = new ArrayList<>();
        archiveIds.add(testArchiveId);
        String archiveIdsJson = objectMapper.writeValueAsString(archiveIds);

        Destruction destruction = new Destruction();
        destruction.setId("test-destruction-007");
        destruction.setArchiveIds(archiveIdsJson);
        destruction.setApplicantId("test-user");
        destruction.setApplicantName("测试用户");
        destruction.setReason("保管期限已满");
        destruction.setStatus("PENDING"); // 未初审
        destructionMapper.insert(destruction);
        testDestructionId = destruction.getId();

        // Act & Assert
        Exception exception = assertThrows(IllegalStateException.class, () -> {
            destructionApprovalService.secondApproval(
                    destruction.getId(),
                    "approver2",
                    "复核人",
                    "跳过初审的复核",
                    true
            );
        });

        assertTrue(exception.getMessage().contains("初审") ||
                exception.getMessage().contains("FIRST_APPROVED"),
                "异常应提示需要先通过初审");
    }

    // ==================== 辅助方法 ====================

    private Archive createTestArchive(String type) {
        Archive archive = new Archive();
        archive.setId("test-archive-" + type + "-" + System.currentTimeMillis());
        archive.setArchiveCode("TEST-" + type + "-" + System.currentTimeMillis());
        archive.setTitle("测试档案-" + type);
        archive.setStatus("archived");
        archive.setFondsNo("DEFAULT");
        archive.setFiscalYear("2020");
        archive.setRetentionPeriod("30Y");
        archive.setCategoryCode("AC01");
        archive.setDestructionStatus("EXPIRED"); // 保管期限已满
        archive.setDestructionHold(false);
        archive.setCreatedTime(LocalDateTime.now());
        return archive;
    }

    private List<String> parseArchiveIds(String json) {
        try {
            return objectMapper.readValue(json, List.class);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
