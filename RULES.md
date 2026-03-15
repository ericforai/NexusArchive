# RULES.md

## 项目协作规则

本文件定义 `NexusArchive` 项目的长期协作规则。

目标只有三个：
- 保持 `main` 干净
- 保持任务状态和代码状态一致
- 避免 `Linear + Symphony` 带来长期残留分支和工作区

## 核心原则

### 1. `main` 是唯一事实源

- 只有根仓库的 `main` 代表当前真实、可交付代码状态
- 任何功能、修复、重构，只有在合并到 `main` 后才算完成
- 任何已完成 issue，不允许只停留在 Symphony 工作区或功能分支中

### 2. `Linear` 管任务，不管代码真相

- `Linear` 负责管理任务优先级、状态、父子关系、评审节奏
- `Linear` 状态不能替代代码状态
- 判断“是否完成”，以代码是否进入 `main` 为准

### 3. `Symphony` 只做临时隔离开发

- 一个 issue 只对应一个 Symphony 工作区
- 一个 Symphony 工作区只服务一个分支
- Symphony 工作区是临时施工现场，不是长期代码存放区
- 已完成 issue 的 Symphony 工作区必须删除

## 标准流程

### 1. 开始开发前

- 在 `Linear` 中确认 issue 已经可以进入开发
- 仅为当前活跃 issue 创建 Symphony 工作区
- 不在主工作区直接堆积未隔离的功能开发

### 2. 开发中

- 所有功能实现、调试、定向测试、review 先在 Symphony 工作区完成
- 分支命名、PR、测试结果必须和当前 issue 对应
- 不允许一个工作区承载多个无关 issue

### 3. 进入评审

- issue 进入 `In Review` 前，必须具备：
- 代码已推送到对应分支
- 已完成定向验证
- 已完成代码 review
- 已建立 PR 或具备合并条件

### 4. 合并收口

issue 完成后必须同时完成以下动作：
- 合并到 `main`
- 将 `Linear` 状态改为 `Done`
- 删除功能分支
- 删除 Symphony 工作区
- 将本地主仓库 fast-forward 到最新 `origin/main`

以上任一步未完成，都不算真正收口。

## 状态流转规则

- `Backlog`：尚未准备开始
- `Todo`：已确认可开始开发
- `In Progress`：已创建工作区并开始编码
- `In Review`：代码已完成，等待 review / CI / merge
- `Done`：代码已进入 `main`，并完成分支与工作区清理

## 清理规则

### 1. 分支清理

- 已合并到 `main` 的功能分支必须删除
- 已失去工程价值的历史分支应删除，不作为长期存档
- 任何“看起来可能还有用”的旧分支，都应先判断其价值是否已被 `main` 吸收

### 2. 工作区清理

- Symphony 中只允许保留当前活跃 issue 的工作区
- 已 `Done` 的 issue 不允许继续保留工作区
- 已完成工作区必须删除，避免造成“代码还没进主线”的错觉

### 3. 主仓库清理

- 主仓库默认应保持干净
- 无关临时文档、产物、实验脚本不应长期滞留
- 若文件不是交付资产，应删除或移出版本管理

## CTO 级执行要求

如果把本项目当作正式研发流程，必须遵守以下管理口径：

- `main` 是唯一可交付状态
- Symphony 是开发隔离层，不是资产层
- Linear 是节奏层，不是代码层
- 合并后的残留工作区、分支、临时文件，视为流程未完成

## 推荐执行方式

每次新任务按以下顺序执行：

1. 在 `Linear` 中确认 issue 状态
2. 创建对应 Symphony 工作区
3. 在工作区开发和验证
4. 发起 review / PR
5. 合并到 `main`
6. 同步本地主仓库 `main`
7. 删除功能分支和 Symphony 工作区

## 反模式

以下行为应视为流程问题：

- issue 已 `Done`，但代码还不在 `main`
- 功能已合并，但工作区和分支长期残留
- 把 Symphony 工作区当成长期开发仓库
- 用 `Linear` 状态代替代码真实状态
- 无法判断主线代码、工作区代码、远端分支代码谁才是最新版本

## 最终原则

- `Linear` 管任务，不管代码真相
- `Symphony` 管隔离开发，不做长期存档
- `main` 才是唯一可交付状态

---

## 生产环境运维与部署安全 (2026-03-15 新增)

### 1. 环境指纹校验 (Environment Fingerprinting)
- 每次发布生产环境后，必须通过浏览器验证物理文件：`http://domain/fingerprint.txt`。
- 指纹内容必须包含构建时间戳或序列号（如 `V4_FINAL_FORCE`）。
- **目的**：杜绝 Nginx 路由配置错误导致加载 404 或陈旧的“僵尸代码”。

### 2. 静态资源路径深度防御 (Path-Agnostic Assets)
- 针对使用 Web Worker 或动态加载字体的第三方库（如 `liteofd`, `pdf.js`），Nginx 配置必须包含**全路径正则捕获**逻辑：
  ```nginx
  location ~* .*/(assets|fonts|public)/(.+)$ {
      alias /usr/share/nginx/html/$1/$2;
  }
  ```
- **目的**：防止 SPA 嵌套路由（Deep Routing）导致的资源相对路径请求失败。

### 3. 文件名大小写冗余映射 (Filename Resilience)
- 服务器端的字体资产（.ttf, .otf）必须通过脚本建立全小写软链接映射。
- **目的**：兼容不同第三方库在大小写敏感系统（Linux）下对文件名的不一致引用。

### 4. MIME 类型严谨性声明
- 必须确保 `include mime.types;` 在 SSL server 块中生效。
- 显式为 `.js` 定义 `application/javascript`，为字体定义 `font/ttf`，防止现代浏览器因 MIME 检查失败而拦截模块加载。
