---
description: 标准 Bug 修复流程 - 避免盲目调试，快速定位问题
---

# 标准 Bug 修复流程 (Standard Bug Fix Workflow)

## 🚨 黄金法则
**"问题出现时，第一步是 `git diff`，而非猜测"**

---

## 阶段 1：快速定位（5分钟内）

### 1.1 检查最近变更
```bash
# 查看最近修改的文件
git diff HEAD~10 --name-only

# 查看最近 commit
git log --oneline -10

# 查看具体改动
git diff HEAD~5
```

### 1.2 判断问题类型
- **回归 Bug**（之前正常，现在不行）→ 直接看 git diff，考虑回滚
- **新功能 Bug** → 检查新增代码逻辑
- **环境问题** → 检查配置、依赖、端口

### 1.3 快速验证：回滚测试
```bash
# 如果怀疑某次 commit 导致问题
git stash
git checkout HEAD~3
# 测试是否正常
# 确认后 git checkout main && git stash pop
```

---

## 阶段 2：诊断分析

### 2.1 日志优先
```java
// 任何新增 Filter/Service 第一时间加日志
log.info("[ClassName] 入口 - {} {}", request.getMethod(), request.getRequestURI());
```

### 2.2 二分法定位
- 不要一次改多处
- 每次改 **一处**，立即测试验证
- 确认改动有效后再继续

### 2.3 API 直接测试
```bash
# 绕过前端，直接测试后端
curl -v -X POST http://localhost:8080/api/xxx -H "Content-Type: application/json" -d '{}'

# CORS 预检测试
curl -v -X OPTIONS http://localhost:8080/api/xxx -H "Origin: http://localhost:5173" -H "Access-Control-Request-Method: POST"
```

---

## 阶段 3：修复实施

### 3.1 最小改动原则
- 优先回滚到已知正常状态
- 如必须修改，只改必要的地方
- 避免"顺便优化"其他代码

### 3.2 修改前 snapshot
```bash
git stash  # 或 git commit -m "WIP: before fix"
```

### 3.3 改动后立即验证
- 每次改动后，立即 curl/浏览器 验证
- 不要积累多处改动后才测试

---

## 常见陷阱清单

| 陷阱 | 正确做法 |
|:---|:---|
| 盲目尝试多种方案 | 先理解问题根因，再动手 |
| 没看 git diff 就开始改 | 第一步永远是 `git diff` |
| 一次改多处再测试 | 改一处测一处 |
| Filter 不加日志 | 任何新 Filter 必须有入口日志 |
| 假设知道原因就开始改 | 先验证假设（日志/断点） |

---

## Spring Security 专项

### Filter 正确写法
```java
// ❌ 错误：普通 Filter 在 Security 链中不生效
public class MyFilter implements Filter { ... }

// ✅ 正确：必须继承 OncePerRequestFilter
public class MyFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(...) { ... }
}
```

### Filter 添加位置
```java
// 添加到 Security FilterChain 中，而非 Servlet 层
http.addFilterBefore(new MyFilter(), DisableEncodeUrlFilter.class);
```

### CORS 配置层级
```
Servlet Filter → Spring Security FilterChain → Controller
                 ↑ CORS 必须在这里处理！
```

---

## 快速检查清单

修复 Bug 前，回答以下问题：

- [ ] 是否检查了 `git diff`？
- [ ] 是否知道问题首次出现的时间？
- [ ] 是否有足够的日志来追踪问题？
- [ ] 是否可以通过回滚快速验证？
- [ ] 改动范围是否最小化？
