# NexusArchive - 电子会计档案系统

一个基于 React + TypeScript + Vite 构建的下一代电子会计档案管理系统，支持 AI 驱动的 OCR、智能关联和四性检测。

## 功能特性

### 预归档库
- **电子凭证池** - 统一管理来自各业务系统的凭证
- **OCR识别** - 批量识别和处理文档
- **凭证关联** - 支持一对多、多对多自动关联
  - 可配置的关联规则
  - 权重调整和规则管理
  - 关联关系可视化

### 系统设置
- **定时任务管理** - 配置自动执行的任务（每天/每周/每月/间隔）
- **消息提醒规则** - 配置系统通知规则和触发条件

## 快速开始

### 本地开发

```bash
# 安装依赖
npm install

# 启动开发服务器
npm run dev

# 访问 http://localhost:5173
```

### 部署到 Vercel

#### 方法一：使用 Vercel CLI

```bash
# 安装 Vercel CLI
npm i -g vercel

# 登录
vercel login

# 部署
vercel

# 生产环境部署
vercel --prod
```

#### 方法二：通过 GitHub

1. 将代码推送到 GitHub
2. 在 [Vercel Dashboard](https://vercel.com/dashboard) 导入项目
3. 自动部署完成

详细部署说明请查看 [部署指南.md](./部署指南.md)

## 技术栈

- **框架**: React 19 + TypeScript
- **构建工具**: Vite 6
- **UI 框架**: Tailwind CSS
- **图标**: Lucide React
- **图表**: Recharts

## 项目结构

```
nexusarchive/
├── components/          # React 组件
│   ├── Dashboard.tsx
│   ├── ArchiveListView.tsx
│   ├── ScheduledTaskManager.tsx
│   └── ...
├── utils/              # 工具函数
│   ├── taskScheduler.ts
│   └── notificationService.ts
├── types.ts            # TypeScript 类型定义
├── constants.tsx       # 常量配置
├── App.tsx             # 主应用组件
└── vite.config.ts      # Vite 配置
```

## 环境变量

创建 `.env.local` 文件（可选）：

```env
GEMINI_API_KEY=your_api_key_here
```

## 数据存储

系统使用浏览器 localStorage 存储：
- 关联规则配置
- 定时任务配置
- 消息提醒规则
- 通知历史

⚠️ **注意**: 清除浏览器缓存会丢失所有本地数据。

## 开发说明

- 开发服务器端口: `5173`
- 构建输出目录: `dist`
- 支持热模块替换 (HMR)

## 许可证

MIT
