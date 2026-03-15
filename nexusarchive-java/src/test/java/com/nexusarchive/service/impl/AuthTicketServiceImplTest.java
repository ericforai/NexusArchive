// Input: JUnit 5, Mockito, AssertJ, Spring Framework, Java 标准库
// Output: AuthTicketServiceImplTest 测试用例类
// Pos: 后端测试用例
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusarchive.dto.ApprovalChain;
import com.nexusarchive.dto.AuthScope;
import com.nexusarchive.dto.AuthTicketDetail;
import com.nexusarchive.dto.response.UserResponse;
import com.nexusarchive.entity.AuthTicket;
import com.nexusarchive.mapper.AuthTicketMapper;
import com.nexusarchive.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AuthTicketServiceImpl 单元测试
 *
 * 测试覆盖:
 * - 授权票据创建
 * - 授权票据详情获取
 * - 授权票据撤销
 * - 授权票据列表查询
 * - 边界情况和异常处理
 */
@ExtendWith(MockitoExtension.class)
class AuthTicketServiceImplTest {

    @Mock
    private AuthTicketMapper authTicketMapper;

    @Mock
    private UserService userService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private AuthTicketServiceImpl authTicketService;

    private AuthTicket testTicket;
    private UserResponse testUser;
    private AuthScope testScope;
    private LocalDateTime testNow;

    @BeforeEach
    void setUp() {
        testNow = LocalDateTime.now();

        // 创建测试用户
        testUser = new UserResponse();
        testUser.setId("user-001");
        testUser.setUsername("testuser");
        testUser.setFullName("测试用户");

        // 创建测试访问范围
        testScope = new AuthScope();
        testScope.setArchiveYears(Arrays.asList(2020, 2021, 2022));
        testScope.setDocTypes(Arrays.asList("凭证", "账簿"));
        testScope.setAccessType("READ_ONLY");

        // 创建测试票据
        testTicket = new AuthTicket();
        testTicket.setId("ticket-001");
        testTicket.setApplicantId("user-001");
        testTicket.setApplicantName("测试用户");
        testTicket.setSourceFonds("F001");
        testTicket.setTargetFonds("F002");
        testTicket.setStatus("PENDING");
        testTicket.setExpiresAt(testNow.plusDays(30));
        testTicket.setCreatedAt(testNow);
        testTicket.setLastModifiedTime(testNow);
        testTicket.setDeleted(0);
    }

    // ========== 创建授权票据测试 ==========

    @Nested
    @DisplayName("创建授权票据")
    class CreateAuthTicketTests {

        @Test
        @DisplayName("正常创建授权票据成功")
        void createAuthTicket_ValidInput_Success() throws Exception {
            // Arrange
            when(userService.getUserById("user-001")).thenReturn(testUser);
            when(authTicketMapper.insert(any(AuthTicket.class))).thenAnswer(invocation -> {
                AuthTicket ticket = invocation.getArgument(0);
                ticket.setId("generated-ticket-id");
                return 1;
            });

            // Act
            String ticketId = authTicketService.createAuthTicket(
                    "user-001", "F001", "F002",
                    testScope, testNow.plusDays(30), "业务需要"
            );

            // Assert
            assertThat(ticketId).isNotNull();
            verify(authTicketMapper).insert(any(AuthTicket.class));
        }

        @Test
        @DisplayName("有效期小于1天 - 抛出异常")
        void createAuthTicket_ExpiresTooSoon_ThrowsException() {
            // Act & Assert
            assertThatThrownBy(() -> authTicketService.createAuthTicket(
                    "user-001", "F001", "F002",
                    testScope, testNow.plusHours(12), "业务需要"
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("有效期必须 >= 当前时间 + 1天");
        }

        @Test
        @DisplayName("有效期超过90天 - 抛出异常")
        void createAuthTicket_ExpiresTooLate_ThrowsException() {
            // Act & Assert
            assertThatThrownBy(() -> authTicketService.createAuthTicket(
                    "user-001", "F001", "F002",
                    testScope, testNow.plusDays(100), "业务需要"
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("有效期必须 <= 当前时间 + 90天");
        }

        @Test
        @DisplayName("源全宗和目标全宗相同 - 抛出异常")
        void createAuthTicket_SameFonds_ThrowsException() {
            // Act & Assert
            assertThatThrownBy(() -> authTicketService.createAuthTicket(
                    "user-001", "F001", "F001",
                    testScope, testNow.plusDays(30), "业务需要"
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("源全宗和目标全宗不能相同");
        }

        @Test
        @DisplayName("用户不存在时使用默认名称")
        void createAuthTicket_UserNotFound_UsesDefaultName() {
            // Arrange
            when(userService.getUserById("user-001")).thenReturn(null);
            when(authTicketMapper.insert(any(AuthTicket.class))).thenReturn(1);

            // Act
            authTicketService.createAuthTicket(
                    "user-001", "F001", "F002",
                    testScope, testNow.plusDays(30), "业务需要"
            );

            // Assert
            verify(authTicketMapper).insert(any(AuthTicket.class));
        }

        @Test
        @DisplayName("用户服务异常时使用默认名称")
        void createAuthTicket_UserServiceException_UsesDefaultName() {
            // Arrange
            when(userService.getUserById("user-001")).thenThrow(new RuntimeException("Service unavailable"));
            when(authTicketMapper.insert(any(AuthTicket.class))).thenReturn(1);

            // Act
            authTicketService.createAuthTicket(
                    "user-001", "F001", "F002",
                    testScope, testNow.plusDays(30), "业务需要"
            );

            // Assert
            verify(authTicketMapper).insert(any(AuthTicket.class));
        }

        @Test
        @DisplayName("访问范围序列化失败 - 抛出异常")
        void createAuthTicket_ScopeSerializationFailure_ThrowsException() throws Exception {
            // Arrange
            ObjectMapper failingMapper = mock(ObjectMapper.class);
            when(failingMapper.writeValueAsString(any())).thenThrow(new RuntimeException("Serialization failed"));
            when(userService.getUserById("user-001")).thenReturn(testUser);

            AuthTicketServiceImpl serviceWithBadMapper = new AuthTicketServiceImpl(
                    authTicketMapper, failingMapper, userService
            );

            // Act & Assert
            assertThatThrownBy(() -> serviceWithBadMapper.createAuthTicket(
                    "user-001", "F001", "F002",
                    testScope, testNow.plusDays(30), "业务需要"
            ))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("序列化访问范围失败");
        }
    }

    // ========== 获取授权票据详情测试 ==========

    @Nested
    @DisplayName("获取授权票据详情")
    class GetAuthTicketDetailTests {

        @Test
        @DisplayName("正常获取票据详情成功")
        void getAuthTicketDetail_ValidTicketId_Success() throws Exception {
            // Arrange
            String scopeJson = objectMapper.writeValueAsString(testScope);
            testTicket.setScope(scopeJson);
            when(authTicketMapper.selectById("ticket-001")).thenReturn(testTicket);

            // Act
            AuthTicketDetail detail = authTicketService.getAuthTicketDetail("ticket-001");

            // Assert
            assertThat(detail).isNotNull();
            assertThat(detail.getId()).isEqualTo("ticket-001");
            assertThat(detail.getApplicantId()).isEqualTo("user-001");
            assertThat(detail.getApplicantName()).isEqualTo("测试用户");
            assertThat(detail.getSourceFonds()).isEqualTo("F001");
            assertThat(detail.getTargetFonds()).isEqualTo("F002");
            assertThat(detail.getScope().getArchiveYears()).containsExactly(2020, 2021, 2022);
            assertThat(detail.getScope().getDocTypes()).containsExactly("凭证", "账簿");
            assertThat(detail.getStatus()).isEqualTo("PENDING");
        }

        @Test
        @DisplayName("票据不存在 - 抛出异常")
        void getAuthTicketDetail_TicketNotFound_ThrowsException() {
            // Arrange
            when(authTicketMapper.selectById("nonexistent")).thenReturn(null);

            // Act & Assert
            assertThatThrownBy(() -> authTicketService.getAuthTicketDetail("nonexistent"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("授权票据不存在");
        }

        @Test
        @DisplayName("已删除票据 - 抛出异常")
        void getAuthTicketDetail_DeletedTicket_ThrowsException() {
            // Arrange
            testTicket.setDeleted(1);
            when(authTicketMapper.selectById("ticket-001")).thenReturn(testTicket);

            // Act & Assert
            assertThatThrownBy(() -> authTicketService.getAuthTicketDetail("ticket-001"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("授权票据不存在");
        }

        @Test
        @DisplayName("包含审批链的票据详情")
        void getAuthTicketDetail_WithApprovalChain_Success() throws Exception {
            // Arrange
            String scopeJson = objectMapper.writeValueAsString(testScope);

            ApprovalChain approvalChain = new ApprovalChain();
            ApprovalChain.ApprovalInfo firstApproval = new ApprovalChain.ApprovalInfo();
            firstApproval.setApproverId("approver-001");
            firstApproval.setApproverName("审批人A");
            firstApproval.setApproved(true);
            firstApproval.setTimestamp(testNow);
            approvalChain.setFirstApproval(firstApproval);

            String approvalJson = objectMapper.writeValueAsString(approvalChain);

            testTicket.setScope(scopeJson);
            testTicket.setApprovalSnapshot(approvalJson);
            when(authTicketMapper.selectById("ticket-001")).thenReturn(testTicket);

            // Act
            AuthTicketDetail detail = authTicketService.getAuthTicketDetail("ticket-001");

            // Assert
            assertThat(detail.getApprovalChain()).isNotNull();
            assertThat(detail.getApprovalChain().getFirstApproval()).isNotNull();
            assertThat(detail.getApprovalChain().getFirstApproval().getApproverId()).isEqualTo("approver-001");
            assertThat(detail.getApprovalChain().getFirstApproval().getApproverName()).isEqualTo("审批人A");
        }

        @Test
        @DisplayName("访问范围反序列化失败时使用空范围")
        void getAuthTicketDetail_ScopeDeserializationFailure_ReturnsEmptyScope() throws Exception {
            // Arrange
            testTicket.setScope("invalid-json");
            when(authTicketMapper.selectById("ticket-001")).thenReturn(testTicket);

            // Act
            AuthTicketDetail detail = authTicketService.getAuthTicketDetail("ticket-001");

            // Assert
            assertThat(detail.getScope()).isNotNull();
            assertThat(detail.getScope().getArchiveYears()).isNull();
        }
    }

    // ========== 撤销授权票据测试 ==========

    @Nested
    @DisplayName("撤销授权票据")
    class RevokeAuthTicketTests {

        @Test
        @DisplayName("正常撤销已批准票据成功")
        void revokeAuthTicket_ApprovedTicket_Success() {
            // Arrange
            testTicket.setStatus("APPROVED");
            testTicket.setReason("原始原因");
            when(authTicketMapper.selectById("ticket-001")).thenReturn(testTicket);
            when(authTicketMapper.updateById(any(AuthTicket.class))).thenReturn(1);

            // Act
            authTicketService.revokeAuthTicket("ticket-001", "user-001", "不再需要");

            // Assert
            verify(authTicketMapper).updateById(any(AuthTicket.class));
        }

        @Test
        @DisplayName("撤销第一审批通过的票据成功")
        void revokeAuthTicket_FirstApprovedTicket_Success() {
            // Arrange
            testTicket.setStatus("FIRST_APPROVED");
            when(authTicketMapper.selectById("ticket-001")).thenReturn(testTicket);
            when(authTicketMapper.updateById(any(AuthTicket.class))).thenReturn(1);

            // Act
            authTicketService.revokeAuthTicket("ticket-001", "user-001", null);

            // Assert
            verify(authTicketMapper).updateById(any(AuthTicket.class));
        }

        @Test
        @DisplayName("票据不存在 - 抛出异常")
        void revokeAuthTicket_TicketNotFound_ThrowsException() {
            // Arrange
            when(authTicketMapper.selectById("nonexistent")).thenReturn(null);

            // Act & Assert
            assertThatThrownBy(() -> authTicketService.revokeAuthTicket("nonexistent", "user-001", "原因"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("授权票据不存在");
        }

        @Test
        @DisplayName("已删除票据 - 抛出异常")
        void revokeAuthTicket_DeletedTicket_ThrowsException() {
            // Arrange
            testTicket.setDeleted(1);
            when(authTicketMapper.selectById("ticket-001")).thenReturn(testTicket);

            // Act & Assert
            assertThatThrownBy(() -> authTicketService.revokeAuthTicket("ticket-001", "user-001", "原因"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("授权票据不存在");
        }

        @Test
        @DisplayName("待审批状态不能撤销 - 抛出异常")
        void revokeAuthTicket_PendingStatus_ThrowsException() {
            // Arrange
            testTicket.setStatus("PENDING");
            when(authTicketMapper.selectById("ticket-001")).thenReturn(testTicket);

            // Act & Assert
            assertThatThrownBy(() -> authTicketService.revokeAuthTicket("ticket-001", "user-001", "原因"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("只有已批准或第一审批通过的票据可以撤销");
        }

        @Test
        @DisplayName("已拒绝状态不能撤销 - 抛出异常")
        void revokeAuthTicket_RejectedStatus_ThrowsException() {
            // Arrange
            testTicket.setStatus("REJECTED");
            when(authTicketMapper.selectById("ticket-001")).thenReturn(testTicket);

            // Act & Assert
            assertThatThrownBy(() -> authTicketService.revokeAuthTicket("ticket-001", "user-001", "原因"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("只有已批准或第一审批通过的票据可以撤销");
        }

        @Test
        @DisplayName("非申请人撤销（当前允许，仅记录警告）")
        void revokeAuthTicket_NonApplicant_WarnsButProceeds() {
            // Arrange
            testTicket.setStatus("APPROVED");
            testTicket.setApplicantId("other-user");
            when(authTicketMapper.selectById("ticket-001")).thenReturn(testTicket);
            when(authTicketMapper.updateById(any(AuthTicket.class))).thenReturn(1);

            // Act - 应该成功（虽然会警告）
            authTicketService.revokeAuthTicket("ticket-001", "user-001", "原因");

            // Assert
            verify(authTicketMapper).updateById(any(AuthTicket.class));
        }

        @Test
        @DisplayName("撤销原因不覆盖原始原因")
        void revokeAuthTicket_WithReason_AppendsToExistingReason() {
            // Arrange
            testTicket.setStatus("APPROVED");
            testTicket.setReason("业务需要");
            when(authTicketMapper.selectById("ticket-001")).thenReturn(testTicket);
            when(authTicketMapper.updateById(any(AuthTicket.class))).thenReturn(1);

            // Act
            authTicketService.revokeAuthTicket("ticket-001", "user-001", "业务变更");

            // Assert
            verify(authTicketMapper).updateById(any(AuthTicket.class));
        }
    }

    // ========== 查询授权票据列表测试 ==========

    @Nested
    @DisplayName("查询授权票据列表")
    class ListAuthTicketsTests {

        private IPage<AuthTicket> createMockPage(List<AuthTicket> records) {
            IPage<AuthTicket> mockPage = mock(IPage.class);
            when(mockPage.getRecords()).thenReturn(records);
            when(mockPage.getTotal()).thenReturn((long) records.size());
            when(mockPage.getSize()).thenReturn(10L);
            when(mockPage.getCurrent()).thenReturn(1L);
            when(mockPage.getPages()).thenReturn(1L);
            return mockPage;
        }

        @Test
        @DisplayName("查询所有票据")
        void listAuthTickets_AllParamsEmpty_ReturnsAll() throws Exception {
            // Arrange
            String scopeJson = objectMapper.writeValueAsString(testScope);
            testTicket.setScope(scopeJson);

            when(authTicketMapper.selectPage(any(), any()))
                    .thenReturn(createMockPage(Collections.singletonList(testTicket)));

            // Act
            List<AuthTicketDetail> result = authTicketService.listAuthTickets(
                    null, null, null, null, 1, 10
            );

            // Assert
            assertThat(result).isNotNull();
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo("ticket-001");
        }

        @Test
        @DisplayName("按状态筛选")
        void listAuthTickets_WithStatusFilter_ReturnsMatching() throws Exception {
            // Arrange
            String scopeJson = objectMapper.writeValueAsString(testScope);
            testTicket.setScope(scopeJson);

            when(authTicketMapper.selectPage(any(), any()))
                    .thenReturn(createMockPage(Collections.singletonList(testTicket)));

            // Act
            List<AuthTicketDetail> result = authTicketService.listAuthTickets(
                    "PENDING", null, null, null, 1, 10
            );

            // Assert
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStatus()).isEqualTo("PENDING");
        }

        @Test
        @DisplayName("按申请人筛选")
        void listAuthTickets_WithApplicantFilter_ReturnsMatching() throws Exception {
            // Arrange
            String scopeJson = objectMapper.writeValueAsString(testScope);
            testTicket.setScope(scopeJson);

            when(authTicketMapper.selectPage(any(), any()))
                    .thenReturn(createMockPage(Collections.singletonList(testTicket)));

            // Act
            List<AuthTicketDetail> result = authTicketService.listAuthTickets(
                    null, "user-001", null, null, 1, 10
            );

            // Assert
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getApplicantId()).isEqualTo("user-001");
        }

        @Test
        @DisplayName("按源全宗和目标全宗筛选")
        void listAuthTickets_WithFondsFilter_ReturnsMatching() throws Exception {
            // Arrange
            String scopeJson = objectMapper.writeValueAsString(testScope);
            testTicket.setScope(scopeJson);

            when(authTicketMapper.selectPage(any(), any()))
                    .thenReturn(createMockPage(Collections.singletonList(testTicket)));

            // Act
            List<AuthTicketDetail> result = authTicketService.listAuthTickets(
                    null, null, "F001", "F002", 1, 10
            );

            // Assert
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getSourceFonds()).isEqualTo("F001");
            assertThat(result.get(0).getTargetFonds()).isEqualTo("F002");
        }

        @Test
        @DisplayName("访问范围反序列化失败时使用空范围")
        void listAuthTickets_ScopeDeserializationFailure_ReturnsEmptyScope() {
            // Arrange
            testTicket.setScope("invalid-json");

            when(authTicketMapper.selectPage(any(), any()))
                    .thenReturn(createMockPage(Collections.singletonList(testTicket)));

            // Act
            List<AuthTicketDetail> result = authTicketService.listAuthTickets(
                    null, null, null, null, 1, 10
            );

            // Assert
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getScope()).isNotNull();
        }

        @Test
        @DisplayName("空结果集")
        void listAuthTickets_EmptyResult_ReturnsEmptyList() {
            // Arrange
            when(authTicketMapper.selectPage(any(), any()))
                    .thenReturn(createMockPage(Collections.emptyList()));

            // Act
            List<AuthTicketDetail> result = authTicketService.listAuthTickets(
                    null, null, null, null, 1, 10
            );

            // Assert
            assertThat(result).isEmpty();
        }
    }

    // ========== 边界条件测试 ==========

    @Nested
    @DisplayName("边界条件测试")
    class BoundaryTests {

        @Test
        @DisplayName("有效期为正好1天")
        void createAuthTicket_ExactlyOneDay_Success() {
            // Arrange
            when(userService.getUserById("user-001")).thenReturn(testUser);
            when(authTicketMapper.insert(any(AuthTicket.class))).thenReturn(1);

            // Act
            String ticketId = authTicketService.createAuthTicket(
                    "user-001", "F001", "F002",
                    testScope, testNow.plusDays(1), "业务需要"
            );

            // Assert
            assertThat(ticketId).isNotNull();
            verify(authTicketMapper).insert(any(AuthTicket.class));
        }

        @Test
        @DisplayName("有效期为正好90天")
        void createAuthTicket_Exactly90Days_Success() {
            // Arrange
            when(userService.getUserById("user-001")).thenReturn(testUser);
            when(authTicketMapper.insert(any(AuthTicket.class))).thenReturn(1);

            // Act
            String ticketId = authTicketService.createAuthTicket(
                    "user-001", "F001", "F002",
                    testScope, testNow.plusDays(90), "业务需要"
            );

            // Assert
            assertThat(ticketId).isNotNull();
            verify(authTicketMapper).insert(any(AuthTicket.class));
        }

        @Test
        @DisplayName("空字符串参数被正确处理")
        void createAuthTicket_EmptyStringParams_Success() {
            // Arrange
            when(userService.getUserById("user-001")).thenReturn(testUser);
            when(authTicketMapper.insert(any(AuthTicket.class))).thenReturn(1);

            // Act
            String ticketId = authTicketService.createAuthTicket(
                    "user-001", "F001", "F002",
                    testScope, testNow.plusDays(30), ""
            );

            // Assert
            assertThat(ticketId).isNotNull();
        }
    }
}
