# 登录闪退问题修复说明

> **⚠️ 历史文档** - 本文档记录的是历史问题和修复方案，其中使用的命令已更新。
>
> **当前启动方式**: `npm run dev` / `npm run dev:stop`
>
> **如遇到类似问题**, 请参考：[开发环境指南](../deployment/docker-development.md)

---

## 问题描述

用户登录后出现闪退（立即跳转回登录页）。

## 根本原因

`FondsContextFilter` 在处理多全宗权限用户时，如果请求中没有指定 `X-Fonds-No` 请求头，且用户有多个全宗权限（大于1个），会返回 `null`，导致过滤器拒绝请求（403 Forbidden），前端检测到错误后触发登出，导致"闪退"。

## 修复方案

已修改 `FondsContextFilter.resolveCurrentFonds()` 方法：

**文件**: `nexusarchive-java/src/main/java/com/nexusarchive/config/FondsContextFilter.java`

**修改前**:
```java
if (allowedFonds.size() == 1) {
    return allowedFonds.get(0);
}
return null; // 如果有多个全宗，返回null，导致拒绝请求
```

**修改后**:
```java
// 如果没有指定全宗号，默认使用第一个全宗（避免登录后跳转失败）
// 前端可以通过 X-Fonds-No 请求头指定具体使用的全宗号
if (!allowedFonds.isEmpty()) {
    return allowedFonds.get(0);
}
return null;
```

## 应用修复

由于后端使用JAR包运行，需要重新构建镜像：

```bash
# 方式一：使用 npm 脚本（推荐）
npm run dev:stop
npm run dev

# 方式二：手动重启
# 停止后端进程
pkill -f "spring-boot:run"

# 重新启动后端
cd nexusarchive-java
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

> **注意**: 旧的 `docker-compose.dev.yml` 已不再使用。开发环境现在采用混合模式（Docker 基础设施 + 本地应用）。

## 验证修复

1. 重新构建并启动后端
2. 清除浏览器缓存
3. 重新登录系统
4. 应该能够正常进入系统，不再闪退

## 相关文件

- 修复代码: `nexusarchive-java/src/main/java/com/nexusarchive/config/FondsContextFilter.java`
- 用户权限修复: `nexusarchive-java/src/main/resources/db/migration/V84__fix_user_fonds_scope_for_existing_data.sql`

