# 技术架构与安全规范 (Technical Architecture & Security)

## 1. 架构原则
*   **私有化部署 (Private Deployment)**: 
    *   应用必须支持完全离线环境运行。
    *   严禁依赖公网 SaaS 服务 (如 Google Fonts, CDN)。
*   **信创适配**:
    *   数据库方言支持: PostgreSQL (开发/演示), Dameng (生产/信创).
    *   中间件: Redis, RabbitMQ (均为离线部署版).

## 2. 安全架构 (Security)

### 2.1 身份认证与权限
*   **JWT Externalization**: 密钥从代码中剥离，通过环境变量注入。
*   **登录防护**: Redis 计数器防爆破 (5次锁定)。
*   **密码强度**: 必须包含大小写、数字、特殊字符。

### 2.2 数据安全
*   **加密算法**: 
    *   **传输层**: HTTPS (支持国密 SSL).
    *   **存储层**: 敏感字段 (如手机号、身份证) 使用 **SM4** 加密存储。
*   **审计日志**: 
    *   基于 AOP (`@ArchivalAudit`) 拦截所有关键操作。
    *   日志包含: Operator, IP, Mac Address (尽可能获取), Operation Type, Object Hash.
    *   **防篡改**: 日志记录一旦写入，数据库层面只读 (通过 Grant 权限控制)。

### 2.3 接口安全
*   **限流 (Rate Limiting)**: 基于 Bucket4j 或 Redis 实现 API 级别的限流。
*   **Header 安全**: 
    *   `X-Frame-Options: DENY`
    *   `X-Content-Type-Options: nosniff`
    *   `X-XSS-Protection: 1; mode=block`

## 3. 交付与运维
*   **一键安装包**: 包含所有依赖 (JDK, DB, Middleware, App)。
*   **Flyway 迁移**: 数据库变更严格通过 Versioned Migrations 管理。
