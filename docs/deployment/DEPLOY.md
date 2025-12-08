# Vercel 部署指南

## 快速部署步骤

### 方法一：通过 Vercel CLI（推荐）

1. **安装 Vercel CLI**
   ```bash
   npm i -g vercel
   ```

2. **登录 Vercel**
   ```bash
   vercel login
   ```

3. **在项目根目录部署**
   ```bash
   vercel
   ```

4. **生产环境部署**
   ```bash
   vercel --prod
   ```

### 方法二：通过 Vercel 网站

1. **访问 [vercel.com](https://vercel.com)** 并登录

2. **导入项目**
   - 点击 "Add New" → "Project"
   - 连接你的 Git 仓库（GitHub/GitLab/Bitbucket）
   - 或直接拖拽项目文件夹

3. **配置项目**
   - Framework Preset: **Vite**
   - Build Command: `npm run build`（自动检测）
   - Output Directory: `dist`（自动检测）
   - Install Command: `npm install`（自动检测）

4. **环境变量（如需要）**
   - 在项目设置中添加环境变量
   - 例如：`GEMINI_API_KEY`（如果使用）

5. **部署**
   - 点击 "Deploy"
   - 等待构建完成

## 项目配置说明

### vercel.json
项目已包含 `vercel.json` 配置文件：
- **框架**: Vite（自动检测）
- **构建命令**: `npm run build`
- **输出目录**: `dist`
- **路由重写**: 所有路由重定向到 `index.html`（支持 SPA 路由）
- **缓存策略**: 静态资源缓存 1 年

### 构建要求

- **Node.js 版本**: 建议 18.x 或更高
- **包管理器**: npm（默认）或 yarn/pnpm

## 部署后检查

1. **访问部署的 URL**
   - Vercel 会提供一个 `*.vercel.app` 域名
   - 可以自定义域名

2. **检查功能**
   - 确认所有页面正常加载
   - 检查路由导航是否正常
   - 验证静态资源加载

## 常见问题

### 1. 构建失败
- 检查 `package.json` 中的依赖是否正确
- 确认 Node.js 版本兼容
- 查看 Vercel 构建日志

### 2. 路由 404
- 确认 `vercel.json` 中的 `rewrites` 配置正确
- 检查是否为 SPA 应用

### 3. 环境变量
- 在 Vercel 项目设置中添加环境变量
- 重新部署以应用更改

## 自动部署

连接 Git 仓库后，Vercel 会自动：
- 监听 `main`/`master` 分支的推送
- 自动触发构建和部署
- 为每个 PR 创建预览部署

## 自定义域名

1. 在 Vercel 项目设置中
2. 进入 "Domains"
3. 添加你的域名
4. 按照提示配置 DNS

## 性能优化

项目已配置：
- ✅ 静态资源长期缓存
- ✅ SPA 路由支持
- ✅ Vite 生产构建优化

## 技术支持

如遇问题，查看：
- [Vercel 文档](https://vercel.com/docs)
- [Vite 部署指南](https://vitejs.dev/guide/static-deploy.html)


