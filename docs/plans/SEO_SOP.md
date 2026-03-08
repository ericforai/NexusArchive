# DigiVoucher 官网 SEO 执行标准 (B2 SOP)

本标准定义了 DigiVoucher 官网内容增长的操作规范，作为“内容集群 (Hub-Cluster)”策略的落地执行蓝图。

## 1. 页面元数据标准 (TDK)
- **Title 格式**：`[关键词/标题] | DigiVoucher 数凭 - [支柱名称]`
  - 示例：`标准解读: DA/T 94-2022 | DigiVoucher 数凭 - 知识库`
- **Description 长度**：控制在 80-160 字符（40-80 汉字）。
- **Keywords**：每页 3-5 个核心词，必须包含该页面的语义主题。

## 2. 结构化 HTML 规范
- **H1 标签**：仅限文章标题或 Banner 主标题，全页唯一。
- **H2 标签**：章节标题，如“1. 什么是单套制归档”。
- **H3 标签**：小节标题。
- **Img Alt**：所有行业案例 Logo 及示意图必须包含简洁的 `alt` 描述（如 `alt="制造业电子凭证归档方案架构图"`）。

## 3. 内链闭环规则 (Hub-Cluster Linking)
- **支柱回引**：每篇 Cluster 文章必须在**首段**及**末段**包含指回 Pillar Page（解决方案页）的锚文本。
- **横向关联**：相关文章之间应有“猜你喜欢”或“延伸阅读”模块，增加页面停留时间与爬取深度。
- **Schema 嵌入**：使用 JSON-LD 嵌入 `Article` 和 `BreadcrumbList` 模式。

## 4. 交付与更新
- 新增页面必须同步更新 `public/sitemap.xml`。
- 私有化环境下，知识库内容随包同步，建议定期导出 PDF/OFD 版以便线下载入。
