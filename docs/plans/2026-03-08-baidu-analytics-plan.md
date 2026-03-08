# 实现计划：集成百度统计

本计划旨在将百度统计脚本集成到 DigiVoucher 系统的 `index.html` 中，以实现全站访问分析。

## 拟议更改

### 核心前端

#### [修改] [index.html](file:///Users/user/nexusarchive/index.html)

- **不再** 直接在 `index.html` 中硬编码脚本，以符合私有化部署的安全性要求。

#### [新建] [BaiduAnalytics.tsx](file:///Users/user/nexusarchive/src/components/common/BaiduAnalytics.tsx) [NEW]

- 创建一个专门的组件用于按需加载百度统计脚本。
- 逻辑：通过环境变量 `VITE_ENABLE_BAIDU_ANALYTICS` 控制开关。

#### [修改] [routes/index.tsx](file:///Users/user/nexusarchive/src/routes/index.tsx)

- 在 `/` (ProductWebsite) 和 `/system/login` 路由中包裹 `BaiduAnalytics` 组件。

## 验证计划

### 手动验证
- 启动前端开发服务器：`npm run dev`
- 使用浏览器访问首页，查看源代码确认脚本已存在。
- 在浏览器控制台中检查是否能够访问 `_hmt` 对象。
- 确认统计请求是否已发出（检查网络请求请求 `hm.js` 和相关的统计数据包 `hm.gif`）。
