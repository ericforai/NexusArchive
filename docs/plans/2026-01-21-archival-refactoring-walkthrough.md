# 凭证池门类重构与强校验交付报告 (Walkthrough)

本报告总结了对电子凭证池进行的“强门类校验”与“多维维度筛选”重构工作的完成情况。

## 1. 核心改进概览

通过本次重构，我们成功将“资料收集”与“预归档处理”两个阶段进行了业务解耦，确保了档案数据的合规性。

### 1.1 强门类校验逻辑 (Collection)
- **强制选择**：在上传阶段引入了档案门类的必选项，防止产生“孤儿”数据。
- **合规提示**：基于 `src/constants/archivalCategories.ts` 动态展示不同门类的审计与合规说明。
- **后端持久化**：统一了 `OriginalVoucher` 与 `CollectionBatch` 的元数据定义。

### 1.2 多维筛选与图标化 (Pre-Archive Pool)
- **交互升级**：在仪表板集成了基于图标的维度筛选器，支持按“凭证、账簿、报表、其他”进行快速切片展示。
- **性能优化**：引入 `useArchiveDataLoader` 的前端缓存过滤机制，大幅减少了筛选时的 API 往返。

### 1.3 智简预览引擎 (Detail View)
- **动态 Tabs**：根据门类自动调整抽屉内容。凭证门类展示完整可视化，非凭证门类（如报表、其他资料）精简为“元数据+附件预览”模式。

### 1.4 导航体系对齐 (Navigation Alignment)
- **新增菜单**：在“预归档库”下新增了“其他会计资料”二级菜单，实现了从 Collection 上传门类到 Pre-Archive 管理频道的闭环对齐。
- **专场预览**：针对不同门类提供了定制化的统计仪表板，自动隐藏无关维度，提高操作聚焦度。

## 2. 代码变更速览

### 后端变更
- [Entity] [OriginalVoucher.java](file:///Users/user/nexusarchive/nexusarchive-java/src/main/java/com/nexusarchive/entity/OriginalVoucher.java): 字段重命名及 `@NotBlank` 增强。
- [Migration] [V2026012101__refactor_archival_category.sql](file:///Users/user/nexusarchive/nexusarchive-java/src/main/resources/db/migration/V2026012101__refactor_archival_category.sql): 完成存量数据平滑迁移。

### 前端变更
- [Constants] [archivalCategories.ts](file:///Users/user/nexusarchive/src/constants/archivalCategories.ts): 建立了全系统唯一的分类标准字典。
- [Components] [PoolDashboard.tsx](file:///Users/user/nexusarchive/src/components/pool-dashboard/PoolDashboard.tsx): 实现图标式多维筛选交互。
- [Hooks] [useArchiveDataLoader.ts](file:///Users/user/nexusarchive/src/features/archives/controllers/useArchiveDataLoader.ts): 核心异步加载逻辑重构，支持门类透传。

## 3. 验证结果 (E2E)

我们已通过浏览器自动化工具对全流程进行了两轮深度验证，确保链路通畅。

### 3.1 视频演示
![E2E 全流程验证录像](/Users/user/.gemini/antigravity/brain/5ccb73d4-54bc-4109-84b7-2b0f9c63018d/e2e_archival_flow_verification_round_2_1768963466329.webp)
*视频展示了从资料收集到凭证池维度筛选，再到详情预览的完整交互。*

### 3.2 关键截图
````carousel
![维度筛选 UI](/Users/user/.gemini/antigravity/brain/5ccb73d4-54bc-4109-84b7-2b0f9c63018d/.system_generated/click_feedback/click_feedback_1768963494117.png)
<!-- slide -->
![分类强校验提示](/Users/user/.gemini/antigravity/brain/5ccb73d4-54bc-4109-84b7-2b0f9c63018d/.system_generated/click_feedback/click_feedback_1768963125754.png)
<!-- slide -->
![其他会计资料专用页](/Users/user/.gemini/antigravity/brain/5ccb73d4-54bc-4109-84b7-2b0f9c63018d/other_materials_page_check_1768964805880.png)
<!-- slide -->
![智简预览展示](/Users/user/.gemini/antigravity/brain/5ccb73d4-54bc-4109-84b7-2b0f9c63018d/.system_generated/click_feedback/click_feedback_1768963594956.png)
````

### 3.3 场景闭环确认
1. **会计凭证 (VOUCHER/AC01)**：验证点击筛选生效，详情页展示“会计凭证”可视化 Tab。
2. **非凭证门类 (REPORT/LEDGER)**：验证点击筛选生效，详情页**自动隐藏**“会计凭证”Tab，减少视觉干扰。
3. **其他会计资料 (OTHER/AC04)**：实现了**专项菜单对齐**。用户可直接在“预归档库 > 其他会计资料”中查看到从采集端上送的所有杂项档案。
4. **异常保护**：已修复 `ArchiveDetailDrawer` 对 `null` 对象的访问崩溃，系统具备稳健的空状态处理能力。

> [!NOTE]
> 本次变更同步移除了 `ArchiveListView` 中冗余的内联上传按钮，引导用户通过标准的“资料收集”模块进行合规采集。

---
*交付日期：2026-01-21*
