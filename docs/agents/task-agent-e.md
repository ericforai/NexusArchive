# Agent E: 质量保障工程师任务书

> **角色**: 质量保障工程师
> **技术栈**: JUnit 5, Mockito, Jest, React Testing Library, 性能测试
> **负责阶段**: 第四阶段 - 质量提升
> **前置依赖**: ⚠️ 需等待 Agent A-D 完成所有功能开发

---

## 📋 项目背景

NexusArchive 目前测试覆盖不足，需要全面提升代码质量：
- 后端仅 16 个测试类
- 核心业务逻辑测试不足
- 无性能基准测试

### 关键约束
- **合规性 > 性能**：但关键路径必须有性能保障
- **测试即文档**：测试用例应能说明业务规则
- **回归防护**：核心流程必须有端到端测试

---

## 🔐 必读规则

执行任务前，请阅读以下规则文件：

1. **[.agent/rules/general.md](file:///Users/user/nexusarchive/.agent/rules/general.md)** - 业务规则参考

---

## ✅ 任务清单

### 4.1 Service 层单元测试（目标覆盖率 80%）

| 服务类 | 优先级 | 关键测试点 | 状态 |
|--------|--------|------------|------|
| `IngestService` | P0 | 归档入库、四性检测触发 | 📋 |
| `FourNatureCheckService` | P0 | 四性检测全流程 | 📋 |
| `BorrowingService` | P0 | 借阅申请、审批、归还 | 📋 |
| `DestructionService` | P0 | 销毁申请、审批、执行 | 📋 |
| `AuthService` | P1 | 登录、登出、Token刷新 | 📋 |
| `ArchiveRelationService` | P1 | 关联关系创建和查询 | 📋 |
| `SignatureService` | P1 | 签章和验签 | 📋 |
| `AuditLogService` | P1 | 日志链完整性 | 📋 |

**测试模板：**
```java
// IngestServiceTest.java
@ExtendWith(MockitoExtension.class)
class IngestServiceTest {
    
    @Mock
    private ArchiveMapper archiveMapper;
    
    @Mock
    private FileContentService fileContentService;
    
    @Mock
    private FourNatureCheckService fourNatureService;
    
    @Mock
    private AuditLogService auditLogService;
    
    @InjectMocks
    private IngestService ingestService;
    
    @Test
    @DisplayName("归档入库 - 正常流程")
    void ingest_ValidArchive_Success() {
        // Arrange
        IngestRequest request = createValidIngestRequest();
        when(fourNatureService.check(any())).thenReturn(CheckResult.passed());
        when(archiveMapper.insert(any())).thenReturn(1);
        
        // Act
        IngestResult result = ingestService.ingest(request);
        
        // Assert
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getArchivalCode()).isNotNull();
        verify(fourNatureService).check(any());
        verify(auditLogService).log(eq(OperationType.CAPTURE), any());
    }
    
    @Test
    @DisplayName("归档入库 - 四性检测失败应拒绝")
    void ingest_FourNatureCheckFailed_Rejected() {
        // Arrange
        IngestRequest request = createValidIngestRequest();
        when(fourNatureService.check(any()))
            .thenReturn(CheckResult.failed("完整性检测失败：缺少必填元数据"));
        
        // Act
        IngestResult result = ingestService.ingest(request);
        
        // Assert
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("完整性检测失败");
        verify(archiveMapper, never()).insert(any());
    }
    
    @Test
    @DisplayName("归档入库 - 重复凭证应返回已归档错误")
    void ingest_DuplicateVoucher_Conflict() {
        // Arrange
        IngestRequest request = createValidIngestRequest();
        when(archiveMapper.findByVoucherNo(request.getVoucherNo()))
            .thenReturn(Optional.of(existingArchive()));
        
        // Act & Assert
        assertThatThrownBy(() -> ingestService.ingest(request))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("409_ALREADY_ARCHIVED");
    }
}
```

```java
// FourNatureCheckServiceTest.java
@ExtendWith(MockitoExtension.class)
class FourNatureCheckServiceTest {
    
    @Mock
    private AuthenticityChecker authenticityChecker;
    
    @Mock
    private IntegrityChecker integrityChecker;
    
    @Mock
    private UsabilityChecker usabilityChecker;
    
    @Mock
    private SafetyChecker safetyChecker;
    
    @InjectMocks
    private FourNatureCheckService service;
    
    @Test
    @DisplayName("四性检测 - 全部通过")
    void check_AllPassed() {
        // Arrange
        Archive archive = createTestArchive();
        when(authenticityChecker.check(any())).thenReturn(CheckResult.passed());
        when(integrityChecker.check(any())).thenReturn(CheckResult.passed());
        when(usabilityChecker.check(any())).thenReturn(CheckResult.passed());
        when(safetyChecker.check(any())).thenReturn(CheckResult.passed());
        
        // Act
        FourNatureResult result = service.check(archive);
        
        // Assert
        assertThat(result.isPassed()).isTrue();
        assertThat(result.getAuthenticityResult().isPassed()).isTrue();
        assertThat(result.getIntegrityResult().isPassed()).isTrue();
        assertThat(result.getUsabilityResult().isPassed()).isTrue();
        assertThat(result.getSafetyResult().isPassed()).isTrue();
    }
    
    @Test
    @DisplayName("四性检测 - 真实性失败（哈希不匹配）")
    void check_AuthenticityFailed_HashMismatch() {
        // Arrange
        Archive archive = createArchiveWithTamperedFile();
        when(authenticityChecker.check(any()))
            .thenReturn(CheckResult.failed("文件哈希值不匹配"));
        
        // Act
        FourNatureResult result = service.check(archive);
        
        // Assert
        assertThat(result.isPassed()).isFalse();
        assertThat(result.getAuthenticityResult().getIssues())
            .contains("文件哈希值不匹配");
    }
}
```

---

### 4.2 Controller 层测试（目标覆盖率 100%）

| 控制器 | 测试内容 | 状态 |
|--------|----------|------|
| `ArchiveController` | CRUD、搜索、导出 | 📋 |
| `IngestController` | 归档入库、批量导入 | 📋 |
| `BorrowingController` | 借阅全流程 | 📋 |
| `ComplianceController` | 四性检测报告 | 📋 |
| `AuthController` | 登录登出、密码策略 | 📋 |
| `AdminController` | 用户、角色、权限管理 | 📋 |

**测试模板：**
```java
@WebMvcTest(ArchiveController.class)
@AutoConfigureMockMvc
class ArchiveControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private ArchiveService archiveService;
    
    @Test
    @WithMockUser(roles = "USER")
    void getArchive_ValidId_ReturnsArchive() throws Exception {
        // Arrange
        String archiveId = "test-id";
        Archive archive = createTestArchive(archiveId);
        when(archiveService.findById(archiveId)).thenReturn(Optional.of(archive));
        
        // Act & Assert
        mockMvc.perform(get("/api/archive/{id}", archiveId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(archiveId))
            .andExpect(jsonPath("$.title").exists());
    }
    
    @Test
    @WithMockUser(roles = "USER")
    void getArchive_NotFound_Returns404() throws Exception {
        when(archiveService.findById(any())).thenReturn(Optional.empty());
        
        mockMvc.perform(get("/api/archive/{id}", "non-existent"))
            .andExpect(status().isNotFound());
    }
    
    @Test
    void getArchive_Unauthorized_Returns401() throws Exception {
        mockMvc.perform(get("/api/archive/{id}", "test-id"))
            .andExpect(status().isUnauthorized());
    }
}
```

---

### 4.3 前端组件测试（目标覆盖率 60%）

| 组件 | 测试内容 | 状态 |
|------|----------|------|
| `useAuthStore` | 登录状态、权限判断 | 📋 |
| `ArchiveList` | 列表渲染、分页、排序 | 📋 |
| `ArchiveDetail` | 详情展示、关联关系 | 📋 |
| `LoginForm` | 表单验证、提交 | 📋 |
| `BorrowingForm` | 借阅申请表单 | 📋 |

**测试模板：**
```typescript
// useAuthStore.test.ts
import { renderHook, act } from '@testing-library/react'
import { useAuthStore } from './useAuthStore'

describe('useAuthStore', () => {
  beforeEach(() => {
    // 清理存储
    localStorage.clear()
    useAuthStore.setState({ token: null, user: null, isAuthenticated: false })
  })
  
  it('登录后应设置用户状态', () => {
    const { result } = renderHook(() => useAuthStore())
    
    act(() => {
      result.current.login('test-token', {
        id: '1',
        username: 'admin',
        realName: '管理员',
        roles: ['ADMIN'],
        permissions: ['archive:read', 'archive:write']
      })
    })
    
    expect(result.current.isAuthenticated).toBe(true)
    expect(result.current.token).toBe('test-token')
    expect(result.current.user?.username).toBe('admin')
  })
  
  it('权限检查应正确匹配', () => {
    const { result } = renderHook(() => useAuthStore())
    
    act(() => {
      result.current.login('token', {
        id: '1',
        username: 'user',
        realName: '用户',
        roles: [],
        permissions: ['archive:read', 'borrowing:*']
      })
    })
    
    expect(result.current.hasPermission('archive:read')).toBe(true)
    expect(result.current.hasPermission('archive:write')).toBe(false)
    expect(result.current.hasPermission('borrowing:create')).toBe(true)
  })
  
  it('登出后应清除状态', () => {
    const { result } = renderHook(() => useAuthStore())
    
    act(() => {
      result.current.login('token', mockUser)
      result.current.logout()
    })
    
    expect(result.current.isAuthenticated).toBe(false)
    expect(result.current.token).toBeNull()
  })
})
```

```typescript
// ArchiveList.test.tsx
import { render, screen, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { ArchiveList } from './ArchiveList'

const queryClient = new QueryClient({
  defaultOptions: { queries: { retry: false } }
})

const wrapper = ({ children }) => (
  <QueryClientProvider client={queryClient}>
    {children}
  </QueryClientProvider>
)

describe('ArchiveList', () => {
  it('应显示加载状态', () => {
    render(<ArchiveList />, { wrapper })
    expect(screen.getByText(/加载中/)).toBeInTheDocument()
  })
  
  it('应正确渲染档案列表', async () => {
    // Mock API 响应
    server.use(
      rest.get('/api/archive', (req, res, ctx) => {
        return res(ctx.json({
          content: [
            { id: '1', title: '测试档案1', archivalCode: 'TEST-001' },
            { id: '2', title: '测试档案2', archivalCode: 'TEST-002' }
          ],
          totalElements: 2
        }))
      })
    )
    
    render(<ArchiveList />, { wrapper })
    
    await waitFor(() => {
      expect(screen.getByText('测试档案1')).toBeInTheDocument()
      expect(screen.getByText('测试档案2')).toBeInTheDocument()
    })
  })
})
```

---

### 4.4 性能优化验证

| 序号 | 任务 | 指标 | 验收标准 |
|------|------|------|----------|
| 4.4.1 | 数据库查询分析 | 慢查询 < 100ms | EXPLAIN 无全表扫描 |
| 4.4.2 | API 响应时间 | P95 < 500ms | 压测验证 |
| 4.4.3 | 分页优化 | 大数据量分页 | 10万条数据分页 < 200ms |
| 4.4.4 | 文件上传 | 大文件上传 | 100MB 文件上传成功 |

**性能测试脚本：**
```bash
# 使用 wrk 进行压测
wrk -t4 -c100 -d30s http://localhost:8080/api/archive?page=0&size=20

# 使用 Apache Bench
ab -n 1000 -c 50 -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/archive?page=0&size=20
```

**数据库优化检查：**
```sql
-- 检查档案表索引
EXPLAIN ANALYZE SELECT * FROM arc_archive 
WHERE category_code = 'AC01' 
ORDER BY created_time DESC 
LIMIT 20;

-- 检查缺失索引
SELECT
    relname AS table_name,
    seq_scan,
    idx_scan,
    n_live_tup
FROM pg_stat_user_tables
WHERE seq_scan > idx_scan
AND n_live_tup > 10000;
```

---

## 🧪 验证步骤

### 1. 后端测试执行
```bash
cd nexusarchive-java
mvn test -Dtest="*Test" -DfailIfNoTests=false
mvn jacoco:report  # 生成覆盖率报告
```

### 2. 前端测试执行
```bash
npm test -- --coverage
```

### 3. 覆盖率报告
```bash
# 后端覆盖率报告位置
open nexusarchive-java/target/site/jacoco/index.html

# 前端覆盖率报告位置
open coverage/lcov-report/index.html
```

---

## 📝 完成标志

任务完成后，请在 `docs/优化计划.md` 中勾选第四阶段相关项目，并更新覆盖率数据。

---

*Agent E 任务书 - 由 Claude 于 2025-12-07 生成*
