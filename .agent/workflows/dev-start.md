---
description: 开发环境启动流程 - 确保后端先于前端启动
---

# 开发环境启动工作流

## ⚠️ 重要：启动顺序

**后端必须先于前端完全启动！** 否则前端API请求将因 `ECONNREFUSED` 而失败，返回500错误。

## 启动步骤

### 方式一：使用启动脚本（推荐）
// turbo
```bash
cd /Users/user/nexusarchive
./scripts/dev-start.sh
```

### 方式二：手动启动

1. **启动后端**（终端1）：
```bash
cd nexusarchive-java
mvn spring-boot:run
```

2. **等待后端就绪** - 确认看到以下日志：
```
Started NexusArchiveApplication in X.XXX seconds
```
或检查健康端点：
```bash
curl -s http://localhost:8080/api/auth/login -X POST 2>/dev/null && echo "Backend ready!"
```

3. **启动前端**（终端2）：
```bash
cd /Users/user/nexusarchive
npm run dev
```

4. **访问应用**：
```
http://localhost:5173
```

## 常见问题

### 前端报500错误
**原因**：后端未启动或未完全就绪
**解决**：
1. 检查后端是否运行：`lsof -i :8080`
2. 如果未运行，先启动后端
3. 刷新前端页面

### 端口被占用
```bash
# 释放8080端口
lsof -ti :8080 | xargs kill -9

# 释放5173端口
lsof -ti :5173 | xargs kill -9
```

## 验证服务状态

```bash
# 检查后端
curl -s http://localhost:8080/api/auth/login -X POST -w "\nHTTP: %{http_code}" 2>/dev/null

# 检查前端代理
curl -s http://localhost:5173/api/auth/login -X POST -w "\nHTTP: %{http_code}" 2>/dev/null
```
