# 2026-03-15 OFD 预览故障深度排障报告 (Root Cause Analysis)

## 1. 问题背景
本地环境预览正常，服务器端（115.190.237.184）通过域名访问时，OFD 预览报 404 错误或 10s 超时挂起。

## 2. 根因剥洋葱 (Root Cause Onion)

### Layer 1: API 路径冗余
- **现象**：请求路径出现 `/api/api/` 重叠。
- **根因**：Axios 的 `baseURL` 与代码手动拼接的路径冲突。
- **修复**：在 `client.ts` 引入 `[OFD-FIX-FINAL-V4]` 拦截器，强制剔除重复前缀。

### Layer 2: 僵尸代码拦截
- **现象**：修改代码并部署后，控制台日志显示的组件版本依然是 2025 年。
- **根因**：Nginx 实际 `root` 指向了 `/opt/nexusarchive/dist`，而部署脚本上传到了 `/usr/share/nginx/html`。
- **修复**：统一 Nginx `root` 路径为 `/usr/share/nginx/html`，并建立物理指纹文件 `fingerprint.txt` 进行实时验证。

### Layer 3: 301 强缓存陷阱
- **现象**：修改 Nginx 后，通过 IP 访问依然跳转到域名首页。
- **根因**：Nginx 之前配置过 301 永久重定向，浏览器在本地记住了该跳转，不再向服务器发起新请求。
- **修复**：在 URL 后添加 `?t=random` 参数强行绕过浏览器缓存，并清理 Nginx 冗余重定向配置。

### Layer 4: 深层路由下的资源 404 (关键点)
- **现象**：字体文件存在但 `liteofd` 报 10s 解析超时。
- **根因**：SPA 嵌套路由（如 `/system/pre-archive/doc-pool`）导致渲染引擎尝试从相对路径加载资源失败。Nginx 的 `root` 指令由于配置了错误的 `/assets` 偏移，导致资源查找路径错位。
- **修复**：在 Nginx 使用 `location ~* /(fonts|assets)/` 全局匹配，并使用标准化路径重定向。

## 3. 运维沉淀 (DevOps Lessons)
1. **大小写兼容**：Linux 下必须通过软链接（`ln -sf`）为所有字体文件建立全小写、全大写、首字母大写的映射。
2. **MIME 类型**：JS 模块必须返回 `application/javascript`，否则会被浏览器拒绝执行。
3. **物理同步**：第三方库（如 `liteofd`）的 Worker 脚本必须在根目录保留副本，以应对复杂的相对路径寻找。
