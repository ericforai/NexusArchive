# 法律法规深度重构与导航修复实施计划

## 1. 目标描述
针对用户反馈的“导航失效（跳转到首页）”以及“内容简单无价值”问题，进行专项修复与内容升维。引入虚拟专家组视角，确保文章不仅具备 SEO 价值，更能为潜在客户提供真实的业务参考和技术指导。

## 2. 拟定更改

### [路由与导航]
#### [MODIFY] [routes/index.tsx](file:///Users/user/nexusarchive/src/routes/index.tsx)
- 注册 `/blog/single-set-system-implementation` 路由。
- 引入对应的 `SingleSetImplementation` 懒加载组件。

### [业务组件]
#### [NEW] [SingleSetImplementation.tsx](file:///Users/user/nexusarchive/src/pages/product-website/blog/SingleSetImplementation.tsx)
- 创建全新的单套制实施指南。
- 内容侧重：纸电转换难点、电子原件法律效力保全、大型企业（如金融/烟草）试点案例拆解。

#### [MODIFY] [DAT92Interpretation.tsx](file:///Users/user/nexusarchive/src/pages/product-website/blog/DAT92Interpretation.tsx)
- 引入虚拟专家组视角：
    - **合规视角**：解析 2024 会计法修正案对不合规归档的处罚红线。
    - **交付视角**：给出从“手工归档”到“自动归档”的 3 阶段演进图谱。

#### [MODIFY] [DAT95Interpretation.tsx](file:///Users/user/nexusarchive/src/pages/product-website/blog/DAT95Interpretation.tsx)
- 引入虚拟专家组视角：
    - **技术视角**：解析数电发票 XML 与 OFD 的解析精度要求（防止 OCR 识别误差导致的账务错误）。
    - **安全视角**：详解重复报销检测的实时高并发处理逻辑。

#### [MODIFY] [GBT18894Interpretation.tsx](file:///Users/user/nexusarchive/src/pages/product-website/blog/GBT18894Interpretation.tsx)
- 引入虚拟专家组视角：
    - **架构视角**：基于 GB/T 18894 的“全生命周期”设计，重新梳理 DigiVoucher 的后端 Service 演进。
    - **信创视角**：国产中间件（中创/金蝶天燕）下的归档性能调优建议。

## 3. 验证计划

### 自动化测试
- 无（本项目多为 UI/内容侧变更）。

### 手动验证
- 启动 `npm run dev`。
- 在博客列表页点击“企业单套制归档实施指南”，验证是否进入正确页面（不再跳回首页）。
- 检查所有 H1 标签是否包含核心法规名称。
- 模拟信创专家视角，检查文章中关于 SM3 算法、OFD、国产数据库s的描述准确性。

> [!WARNING]
> 本次任务严禁执行 `git push`。所有修改结果请通过 artifact 阅读。
