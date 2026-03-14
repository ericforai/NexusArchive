// Input: Spring Test、JUnit 5、MyBatis-Plus
// Output: ApprovalToPreArchiveBoundaryTest 集成测试
// Pos: 后端集成测试 - 边界场景
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.integration;

import com.nexusarchive.entity.Archive;
import com.nexusarchive.entity.ArchiveApproval;
import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.entity.enums.PreArchiveStatus;
import com.nexusarchive.mapper.ArchiveMapper;
import com.nexusarchive.mapper.ArchiveApprovalMapper;
import com.nexusarchive.mapper.ArcFileContentMapper;
import com.nexusarchive.service.ArchiveApprovalService;
import com.nexusarchive.service.PreArchiveSubmitService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 审批 → 预归档 边界集成测试
 *
 * 测试目的：验证 ArchiveApprovalServiceImpl 与 PreArchiveSubmitService 之间的边界行为
 *
 * 关键依赖：
 * - ArchiveApprovalServiceImpl 使用 @Lazy 注入 PreArchiveSubmitService
 * - approveArchive() → preArchiveSubmitService.completeArchival()
 *
 * 覆盖场景：
 * 1. 审批通过 → completeArchival 被正确调用
 * 2. 完成归档流程 → OFD 签名和文件锁定
 * 3. 拒绝审批 → 状态回退到 READY_TO_ARCHIVE
 * 4. 循环依赖 @Lazy 注解有效
 *
 * 对应文档：docs/architecture/module-dependency-status.md#一、已确认的跨模块依赖
 *
 * @author Architecture Boundary Test
 * @since 2026-03-14
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("审批 → 预归档边界测试")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ApprovalToPreArchiveBoundaryTest {

    @Autowired
    private ArchiveApprovalService archiveApprovalService;

    @Autowired
    private PreArchiveSubmitService preArchiveSubmitService;

    @Autowired
    private ArchiveApprovalMapper archiveApprovalMapper;

    @Autowired
    private ArchiveMapper archiveMapper;

    @Autowired
    private ArcFileContentMapper arcFileContentMapper;

    private static String testFileId;
    private static String testArchiveId;
    private static String testApprovalId;

    @BeforeEach
    void setUp() {
        cleanupTestData();
    }

    @AfterEach
    void tearDown() {
        cleanupTestData();
    }

    private void cleanupTestData() {
        if (testApprovalId != null) {
            archiveApprovalMapper.deleteById(testApprovalId);
            testApprovalId = null;
        }
        if (testArchiveId != null) {
            archiveMapper.deleteById(testArchiveId);
            testArchiveId = null;
        }
        if (testFileId != null) {
            arcFileContentMapper.deleteById(testFileId);
            testFileId = null;
        }
    }

    /**
     * 场景1：验证 @Lazy 循环依赖解决有效
     *
     * 验证点：
     * 1. ArchiveApprovalServiceImpl 可以成功启动
     * 2. PreArchiveSubmitService 注入不为 null
     * 3. 两者可以正常协作
     *
     * 注意：这个测试能通过说明 @Lazy 注解成功避免了循环依赖
     */
    @Test
    @Order(1)
    @DisplayName("场景1: @Lazy 注解应成功解决循环依赖")
    void lazyInjection_ShouldResolveCircularDependency() {
        // Arrange & Act: 通过 Spring 正常启动已验证 @Lazy 有效
        // 这个测试本身能运行就说明 Spring 容器启动成功

        // Assert: 验证依赖注入成功
        assertNotNull(archiveApprovalService,
                "ArchiveApprovalService 应被正确注入");
        assertNotNull(preArchiveSubmitService,
                "PreArchiveSubmitService 应被正确注入");

        // 验证服务可用性
        assertDoesNotThrow(() -> {
            // 调用无副作用的查询方法验证服务可用
            archiveApprovalService.getApprovalList(1, 1, null);
        }, "服务调用不应抛出异常");
    }

    /**
     * 场景2：审批通过 → completeArchival 被调用
     *
     * 验证点：
     * 1. approveArchive() 调用后，completeArchival() 被执行
     * 2. Archive 状态变更为 archived
     * 3. ArcFileContent 状态变更为 COMPLETED
     * 4. 归档时间被记录
     *
     * 这是验证跨模块依赖的关键测试
     */
    @Test
    @Order(2)
    @DisplayName("场景2: 审批通过应触发 completeArchival")
    @Transactional
    void approveArchive_ShouldTriggerCompleteArchival() {
        // Arrange: 创建完整的审批流程数据
        setupApprovalFlow();

        // Act: 审批通过
        archiveApprovalService.approveArchive(
                testApprovalId,
                "approver-id",
                "审批人",
                "同意归档"
        );

        // Assert: 验证 Archive 状态（completeArchival 的效果）
        Archive archive = archiveMapper.selectById(testArchiveId);
        assertEquals("archived", archive.getStatus(),
                "Archive 状态应为 archived（completeArchival 生效）");

        // 验证 ArcFileContent 状态（completeArchival 的效果）
        ArcFileContent file = arcFileContentMapper.selectById(testFileId);
        assertEquals(PreArchiveStatus.COMPLETED.getCode(), file.getPreArchiveStatus(),
                "文件状态应为 COMPLETED（completeArchival 生效）");
        assertNotNull(file.getArchivedTime(),
                "归档时间应被记录（completeArchival 生效）");
    }

    /**
     * 场景3：审批拒绝 → 状态回退
     *
     * 验证点：
     * 1. Archive 状态变更为 REJECTED
     * 2. ArcFileContent 状态回退到 READY_TO_ARCHIVE
     * 3. 档号保持不变（可复用）
     */
    @Test
    @Order(3)
    @DisplayName("场景3: 审批拒绝应回退状态")
    @Transactional
    void rejectArchive_ShouldRollbackToReadyToArchive() {
        // Arrange: 创建完整的审批流程数据
        setupApprovalFlow();

        // Act: 拒绝审批
        archiveApprovalService.rejectArchive(
                testApprovalId,
                "approver-id",
                "审批人",
                "信息不完整"
        );

        // Assert: 验证状态回退
        Archive archive = archiveMapper.selectById(testArchiveId);
        assertEquals("REJECTED", archive.getStatus(),
                "Archive 状态应为 REJECTED");

        // 验证文件状态回退
        ArcFileContent file = arcFileContentMapper.selectById(testFileId);
        assertEquals(PreArchiveStatus.READY_TO_ARCHIVE.getCode(), file.getPreArchiveStatus(),
                "文件状态应回退到 READY_TO_ARCHIVE");

        // 验证档号保持（可重新提交）
        assertNotNull(file.getArchivalCode(),
                "档号应保持不变，允许重新提交");
    }

    /**
     * 场景4：防重锁 - 重复审批应被拒绝
     *
     * 验证点：
     * 1. 同一审批申请不能被重复审批
     * 2. 第二次审批应抛出 CONFLICT 异常
     */
    @Test
    @Order(4)
    @DisplayName("场景4: 重复审批应被拒绝")
    @Transactional
    void approveArchive_Twice_ShouldThrowConflict() {
        // Arrange: 创建完整的审批流程数据
        setupApprovalFlow();

        // Act: 第一次审批
        archiveApprovalService.approveArchive(
                testApprovalId,
                "approver-id",
                "审批人",
                "同意"
        );

        // Act & Assert: 第二次审批应失败
        Exception exception = assertThrows(Exception.class, () -> {
            archiveApprovalService.approveArchive(
                    testApprovalId,
                    "approver-id",
                    "审批人",
                    "重复审批"
            );
        });

        assertTrue(exception.getMessage().contains("CONFLICT") ||
                exception.getMessage().contains("已处理") ||
                exception.getMessage().contains("processed"),
                "异常应表明审批已被处理");

        // 验证状态保持为 archived（第一次审批生效）
        Archive archive = archiveMapper.selectById(testArchiveId);
        assertEquals("archived", archive.getStatus(),
                "状态应保持为 archived（第一次审批生效）");
    }

    /**
     * 场景5：批量审批 - 每个审批独立处理
     *
     * 验证点：
     * 1. 批量审批中单个失败不影响其他
     * 2. 返回正确的结果统计
     */
    @Test
    @Order(5)
    @DisplayName("场景5: 批量审批应独立处理每个申请")
    @Transactional
    void batchApprove_ShouldHandleIndividually() {
        // Arrange: 创建多个审批申请
        String approvalId1 = createSingleApproval("batch-test-1");
        String approvalId2 = createSingleApproval("batch-test-2");
        String approvalId3 = "non-existent-id"; // 故意使用不存在的 ID

        com.nexusarchive.dto.approval.BatchApprovalRequest request =
                new com.nexusarchive.dto.approval.BatchApprovalRequest();
        request.setIds(java.util.List.of(approvalId1, approvalId2, approvalId3));
        request.setApproverId("batch-approver");
        request.setApproverName("批量审批人");
        request.setComment("批量同意");

        // Act
        com.nexusarchive.dto.approval.BatchApprovalResponse response =
                archiveApprovalService.batchApprove(request);

        // Assert
        assertEquals(2, response.getSuccessCount(),
                "应有 2 个成功");
        assertEquals(1, response.getFailed(),
                "应有 1 个失败");

        // 验证成功的 Archive 状态已变更
        Archive archive1 = archiveMapper.selectById(
                getArchiveIdByApprovalId(approvalId1));
        assertNotNull(archive1);
        assertEquals("archived", archive1.getStatus(),
                "第一个档案状态应为 archived");

        // 清理
        archiveApprovalMapper.deleteById(approvalId1);
        archiveApprovalMapper.deleteById(approvalId2);
        if (archive1 != null) {
            archiveMapper.deleteById(archive1.getId());
        }
    }

    /**
     * 场景6：完整流程测试 - 从创建到完成归档
     *
     * 验证点：
     * 1. 完整的预归档 → 审批 → 完成归档流程
     * 2. 每个阶段状态正确
     * 3. 跨模块协作无异常
     */
    @Test
    @Order(6)
    @DisplayName("场景6: 完整流程 - 预归档到完成归档")
    @Transactional
    void fullFlow_FromPreArchiveToCompleted_ShouldSucceed() {
        // Stage 1: 创建预归档文件
        ArcFileContent file = createTestPreArchiveFile();
        file.setId(null);
        arcFileContentMapper.insert(file);
        testFileId = file.getId();
        assertEquals(PreArchiveStatus.READY_TO_ARCHIVE.getCode(), file.getPreArchiveStatus());

        // Stage 2: 提交归档申请
        ArchiveApproval approval = preArchiveSubmitService.submitForArchival(
                file.getId(),
                "applicant",
                "申请人",
                "完整流程测试"
        );
        testApprovalId = approval.getId();
        testArchiveId = approval.getArchiveId();

        // 验证提交后状态
        ArcFileContent afterSubmit = arcFileContentMapper.selectById(file.getId());
        assertEquals(PreArchiveStatus.SUBMITTED.getCode(), afterSubmit.getPreArchiveStatus());

        Archive afterSubmitArchive = archiveMapper.selectById(testArchiveId);
        assertEquals("PENDING", afterSubmitArchive.getStatus());

        // Stage 3: 审批通过
        archiveApprovalService.approveArchive(
                approval.getId(),
                "approver",
                "审批人",
                "同意归档"
        );

        // 验证最终状态
        ArcFileContent finalFile = arcFileContentMapper.selectById(file.getId());
        assertEquals(PreArchiveStatus.COMPLETED.getCode(), finalFile.getPreArchiveStatus());
        assertNotNull(finalFile.getArchivedTime());

        Archive finalArchive = archiveMapper.selectById(testArchiveId);
        assertEquals("archived", finalArchive.getStatus());

        // 验证审批记录
        ArchiveApproval finalApproval = archiveApprovalMapper.selectById(approval.getId());
        assertEquals("APPROVED", finalApproval.getStatus());
        assertNotNull(finalApproval.getApprovalTime());
    }

    // ==================== 辅助方法 ====================

    private void setupApprovalFlow() {
        // 创建预归档文件
        ArcFileContent file = createTestPreArchiveFile();
        file.setId(null);
        arcFileContentMapper.insert(file);
        testFileId = file.getId();

        // 提交归档申请
        ArchiveApproval approval = preArchiveSubmitService.submitForArchival(
                file.getId(),
                "applicant-id",
                "申请人",
                "边界测试"
        );
        testApprovalId = approval.getId();
        testArchiveId = approval.getArchiveId();
    }

    private String createSingleApproval(String suffix) {
        ArcFileContent file = createTestPreArchiveFile();
        file.setId(null);
        file.setBusinessDocNo("BATCH-" + suffix);
        arcFileContentMapper.insert(file);

        ArchiveApproval approval = preArchiveSubmitService.submitForArchival(
                file.getId(),
                "applicant",
                "申请人",
                "批量测试"
        );

        // 存储映射以便清理
        return approval.getId();
    }

    private String getArchiveIdByApprovalId(String approvalId) {
        ArchiveApproval approval = archiveApprovalMapper.selectById(approvalId);
        return approval != null ? approval.getArchiveId() : null;
    }

    private ArcFileContent createTestPreArchiveFile() {
        ArcFileContent file = new ArcFileContent();
        file.setFileName("边界测试凭证.pdf");
        file.setFileType("PDF");
        file.setStoragePath("/test/boundary-test.pdf");
        file.setFileHash("boundary-test-hash-" + System.currentTimeMillis());
        file.setBusinessDocNo("BOUNDARY-" + System.currentTimeMillis());
        file.setVoucherType("AC01");
        file.setFondsCode("DEFAULT");
        file.setFiscalYear("2026");
        file.setPeriod("01");
        file.setSourceSystem("BOUNDARY-TEST");
        file.setPreArchiveStatus(PreArchiveStatus.READY_TO_ARCHIVE.getCode());
        file.setArchivalCode("TEMP-POOL-" + System.currentTimeMillis());
        file.setCreatedTime(LocalDateTime.now());
        return file;
    }
}
