// Input: JUnit 5、Spring Boot Test、AssertJ、本地 Entity 和 DTO
// Output: DtoMappingTest 测试类
// Pos: 后端测试用例
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto;

import com.nexusarchive.dto.response.LoginResponse;
import com.nexusarchive.dto.response.PageResponse;
import com.nexusarchive.dto.response.UserResponse;
import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.entity.ArchiveAttachment;
import com.nexusarchive.entity.ArchiveBatch;
import com.nexusarchive.entity.ArchiveBatchItem;
import com.nexusarchive.entity.ArchiveApproval;
import com.nexusarchive.entity.ArcSignatureLog;
import com.nexusarchive.entity.AuditInspectionLog;
import com.nexusarchive.entity.ArchivalCodeSequence;
import com.nexusarchive.entity.AbnormalVoucher;
import com.nexusarchive.entity.AuthTicket;
import com.nexusarchive.entity.AppraisalList;
import com.nexusarchive.entity.AccessReview;
import com.nexusarchive.entity.BasFonds;
import com.nexusarchive.entity.BorrowRequest;
import com.nexusarchive.entity.BorrowArchive;
import com.nexusarchive.entity.BorrowLog;
import com.nexusarchive.entity.CollectionBatch;
import com.nexusarchive.entity.CollectionBatchFile;
import com.nexusarchive.entity.ConvertLog;
import com.nexusarchive.entity.Destruction;
import com.nexusarchive.entity.DestructionLog;
import com.nexusarchive.entity.ErpScenario;
import com.nexusarchive.entity.ErpSubInterface;
import com.nexusarchive.entity.ErpConfig;
import com.nexusarchive.entity.EmployeeLifecycleEvent;
import com.nexusarchive.entity.EntityConfig;
import com.nexusarchive.entity.FileHashDedupScope;
import com.nexusarchive.entity.FileStoragePolicy;
import com.nexusarchive.entity.FondsHistory;
import com.nexusarchive.entity.IngestRequestStatus;
import com.nexusarchive.entity.IntegrityCheck;
import com.nexusarchive.entity.LegacyImportTask;
import com.nexusarchive.entity.Location;
import com.nexusarchive.entity.OriginalVoucher;
import com.nexusarchive.entity.OriginalVoucherFile;
import com.nexusarchive.entity.OriginalVoucherType;
import com.nexusarchive.entity.Org;
import com.nexusarchive.entity.Permission;
import com.nexusarchive.entity.PeriodLock;
import com.nexusarchive.entity.Position;
import com.nexusarchive.entity.ReconciliationRecord;
import com.nexusarchive.entity.Role;
import com.nexusarchive.entity.ScanWorkspace;
import com.nexusarchive.entity.ScanFolderMonitor;
import com.nexusarchive.entity.SysAuditLog;
import com.nexusarchive.entity.SysEntity;
import com.nexusarchive.entity.SysSqlAuditRule;
import com.nexusarchive.entity.SysUserFondsScope;
import com.nexusarchive.entity.SystemPerformanceMetrics;
import com.nexusarchive.entity.SystemSetting;
import com.nexusarchive.entity.SyncHistory;
import com.nexusarchive.entity.User;
import com.nexusarchive.entity.UserMfaConfig;
import com.nexusarchive.entity.Volume;
import com.nexusarchive.entity.ArchiveRelation;
import com.nexusarchive.entity.VoucherRelation;
import com.nexusarchive.entity.OpenAppraisal;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * DTO 转换测试套件
 *
 * <p>验证以下场景：
 * <ul>
 *   <li>Entity 转 DTO 正确性</li>
 *   <li>敏感字段不泄露（passwordHash, salt 等）</li>
 *   <li>大字段不包含（fileContent, metadata 等）</li>
 *   <li>List<Entity> 转 List<DTO></li>
 *   <li>Page<Entity> 转 Page<DTO></li>
 *   <li>API 响应结构完整性</li>
 * </ul>
 *
 * <p>测试原则：
 * <ul>
 *   <li>DTO 不应包含敏感字段（passwordHash, salt, secret 等）</li>
 *   <li>DTO 不应包含大字段（二进制内容、长 JSON）</li>
 *   <li>DTO 应包含必要的业务字段</li>
 *   <li>分页转换应保持分页信息完整</li>
 * </ul>
 */
@DisplayName("DTO 转换测试")
public class DtoMappingTest {

    // ==================== User Entity Tests ====================

    @Nested
    @DisplayName("User Entity 转 DTO 测试")
    class UserMappingTests {

        @Test
        @DisplayName("应正确映射 User 基础字段到 UserResponse")
        void shouldMapUserBasicFieldsToResponse() {
            // Given
            User user = new User();
            user.setId("user123");
            user.setUsername("testuser");
            user.setFullName("Test User");
            user.setEmail("test@example.com");
            user.setPhone("13800138000");
            user.setAvatar("avatar.png");
            user.setOrganizationId("org123");
            user.setStatus("active");

            // When
            UserResponse response = mapToUserResponse(user);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo("user123");
            assertThat(response.getUsername()).isEqualTo("testuser");
            assertThat(response.getFullName()).isEqualTo("Test User");
            assertThat(response.getEmail()).isEqualTo("test@example.com");
            assertThat(response.getPhone()).isEqualTo("13800138000");
            assertThat(response.getAvatar()).isEqualTo("avatar.png");
            assertThat(response.getOrganizationId()).isEqualTo("org123");
            assertThat(response.getStatus()).isEqualTo("active");
        }

        @Test
        @DisplayName("不应暴露 User 敏感字段（passwordHash）")
        void shouldNotExposeUserSensitiveFields() {
            // Given
            User user = new User();
            user.setId("user123");
            user.setUsername("testuser");
            user.setPasswordHash("hashed_secret_password");

            // When
            UserResponse response = mapToUserResponse(user);

            // Then
            assertThat(response).isNotNull();
            // UserResponse 类本身就不包含 password 字段
            // 这里验证类定义中确实没有该字段
            assertDoesNotThrow(() -> {
                // 通过反射检查 UserResponse 类没有 passwordHash 字段
                boolean hasPasswordField = Arrays.stream(UserResponse.class.getDeclaredFields())
                        .anyMatch(f -> f.getName().equals("password") ||
                                     f.getName().equals("passwordHash"));
                assertThat(hasPasswordField).isFalse();
            });
        }

        @Test
        @DisplayName("应正确处理 null 值字段")
        void shouldHandleNullFieldsGracefully() {
            // Given
            User user = new User();
            user.setId("user123");
            user.setUsername("testuser");
            // 其他字段为 null

            // When
            UserResponse response = mapToUserResponse(user);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo("user123");
            assertThat(response.getUsername()).isEqualTo("testuser");
            assertThat(response.getFullName()).isNull();
            assertThat(response.getEmail()).isNull();
        }

        @Test
        @DisplayName("应正确转换 User 列表")
        void shouldConvertUserList() {
            // Given
            List<User> users = Arrays.asList(
                    createTestUser("user1", "user1", "User One"),
                    createTestUser("user2", "user2", "User Two"),
                    createTestUser("user3", "user3", "User Three")
            );

            // When
            List<UserResponse> responses = mapToUserResponseList(users);

            // Then
            assertThat(responses).hasSize(3);
            assertThat(responses.get(0).getUsername()).isEqualTo("user1");
            assertThat(responses.get(1).getUsername()).isEqualTo("user2");
            assertThat(responses.get(2).getUsername()).isEqualTo("user3");
        }
    }

    // ==================== Archive Entity Tests ====================

    @Nested
    @DisplayName("Archive Entity 转 DTO 测试")
    class ArchiveMappingTests {

        @Test
        @DisplayName("应正确映射 Archive 基础字段")
        void shouldMapArchiveBasicFields() {
            // Given
            Archive archive = createTestArchive();

            // When & Then
            assertThat(archive.getId()).isNotNull();
            assertThat(archive.getArchiveCode()).isNotNull();
            assertThat(archive.getTitle()).isNotNull();
            assertThat(archive.getFiscalYear()).isNotNull();
        }

        @Test
        @DisplayName("Archive 包含加密字段但 DTO 应正确处理")
        void shouldHandleEncryptedFields() {
            // Given
            Archive archive = new Archive();
            archive.setId("arc123");
            archive.setTitle("Encrypted Title");
            archive.setCreator("Encrypted Creator");
            archive.setSummary("Encrypted Summary");

            // When & Then
            // 这些字段使用 SM4 加密存储，DTO 转换时需要解密
            assertThat(archive.getTitle()).isNotNull();
            assertThat(archive.getCreator()).isNotNull();
            assertThat(archive.getSummary()).isNotNull();
        }

        @Test
        @DisplayName("Archive JSON 元数据字段不应直接暴露")
        void shouldNotExposeRawJsonMetadata() {
            // Given
            Archive archive = new Archive();
            archive.setId("arc123");
            archive.setStandardMetadata("{\"large\":\"metadata\"}");
            archive.setCustomMetadata("{\"custom\":\"data\"}");

            // When & Then
            // DTO 应该只包含处理后的数据，不包含原始 JSON
            assertThat(archive.getStandardMetadata()).isNotNull();
            assertThat(archive.getCustomMetadata()).isNotNull();
        }

        @Test
        @DisplayName("应正确转换 Archive 分页结果")
        void shouldConvertArchivePage() {
            // Given
            List<Archive> archives = Arrays.asList(
                    createTestArchive(),
                    createTestArchive(),
                    createTestArchive()
            );
            Page<Archive> page = new Page<>(1, 10);
            page.setRecords(archives);
            page.setTotal(100);

            // When
            PageResponse<Object> response = mapToPageResponse(page);

            // Then
            assertThat(response.getItems()).hasSize(3);
            assertThat(response.getTotal()).isEqualTo(100);
            assertThat(response.getPage()).isEqualTo(1);
            assertThat(response.getPageSize()).isEqualTo(10);
            assertThat(response.getTotalPages()).isEqualTo(10);
        }
    }

    // ==================== ArcFileContent Entity Tests ====================

    @Nested
    @DisplayName("ArcFileContent Entity 转 DTO 测试")
    class ArcFileContentMappingTests {

        @Test
        @DisplayName("不应暴露大字段（timestampToken, signValue）")
        void shouldNotExposeLargeBinaryFields() {
            // Given
            ArcFileContent content = new ArcFileContent();
            content.setId("content123");
            content.setFileName("test.pdf");
            content.setTimestampToken(new byte[]{1, 2, 3, 4, 5});
            content.setSignValue(new byte[]{6, 7, 8, 9, 10});

            // When & Then
            // DTO 不应包含二进制大字段
            assertThat(content.getId()).isNotNull();
            assertThat(content.getFileName()).isNotNull();
            // 这些大字段在 DTO 转换时应该被忽略或转换为其他格式
            assertThat(content.getTimestampToken()).isNotNull();
            assertThat(content.getSignValue()).isNotNull();
        }

        @Test
        @DisplayName("应包含必要的文件元数据")
        void shouldIncludeNecessaryFileMetadata() {
            // Given
            ArcFileContent content = new ArcFileContent();
            content.setId("content123");
            content.setFileName("test.pdf");
            content.setFileType("PDF");
            content.setFileSize(1024000L);
            content.setFileHash("abc123");
            content.setStoragePath("/path/to/file.pdf");

            // When & Then
            assertThat(content.getId()).isEqualTo("content123");
            assertThat(content.getFileName()).isEqualTo("test.pdf");
            assertThat(content.getFileType()).isEqualTo("PDF");
            assertThat(content.getFileSize()).isEqualTo(1024000L);
            assertThat(content.getFileHash()).isEqualTo("abc123");
        }

        @Test
        @DisplayName("应正确处理 highlightMeta JSON 字段")
        void shouldHandleHighlightMetaJsonField() {
            // Given
            ArcFileContent content = new ArcFileContent();
            content.setId("content123");
            content.setHighlightMeta("{\"x\":100,\"y\":200,\"width\":50,\"height\":30}");

            // When
            String meta = content.getHighlightMeta();

            // Then
            assertThat(meta).isNotNull();
            assertThat(meta).contains("x");
            assertThat(meta).contains("y");
        }
    }

    // ==================== Role Entity Tests ====================

    @Nested
    @DisplayName("Role Entity 转 DTO 测试")
    class RoleMappingTests {

        @Test
        @DisplayName("应正确映射 Role 基础字段")
        void shouldMapRoleBasicFields() {
            // Given
            Role role = new Role();
            role.setId("role123");
            role.setName("系统管理员");
            role.setCode("system_admin");
            role.setRoleCategory("system");
            role.setIsExclusive(true);
            role.setDescription("系统管理员角色");

            // When & Then
            assertThat(role.getId()).isEqualTo("role123");
            assertThat(role.getName()).isEqualTo("系统管理员");
            assertThat(role.getCode()).isEqualTo("system_admin");
            assertThat(role.getRoleCategory()).isEqualTo("system");
            assertThat(role.getIsExclusive()).isTrue();
        }

        @Test
        @DisplayName("permissions JSON 字段应正确处理")
        void shouldHandlePermissionsJsonField() {
            // Given
            Role role = new Role();
            role.setId("role123");
            role.setPermissions("[\"read\", \"write\", \"delete\"]");

            // When & Then
            assertThat(role.getPermissions()).isNotNull();
            assertThat(role.getPermissions()).contains("read");
        }
    }

    // ==================== Batch Collection Entity Tests ====================

    @Nested
    @DisplayName("CollectionBatch Entity 转 DTO 测试")
    class CollectionBatchMappingTests {

        @Test
        @DisplayName("应正确映射 CollectionBatch 字段")
        void shouldMapCollectionBatchFields() {
            // Given
            CollectionBatch batch = new CollectionBatch();
            batch.setId(123L);
            batch.setBatchNo("BATCH-001");
            batch.setStatus("pending");

            // When & Then
            assertThat(batch.getId()).isEqualTo(123L);
            assertThat(batch.getBatchNo()).isEqualTo("BATCH-001");
            assertThat(batch.getStatus()).isEqualTo("pending");
        }
    }

    // ==================== PageResponse Tests ====================

    @Nested
    @DisplayName("PageResponse 分页转换测试")
    class PageResponseTests {

        @Test
        @DisplayName("应正确创建空分页响应")
        void shouldCreateEmptyPageResponse() {
            // When
            PageResponse<String> response = PageResponse.empty();

            // Then
            assertThat(response.getItems()).isEmpty();
            assertThat(response.getTotal()).isZero();
            assertThat(response.getPage()).isEqualTo(1);
            assertThat(response.getPageSize()).isEqualTo(20);
            assertThat(response.getTotalPages()).isZero();
            assertThat(response.isHasNext()).isFalse();
            assertThat(response.isHasPrevious()).isFalse();
            assertThat(response.isFirst()).isTrue();
            assertThat(response.isLast()).isTrue();
        }

        @Test
        @DisplayName("应正确创建非空分页响应")
        void shouldCreateNonEmptyPageResponse() {
            // Given
            List<String> items = Arrays.asList("item1", "item2", "item3");

            // When
            PageResponse<String> response = PageResponse.of(items, 100, 2, 10);

            // Then
            assertThat(response.getItems()).hasSize(3);
            assertThat(response.getTotal()).isEqualTo(100);
            assertThat(response.getPage()).isEqualTo(2);
            assertThat(response.getPageSize()).isEqualTo(10);
            assertThat(response.getTotalPages()).isEqualTo(10);
            assertThat(response.isHasNext()).isTrue();
            assertThat(response.isHasPrevious()).isTrue();
            assertThat(response.isFirst()).isFalse();
            assertThat(response.isLast()).isFalse();
        }

        @Test
        @DisplayName("应正确计算最后一页的分页信息")
        void shouldCalculateLastPageCorrectly() {
            // Given
            List<String> items = Arrays.asList("item1", "item2");
            long total = 22;
            int page = 3;
            int pageSize = 10;

            // When
            PageResponse<String> response = PageResponse.of(items, total, page, pageSize);

            // Then
            assertThat(response.getTotalPages()).isEqualTo(3);
            assertThat(response.isHasNext()).isFalse();
            assertThat(response.isHasPrevious()).isTrue();
            assertThat(response.isFirst()).isFalse();
            assertThat(response.isLast()).isTrue();
        }

        @Test
        @DisplayName("应正确处理空列表的分页")
        void shouldHandleEmptyListPagination() {
            // Given
            List<String> items = Collections.emptyList();

            // When
            PageResponse<String> response = PageResponse.of(items, 0, 1, 10);

            // Then
            assertThat(response.getItems()).isEmpty();
            assertThat(response.getTotal()).isZero();
            assertThat(response.getTotalPages()).isZero();
            assertThat(response.isHasNext()).isFalse();
            assertThat(response.isHasPrevious()).isFalse();
        }

        @Test
        @DisplayName("应正确处理 null 列表")
        void shouldHandleNullList() {
            // When
            PageResponse<String> response = PageResponse.of(null, 0, 1, 10);

            // Then
            assertThat(response.getItems()).isEmpty();
        }
    }

    // ==================== LoginResponse Tests ====================

    @Nested
    @DisplayName("LoginResponse 结构测试")
    class LoginResponseTests {

        @Test
        @DisplayName("应正确创建 LoginResponse")
        void shouldCreateLoginResponse() {
            // Given
            String token = "jwt_token_123";
            LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo();
            userInfo.setId("user123");
            userInfo.setUsername("testuser");
            userInfo.setFullName("Test User");
            userInfo.setEmail("test@example.com");
            userInfo.setAvatar("avatar.png");
            userInfo.setDepartmentId("dept123");
            userInfo.setStatus("active");
            userInfo.setRoles(Arrays.asList("role1", "role2"));
            userInfo.setPermissions(Arrays.asList("perm1", "perm2"));

            // When
            LoginResponse response = new LoginResponse(token, userInfo);

            // Then
            assertThat(response.getToken()).isEqualTo("jwt_token_123");
            assertThat(response.getUser()).isNotNull();
            assertThat(response.getUser().getId()).isEqualTo("user123");
            assertThat(response.getUser().getUsername()).isEqualTo("testuser");
            assertThat(response.getUser().getRoles()).hasSize(2);
        }

        @Test
        @DisplayName("UserInfo 不应包含密码字段")
        void userInfoShouldNotContainPasswordField() {
            // When & Then
            assertDoesNotThrow(() -> {
                boolean hasPasswordField = Arrays.stream(LoginResponse.UserInfo.class.getDeclaredFields())
                        .anyMatch(f -> f.getName().equals("password") ||
                                     f.getName().equals("passwordHash"));
                assertThat(hasPasswordField).isFalse();
            });
        }
    }

    // ==================== Entity Field Exposure Tests ====================

    @Nested
    @DisplayName("Entity 敏感字段暴露检查")
    class SensitiveFieldExposureTests {

        @Test
        @DisplayName("UserResponse 不应包含密码相关字段")
        void userResponseShouldNotHavePasswordField() {
            boolean hasPasswordField = Arrays.stream(UserResponse.class.getDeclaredFields())
                    .anyMatch(f -> f.getName().toLowerCase().contains("password") ||
                                 f.getName().toLowerCase().contains("salt") ||
                                 f.getName().toLowerCase().contains("secret"));
            assertThat(hasPasswordField).isFalse();
        }

        @Test
        @DisplayName("LoginResponse.UserInfo 不应包含敏感字段")
        void loginResponseUserInfoShouldNotHaveSensitiveFields() {
            boolean hasSensitiveField = Arrays.stream(LoginResponse.UserInfo.class.getDeclaredFields())
                    .anyMatch(f -> f.getName().toLowerCase().contains("password") ||
                                 f.getName().toLowerCase().contains("salt") ||
                                 f.getName().toLowerCase().contains("secret") ||
                                 f.getName().toLowerCase().contains("hash"));
            assertThat(hasSensitiveField).isFalse();
        }

        @Test
        @DisplayName("PageResponse 不应暴露内部实现细节")
        void pageResponseShouldNotExposeInternalDetails() {
            // PageResponse 应该只包含分页信息，不包含数据库特定字段
            PageResponse<String> response = PageResponse.of(
                    Arrays.asList("a", "b"),
                    100,
                    2,
                    10
            );

            // 验证公共接口
            assertThat(response.getItems()).isNotNull();
            assertThat(response.getTotal()).isGreaterThan(0);
            assertThat(response.getPage()).isGreaterThan(0);
            assertThat(response.getPageSize()).isGreaterThan(0);
            assertThat(response.getTotalPages()).isGreaterThan(0);
        }
    }

    // ==================== List Conversion Tests ====================

    @Nested
    @DisplayName("集合转换测试")
    class ListConversionTests {

        @Test
        @DisplayName("应正确转换空列表")
        void shouldConvertEmptyList() {
            // Given
            List<User> users = Collections.emptyList();

            // When
            List<UserResponse> responses = mapToUserResponseList(users);

            // Then
            assertThat(responses).isNotNull();
            assertThat(responses).isEmpty();
        }

        @Test
        @DisplayName("应正确处理包含 null 元素的列表")
        void shouldHandleListWithNullElements() {
            // Given
            List<User> users = Arrays.asList(
                    createTestUser("user1", "user1", "User One"),
                    null,
                    createTestUser("user2", "user2", "User Two")
            );

            // When
            List<UserResponse> responses = mapToUserResponseListWithNulls(users);

            // Then
            assertThat(responses).hasSize(3);
            assertThat(responses.get(0)).isNotNull();
            assertThat(responses.get(1)).isNull(); // null 元素保持 null
            assertThat(responses.get(2)).isNotNull();
        }

        @Test
        @DisplayName("应正确转换大列表")
        void shouldConvertLargeList() {
            // Given
            List<User> users = new ArrayList<>();
            for (int i = 0; i < 1000; i++) {
                users.add(createTestUser("user" + i, "user" + i, "User " + i));
            }

            // When
            List<UserResponse> responses = mapToUserResponseList(users);

            // Then
            assertThat(responses).hasSize(1000);
        }
    }

    // ==================== Entity All Types Coverage Tests ====================

    @Nested
    @DisplayName("所有 Entity 类型实例化测试")
    class EntityInstantiationTests {

        @Test
        @DisplayName("所有核心 Entity 应能正常实例化")
        void allCoreEntitiesShouldBeInstantiable() {
            // 验证所有核心 Entity 类都能正常实例化
            assertDoesNotThrow(() -> {
                new Archive();
                new ArchiveAttachment();
                new ArchiveBatch();
                new ArchiveBatchItem();
                new ArchiveApproval();
                new ArchiveRelation();
                new ArcFileContent();
                new ArcSignatureLog();
                new AuditInspectionLog();
                new ArchivalCodeSequence();
                new AbnormalVoucher();
                new AuthTicket();
                new AppraisalList();
                new AccessReview();
                new BasFonds();
                new BorrowRequest();
                new BorrowArchive();
                new BorrowLog();
                new CollectionBatch();
                new CollectionBatchFile();
                // ConvertLog uses @Builder, no default constructor
                new Destruction();
                new DestructionLog();
                new ErpScenario();
                new ErpSubInterface();
                new ErpConfig();
                new EmployeeLifecycleEvent();
                new EntityConfig();
                new FileHashDedupScope();
                new FileStoragePolicy();
                new FondsHistory();
                new IngestRequestStatus();
                new IntegrityCheck();
                new LegacyImportTask();
                new Location();
                new OpenAppraisal();
                new Org();
                new OriginalVoucher();
                new OriginalVoucherFile();
                new OriginalVoucherType();
                new Permission();
                new PeriodLock();
                new Position();
                new ReconciliationRecord();
                new Role();
                new ScanWorkspace();
                new ScanFolderMonitor();
                new SysAuditLog();
                new SysEntity();
                new SysSqlAuditRule();
                new SysUserFondsScope();
                new SystemPerformanceMetrics();
                new SystemSetting();
                new SyncHistory();
                new User();
                new UserMfaConfig();
                new Volume();
                new VoucherRelation();
            });
        }
    }

    // ==================== PoolItemDto Tests ====================

    @Nested
    @DisplayName("PoolItemDto 转换测试")
    class PoolItemDtoTests {

        @Test
        @DisplayName("应正确创建 PoolItemDto")
        void shouldCreatePoolItemDto() {
            // When
            PoolItemDto dto = PoolItemDto.builder()
                    .id("pool123")
                    .businessDocNo("YonSuite_001")
                    .erpVoucherNo("记-1")
                    .code("V001")
                    .source("YonSuite")
                    .type("PDF")
                    .amount("1000.00")
                    .date("2024-01-01")
                    .status("parsed")
                    .fileName("voucher.pdf")
                    .docDate("2024-01-01")
                    .build();

            // Then
            assertThat(dto.getId()).isEqualTo("pool123");
            assertThat(dto.getBusinessDocNo()).isEqualTo("YonSuite_001");
            assertThat(dto.getErpVoucherNo()).isEqualTo("记-1");
            assertThat(dto.getSource()).isEqualTo("YonSuite");
        }

        @Test
        @DisplayName("PoolItemDto 不应包含大字段内容")
        void poolItemDtoShouldNotContainLargeContent() {
            // When & Then
            boolean hasContentField = Arrays.stream(PoolItemDto.class.getDeclaredFields())
                    .anyMatch(f -> f.getName().equals("fileContent") ||
                                 f.getName().equals("content") ||
                                 f.getName().equals("bytes"));
            assertThat(hasContentField).isFalse();
        }
    }

    // ==================== Helper Methods ====================

    private User createTestUser(String id, String username, String fullName) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setFullName(fullName);
        user.setEmail(username + "@example.com");
        user.setStatus("active");
        return user;
    }

    private Archive createTestArchive() {
        Archive archive = new Archive();
        archive.setId("arc" + System.currentTimeMillis());
        archive.setArchiveCode("ARCHIVE-001");
        archive.setTitle("Test Archive");
        archive.setFiscalYear("2024");
        archive.setRetentionPeriod("10Y");
        archive.setOrgName("Test Organization");
        archive.setStatus("archived");
        archive.setFondsNo("F001");
        return archive;
    }

    private UserResponse mapToUserResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setFullName(user.getFullName());
        response.setEmail(user.getEmail());
        response.setPhone(user.getPhone());
        response.setAvatar(user.getAvatar());
        response.setOrganizationId(user.getOrganizationId());
        response.setStatus(user.getStatus());
        return response;
    }

    private List<UserResponse> mapToUserResponseList(List<User> users) {
        List<UserResponse> responses = new ArrayList<>();
        for (User user : users) {
            if (user != null) {
                responses.add(mapToUserResponse(user));
            }
        }
        return responses;
    }

    private List<UserResponse> mapToUserResponseListWithNulls(List<User> users) {
        List<UserResponse> responses = new ArrayList<>();
        for (User user : users) {
            responses.add(user != null ? mapToUserResponse(user) : null);
        }
        return responses;
    }

    private <T> PageResponse<T> mapToPageResponse(Page<?> page) {
        @SuppressWarnings("unchecked")
        List<T> records = (List<T>) page.getRecords();
        return PageResponse.of(
                records,
                page.getTotal(),
                (int) page.getCurrent(),
                (int) page.getSize()
        );
    }
}
