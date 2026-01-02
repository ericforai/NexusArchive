# 登录闪退问题修复说明

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
# 重新构建后端镜像
cd nexusarchive-java
docker-compose -f ../docker-compose.dev.yml build nexus-backend

# 重启后端服务
docker-compose -f ../docker-compose.dev.yml restart nexus-backend
```

或者如果使用 `dev-start.sh` 脚本：

```bash
./scripts/dev-stop.sh
./scripts/dev-start.sh
```

## 验证修复

1. 重新构建并启动后端
2. 清除浏览器缓存
3. 重新登录系统
4. 应该能够正常进入系统，不再闪退

## 相关文件

- 修复代码: `nexusarchive-java/src/main/java/com/nexusarchive/config/FondsContextFilter.java`
- 用户权限修复: `nexusarchive-java/src/main/resources/db/migration/V84__fix_user_fonds_scope_for_existing_data.sql`

