// Input: API 客户端定义
// Output: 极简架构说明
// Pos: src/api/README.md
// 一旦我所属的文件夹有所变化，请更新我。

# API 调用层 (API)

本目录封装了所有与后端交互的 HTTP 请求。

## 目录结构

- `admin.ts`: 系统管理接口（用户、角色、组织等）。
- `archives.ts`: 核心档案操作接口。
- `archiveBatch.ts`: 归档批次管理接口（含批量审批）。
- `archiveApproval.ts`: 归档审批流程接口（含批量审批）。
- `abnormal.ts`: 异常档案处理接口。
- `attachments.ts`: 附件管理接口。
- `audit.ts`: 审计日志接口。
- `auth.ts`: 认证与登录接口。
- `autoAssociation.ts`: 自动关联规则接口。
- `borrowing.ts`: 借阅管理接口。
- `client.ts`: Axios 实例配置（拦截器、Token 处理）。
- `destruction.ts`: 档案销毁接口（含批量审批）。
- `erp.ts`: ERP 集成配置接口。
- `fonds.ts`: 全宗管理接口。
- `fondsHistory.ts`: 全宗沿革管理接口（迁移、合并、分立、重命名）。
- `entity.ts`: 法人实体管理接口（CRUD、法人与全宗关联）。
- `entityConfig.ts`: 法人配置管理接口（为每个法人设置独立配置）。
- `enterpriseArchitecture.ts`: 集团架构树视图接口（获取"法人 -> 全宗 -> 档案"层级数据）。
- `authTicket.ts`: 跨全宗访问授权票据接口（申请、审批、撤销）。
- `license.ts`: 系统授权接口。
- `matching.ts`: 智能匹配接口。
- `nav.ts`: 导航菜单接口。
- `notifications.ts`: 通知消息接口。
- `openAppraisal.ts`: 开放鉴定接口。
- `originalVoucher.ts`: 原始凭证接口。
- `pool.ts`: 归档池接口。
- `search.ts`: 全文检索接口。
- `stats.ts`: 统计分析接口。
- `warehouse.ts`: 库房管理接口。
- `workflow.ts`: 工作流引擎接口。

## 规范

1. **强类型**: 每个 API 调用都必须定义请求参数和响应数据的 TypeScript 类型。
2. **无副作用**: API 层仅负责请求发送，不应包含业务逻辑或 UI 弹窗处理。
