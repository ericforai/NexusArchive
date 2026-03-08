# SEO 优化实现计划 (DigiVoucher 数凭系统)

优化系统的搜索引擎友好度与内部搜索的可访问性，即使在私有化部署环境下，标准化的 SEO 实践也是系统专业性的体现。

## 用户评审要求

- [!IMPORTANT]
- **私有化安全**：Sitemap 和 Robots.txt 仅包含公开路径（如 `/`, `/system/login`），避免泄露内网业务路径。
- **信创适配**：所有改动需保持对国产浏览器（双核架构）的良好支持。

## 拟议变更

### 1. 全局基础 SEO (index.html)

#### [MODIFY] [index.html](file:///Users/user/nexusarchive/index.html)
- 优化 `<title>` 为更具吸引力的格式。
- 精简并增强 `keywords` 和 `description`，确保包含核心关键词（单套制、DA/T 94-2022）。

---

### 2. 动态路由 Title 优化 (React SPA)

#### [NEW] [useDocumentTitle.ts](file:///Users/user/nexusarchive/src/hooks/useDocumentTitle.ts)
- 创建自定义 Hook，根据当前路由自动更新 `document.title`。

#### [MODIFY] [index.tsx](file:///Users/user/nexusarchive/src/routes/index.tsx)
- 为 `routes` 配置项添加 `title` 属性，用于标识各页面功能。

#### [MODIFY] [SystemLayout.tsx](file:///Users/user/nexusarchive/src/layouts/SystemLayout.tsx)
- 调用 `useDocumentTitle` Hook，实现页面切换时的标题实时更新。

---

### 3. 产品落地页语义化与可访问性

#### [MODIFY] [XinchuangPartnersSection.tsx](file:///Users/user/nexusarchive/src/pages/product-website/components/XinchuangPartnersSection.tsx)
- 为合作伙伴 Logo 添加 `alt` 属性，增强可访问性。

#### [MODIFY] [Footer.tsx](file:///Users/user/nexusarchive/src/pages/product-website/components/Footer.tsx)
- 使用 `<footer>` 和 `<nav>` 语义标签优化结构。

---

### 4. 技术 SEO 基础建设

#### [NEW] [robots.txt](file:///Users/user/nexusarchive/public/robots.txt)
- 规范化搜索引擎爬取规则。

#### [NEW] [sitemap.xml](file:///Users/user/nexusarchive/public/sitemap.xml)
- 建立站点地图，引导索引核心页面。

## 验证计划

### 自动化测试
- 运行 `npm run test` 确保路由逻辑未受损。
- 使用 `Lighthouse` (需手动运行) 检查 SEO 分数。

### 手动验证
1. 导航至 `/system/archives`，检查浏览器卷标是否显示“档案中心 - DigiVoucher”。
2. 查看源代码，确认各页面 `h1` 唯一且准确。
3. 验证 `robots.txt` 可通过 `http://localhost:5173/robots.txt` 访问。

---

## 专家组预审建议 (Expert Review)

### 合规专家 (Compliance Authority)
- **建议**：确保 SEO 关键词中引用的标准号（如 DA/T 94-2022）准确无误。
- **注意**：不对未公开的业务接口进行 SEO 描述，防止敏感信息通过 Meta 标签泄露。

### 信创架构师 (Xinchuang Architect)
- **建议**：Meta 标签中应通过 `renderer` 强制 360/QQ 浏览器使用 Webkit 内核，提升渲染兼容性。

### 交付专家 (Delivery Strategist)
- **建议**：私有化部署时，Sitemap 应支持通过配置文件控制是否生成，默认仅包含公共引导页。
