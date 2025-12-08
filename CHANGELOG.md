# 更新日志 (Changelog)

本文件记录 NexusArchive 电子会计档案系统的版本更新历史。

---

## [2025-12-07] 文档重构

### 变更
- 重构 `docs/` 目录结构，按类型分类文档
- 精简 `README.md`，详细内容迁移至专门文档
- 新增 `docs/README.md` 作为文档导航中心

---

## [2025-12-06] 前端路由架构重构 ⭐ 重大更新

### 核心变更
- **路由架构升级**: 采用 `createBrowserRouter` + 嵌套路由，实现真正的 URL 驱动导航
- **支持深层链接**: 所有模块现在可通过 URL 直接访问（如 `/system/archive/vouchers`）
- **刷新保持位置**: 页面刷新后保持当前位置，不再回退到门户首页
- **浏览器历史支持**: 完整支持前进/后退按钮
- **登录后跳转**: 未登录访问受保护路由时，登录后自动跳转回原页面

### 新增文件
| 文件 | 说明 |
|------|------|
| `src/routes/index.tsx` | 路由配置中心，定义完整嵌套路由和懒加载 |
| `src/routes/paths.ts` | 路由路径常量，独立文件避免循环依赖 |
| `src/layouts/SystemLayout.tsx` | 系统布局组件，包含 Sidebar + TopBar + Outlet |
| `src/components/auth/ProtectedRoute.tsx` | 路由守卫，处理认证和登录跳转 |

### 修改文件
| 文件 | 变更 |
|------|------|
| `src/App.tsx` | 使用 `RouterProvider` 替代传统 `Routes` |
| `src/components/Sidebar.tsx` | 使用 `NavLink` 替代状态切换，自动高亮当前路由 |
| `src/components/LoginView.tsx` | 支持 `useLocation` 获取跳转目标，登录后自动跳转 |
| `src/components/Dashboard.tsx` | 使用 `useNavigate` 替代 `onNavigate` prop |
| `src/components/ArchiveListView.tsx` | 支持 `routeConfig` 参数，自动解析模块配置 |
| `src/components/BorrowingView.tsx` | 添加缺失的图标导入 (Download, FileSpreadsheet) |

### 路由映射表
```
/system                         → 门户首页 (Dashboard)
/system/panorama/:id?           → 全景视图
/system/pre-archive/*           → 预归档库
/system/collection/*            → 资料收集
/system/archive/*               → 档案管理（会计凭证/账簿/报告/装盒/组卷等）
/system/query/*                 → 档案查询
/system/borrowing               → 档案借阅
/system/destruction             → 档案销毁
/system/warehouse/*             → 库房管理
/system/stats/*                 → 数据统计
/system/settings                → 系统设置
/system/admin/*                 → 后台管理
/system/login                   → 登录页（独立于布局）
```

### 技术亮点
- **懒加载优化**: 所有功能模块使用 `React.lazy()` 实现按需加载
- **循环依赖避免**: 路由常量独立到 `paths.ts`，打破 routes ↔ Sidebar 循环引用
- **向后兼容**: `ArchiveListView` 同时支持传统 `config` props 和新的 `routeConfig` 字符串

### 其他更新
- **接口权限兜底**：统一后台角色判断为 `SYSTEM_ADMIN` 并补充 `nav:all` 权限
- **登录流与异常**：增强 `RestAccessDeniedHandler` 与 `GlobalExceptionHandler`

---

## [2025-12-05] 用友云凭证分录同步

### 新增
- 自动同步凭证的完整分录信息，包括摘要和科目
- 分录数据存储于 `custom_metadata` 字段 (PostgreSQL JSONB 类型)
- 导出 AIP 包时展示真实的业务摘要和科目名称

### 修复
- **Connection Refused 问题**: 添加 JVM 参数 `-Djava.net.useSystemProxies=false` 禁用系统代理
- **JSONB 类型不匹配问题**: 创建 `PostgresJsonTypeHandler.java` 正确处理 JSONB 类型转换

---

## 第三阶段：私有化部署产品化改造（已完成）

详情见 [phase3-private-deployment-summary.md](/docs/planning/phase3-private-deployment-summary.md)

### 变更
- **用户体验优化**：创建面向非技术用户的管理控制台和简化界面
- **自动化运维**：实现全面的系统健康监控、自动备份恢复
- **便捷部署交付**：提供一键安装脚本、离线安装包

---

## 第二阶段：架构优化（已完成）

详情见 [phase2-implementation-summary.md](/docs/planning/phase2-implementation-summary.md)

### 变更
- **架构重构**：确立"高性能模块化单体"架构
- **性能提升**：集成 Redis 高性能缓存
- **信创安全**：落地 SM4 国密算法字段级加密
- **监控告警**：使用 Prometheus 和 Grafana 实现系统监控

---

## 第一阶段：代码质量审查优化（已完成）

详情见 [phase1-implementation-summary.md](/docs/planning/phase1-implementation-summary.md)

### 变更
- **会计合规性增强**：新增金额精度校验、标准化XML报告生成器
- **四性检测增强**：集成金额校验，完善审计日志记录
- **安全增强**：实现电子签名验证服务
- **审计完整性**：增强审计日志记录
