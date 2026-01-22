# 任务清单：统一预归档库重构与强校验方案

## 阶段 1：基础定义与后端准备 [x]
- [x] 创建全局门类定义文件 `src/constants/archivalCategories.ts`
- [x] 编写数据库迁移脚本 `V2026012101__refactor_archival_category.sql`
- [x] 修改后端 `OriginalVoucher` 实体，增加字段及 `@NotBlank` 校验
- [x] 更新 `BatchToArchiveService` 以解析门类元数据

## 阶段 2：前端 Collection 模块增强 [x]
- [x] 重构 `BatchUploadView.tsx` 中的门类选择逻辑
- [x] 实现显性的强校验 Alert 提示组件
- [x] 增加上传前的表单完整性校验（强制选择门类）

## 阶段 3：前端 Pre-Archive 池重构 [x]
- [x] 重构 `PoolPage.tsx` 获取数据的筛选逻辑，实现带图标的门类筛选器
- [x] 在列表项中集成轻量化图标（lucide-react）
- [x] 简化 `ArchiveDetailDrawer.tsx` 的预览引擎逻辑
- [x] 移除 `ArchiveListView.tsx` 等页面的旧上传按钮，改为“前往资料收集”引导

## 阶段 4：验证与交付 [/]
- [/] 执行浏览器端 E2E 全流程闭环验证（上传-筛选-预览） @进行中
- [ ] 完成 4 个门类的全流程上传-预览闭环验证
- [ ] 更新项目文档及 CHANGELOG
