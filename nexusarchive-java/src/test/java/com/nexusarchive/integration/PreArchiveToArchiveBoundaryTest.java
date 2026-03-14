// Input: Spring Test、JUnit 5、MyBatis-Plus
// Output: PreArchiveToArchiveBoundaryTest 集成测试
// Pos: 后端集成测试 - 边界场景
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.integration;

import com.nexusarchive.entity.Archive;
import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.entity.ArchiveApproval;
import com.nexusarchive.entity.enums.PreArchiveStatus;
import com.nexusarchive.mapper.ArchiveMapper;
import com.nexusarchive.mapper.ArcFileContentMapper;
import com.nexusarchive.mapper.ArchiveApprovalMapper;
import com.nexusarchive.service.PreArchiveSubmitService;
import com.nexusarchive.service.ArchiveApprovalService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 预归档 → 正式归档 边界集成测试
 *
 * 测试目的：验证 PreArchiveSubmitService 与 Archive 之间的边界行为
 * 覆盖场景：
 * 1. 预归档文件提交归档申请 → 创建 Archive 记录
 * 2. 预归档文件提交归档申请 → 更新已存在的 Archive 记录（ERP 同步场景）
 * 3. 审批通过 → 触发 completeArchival → 状态协调变更
 * 4. 审批拒绝 → 状态回退
 *
 * 对应文档：docs/architecture/module-dependency-status.md#一、已确认的跨模块依赖
 *
 * @author Architecture Boundary Test
 * @since 2026-03-14
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("预归档 → 正式归档边界测试")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PreArchiveToArchiveBoundaryTest {

    @Autowired
    private PreArchiveSubmitService preArchiveSubmitService;

    @Autowired
    private ArchiveApprovalService archiveApprovalService;

    @Autowired
    private ArcFileContentMapper arcFileContentMapper;

    @Autowired
    private ArchiveMapper archiveMapper;

    @Autowired
    private ArchiveApprovalMapper archiveApprovalMapper;

    private static String testFileId;
    private static String testArchiveId;
    private static String testApprovalId;

    @BeforeEach
    void setUp() {
        // 每个测试前准备测试数据
        cleanupTestData();
    }

    @AfterEach
    void tearDown() {
        // 每个测试后清理数据
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
     * 场景1：预归档文件提交归档 → 创建新 Archive 记录
     *
     * 验证点：
     * 1. Archive 记录被正确创建
     * 2. 档号格式正确（包含全宗号-年度-保管期限-分类-件号）
     * 3. ArcFileContent 状态变更为 SUBMITTED
     * 4. 审批申请被创建
     */
    @Test
    @Order(1)
    @DisplayName("场景1: 预归档提交归档应创建新 Archive 记录")
    @Transactional
    void submitForArchival_WhenArchiveNotExists_ShouldCreateNewArchive() {
        // Arrange: 准备一个 READY_TO_ARCHIVE 状态的预归档文件
        ArcFileContent file = createTestPreArchiveFile();
        file.setPreArchiveStatus(PreArchiveStatus.READY_TO_ARCHIVE.getCode());
        file.setId(null); // 让数据库生成 ID
        arcFileContentMapper.insert(file);
        testFileId = file.getId();

        // Act: 提交归档申请
        ArchiveApproval approval = preArchiveSubmitService.submitForArchival(
                file.getId(),
                "test-user",
                "测试用户",
                "边界测试提交"
        );
        testApprovalId = approval.getId();

        // Assert: 验证 Archive 记录
        List<Archive> archives = archiveMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Archive>()
                        .eq(Archive::getArchiveCode, approval.getArchiveCode())
        );
        assertEquals(1, archives.size(), "应创建一个 Archive 记录");
        Archive createdArchive = archives.get(0);
        testArchiveId = createdArchive.getId();

        // 验证档号格式
        assertTrue(createdArchive.getArchiveCode().contains("-"),
                "档号应包含分隔符");
        assertEquals("PENDING", createdArchive.getStatus(),
                "新建 Archive 状态应为 PENDING");

        // 验证 ArcFileContent 状态变更
        ArcFileContent updatedFile = arcFileContentMapper.selectById(file.getId());
        assertEquals(PreArchiveStatus.SUBMITTED.getCode(), updatedFile.getPreArchiveStatus(),
                "文件状态应变更为 SUBMITTED");

        // 验证审批申请
        assertNotNull(approval.getId(), "审批申请应被创建");
        assertEquals(testArchiveId, approval.getArchiveId(),
                "审批申请应关联到正确的 Archive");
    }

    /**
     * 场景2：ERP 同步场景 - Archive 已存在，应更新而非创建
     *
     * 验证点：
     * 1. 不创建新的 Archive 记录
     * 2. 更新现有 Archive 的档号和标题
     * 3. 状态正确变更为 PENDING
     */
    @Test
    @Order(2)
    @DisplayName("场景2: ERP同步场景 - Archive已存在应更新")
    @Transactional
    void submitForArchival_WhenArchiveExistsFromERP_ShouldUpdateArchive() {
        // Arrange: 先创建一个 Archive（模拟 ERP 同步）
        Archive existingArchive = new Archive();
        existingArchive.setId("erp-sync-test-001");
        existingArchive.setArchiveCode("TEMP-ERP-001");
        existingArchive.setTitle("ERP 同步临时标题");
        existingArchive.setStatus("DRAFT");
        existingArchive.setFondsNo("DEFAULT");
        existingArchive.setFiscalYear("2026");
        existingArchive.setRetentionPeriod("30Y");
        existingArchive.setCategoryCode("AC01");
        archiveMapper.insert(existingArchive);

        // 创建相同 ID 的预归档文件（ERP 同步时 ID 会相同）
        ArcFileContent file = createTestPreArchiveFile();
        file.setId(existingArchive.getId());
        file.setPreArchiveStatus(PreArchiveStatus.READY_TO_ARCHIVE.getCode());
        arcFileContentMapper.insert(file);
        testFileId = file.getId();
        testArchiveId = existingArchive.getId();

        // Act: 提交归档申请
        ArchiveApproval approval = preArchiveSubmitService.submitForArchival(
                file.getId(),
                "test-user",
                "测试用户",
                "ERP 同步边界测试"
        );
        testApprovalId = approval.getId();

        // Assert: 验证 Archive 被更新而非新建
        Archive updatedArchive = archiveMapper.selectById(existingArchive.getId());
        assertNotNull(updatedArchive, "Archive 记录应存在");
        assertNotEquals("TEMP-ERP-001", updatedArchive.getArchiveCode(),
                "档号应被更新为正式档号");
        assertEquals("PENDING", updatedArchive.getStatus(),
                "状态应更新为 PENDING");
        assertNotEquals("ERP 同步临时标题", updatedArchive.getTitle(),
                "标题应被更新");

        // 验证只有一条 Archive 记录
        long count = archiveMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Archive>()
                        .eq(Archive::getId, existingArchive.getId())
        );
        assertEquals(1, count, "应只有一条 Archive 记录（更新而非新建）");
    }

    /**
     * 场景3：审批通过 → 触发 completeArchival → Archive 状态变更为 archived
     *
     * 验证点：
     * 1. Archive 状态从 PENDING 变更为 archived
     * 2. ArcFileContent 状态变更为 COMPLETED
     * 3. ArchiveApproval 状态变更为 APPROVED
     */
    @Test
    @Order(3)
    @DisplayName("场景3: 审批通过应触发完成归档流程")
    @Transactional
    void approveArchive_ShouldTriggerCompleteArchival() {
        // Arrange: 先提交归档申请
        ArcFileContent file = createTestPreArchiveFile();
        file.setPreArchiveStatus(PreArchiveStatus.READY_TO_ARCHIVE.getCode());
        file.setId(null);
        arcFileContentMapper.insert(file);
        testFileId = file.getId();

        ArchiveApproval approval = preArchiveSubmitService.submitForArchival(
                file.getId(),
                "test-user",
                "测试用户",
                "边界测试"
        );
        testApprovalId = approval.getId();

        // Act: 审批通过
        archiveApprovalService.approveArchive(
                approval.getId(),
                "approver",
                "审批人",
                "同意归档"
        );

        // Assert: 验证 Archive 状态
        Archive archive = archiveMapper.selectById(approval.getArchiveId());
        testArchiveId = archive.getId();
        assertEquals("archived", archive.getStatus(),
                "Archive 状态应变更为 archived");

        // 验证 ArcFileContent 状态
        ArcFileContent updatedFile = arcFileContentMapper.selectById(file.getId());
        assertEquals(PreArchiveStatus.COMPLETED.getCode(), updatedFile.getPreArchiveStatus(),
                "文件状态应变更为 COMPLETED");
        assertNotNull(updatedFile.getArchivedTime(),
                "归档时间应被记录");

        // 验证审批状态
        ArchiveApproval updatedApproval = archiveApprovalMapper.selectById(approval.getId());
        assertEquals("APPROVED", updatedApproval.getStatus(),
                "审批状态应变更为 APPROVED");
    }

    /**
     * 场景4：审批拒绝 → 状态回退
     *
     * 验证点：
     * 1. Archive 状态变更为 REJECTED
     * 2. ArcFileContent 状态回退到 READY_TO_ARCHIVE
     * 3. 可以重新提交
     */
    @Test
    @Order(4)
    @DisplayName("场景4: 审批拒绝应回退状态")
    @Transactional
    void rejectArchive_ShouldRollbackStatus() {
        // Arrange: 先提交归档申请
        ArcFileContent file = createTestPreArchiveFile();
        file.setPreArchiveStatus(PreArchiveStatus.READY_TO_ARCHIVE.getCode());
        file.setId(null);
        arcFileContentMapper.insert(file);
        testFileId = file.getId();

        ArchiveApproval approval = preArchiveSubmitService.submitForArchival(
                file.getId(),
                "test-user",
                "测试用户",
                "边界测试"
        );
        testApprovalId = approval.getId();

        // Act: 拒绝审批
        archiveApprovalService.rejectArchive(
                approval.getId(),
                "approver",
                "审批人",
                "信息不完整，请补充"
        );

        // Assert: 验证状态回退
        Archive archive = archiveMapper.selectById(approval.getArchiveId());
        testArchiveId = archive.getId();
        assertEquals("REJECTED", archive.getStatus(),
                "Archive 状态应为 REJECTED");

        ArcFileContent updatedFile = arcFileContentMapper.selectById(file.getId());
        assertEquals(PreArchiveStatus.READY_TO_ARCHIVE.getCode(), updatedFile.getPreArchiveStatus(),
                "文件状态应回退到 READY_TO_ARCHIVE");

        // 验证可以重新提交（状态允许）
        assertEquals(PreArchiveStatus.READY_TO_ARCHIVE.getCode(), updatedFile.getPreArchiveStatus(),
                "文件状态允许重新提交归档");
    }

    /**
     * 场景5：状态转换一致性 - 双重状态机协调
     *
     * 验证点：
     * ArcFileContent 和 Archive 的状态转换是原子性的
     * - READY_TO_ARCHIVE → SUBMITTED → COMPLETED (文件)
     * - null → PENDING → archived (档案)
     */
    @Test
    @Order(5)
    @DisplayName("场景5: 状态转换应保持一致性")
    @Transactional
    void stateTransition_ShouldMaintainConsistency() {
        // Arrange
        ArcFileContent file = createTestPreArchiveFile();
        file.setPreArchiveStatus(PreArchiveStatus.READY_TO_ARCHIVE.getCode());
        file.setId(null);
        arcFileContentMapper.insert(file);
        testFileId = file.getId();

        // Act: 完整流程
        ArchiveApproval approval = preArchiveSubmitService.submitForArchival(
                file.getId(), "user", "用户", "测试"
        );
        archiveApprovalService.approveArchive(
                approval.getId(), "approver", "审批人", "同意"
        );

        // Assert: 验证状态一致性
        ArcFileContent finalFile = arcFileContentMapper.selectById(file.getId());
        Archive finalArchive = archiveMapper.selectById(approval.getArchiveId());
        testArchiveId = finalArchive.getId();

        // 文件状态: READY_TO_ARCHIVE → SUBMITTED → COMPLETED
        assertEquals(PreArchiveStatus.COMPLETED.getCode(), finalFile.getPreArchiveStatus());

        // 档案状态: null → PENDING → archived
        assertEquals("archived", finalArchive.getStatus());

        // 关联关系正确
        assertEquals(finalArchive.getArchiveCode(), finalFile.getArchivalCode(),
                "档号应保持一致");
    }

    // ==================== 辅助方法 ====================

    private ArcFileContent createTestPreArchiveFile() {
        ArcFileContent file = new ArcFileContent();
        file.setFileName("测试凭证.pdf");
        file.setFileType("PDF");
        file.setStoragePath("/test/path/test.pdf");
        file.setFileHash("test-hash-" + System.currentTimeMillis());
        file.setBusinessDocNo("TEST-" + System.currentTimeMillis());
        file.setVoucherType("AC01");
        file.setFondsCode("DEFAULT");
        file.setFiscalYear("2026");
        file.setPeriod("01");
        file.setSourceSystem("TEST");
        file.setArchivalCode("TEMP-POOL-" + System.currentTimeMillis());
        file.setCreatedTime(LocalDateTime.now());
        return file;
    }
}
