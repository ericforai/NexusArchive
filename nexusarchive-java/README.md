一旦我所属的文件夹有所变化，请更新我。
本目录包含 Java 后端源码与运行配置。
用于构建、启动与运维后端服务。

## 文件清单

| 文件 | 地位 | 功能 |
| --- | --- | --- |
| `.dockerignore` | 配置文件 | Docker 构建忽略清单 |
| `Dockerfile` | 构建入口 | 后端镜像构建脚本 |
| `README.md` | 说明文档 | 后端使用与部署说明 |
| `backend.log` | 运行日志 | 后端运行输出（可忽略） |
| `backend_error.log` | 运行日志 | 后端错误日志（可忽略） |
| `backend_startup.log` | 运行日志 | 启动日志（可忽略） |
| `data/` | 目录入口 | 后端数据与样例文件 |
| `keystore/` | 目录入口 | 证书与密钥材料 |
| `lib/` | 目录入口 | 第三方 Jar 依赖 |
| `logs/` | 目录入口 | 后端日志输出目录 |
| `pid.txt` | 运行文件 | 进程 PID 记录 |
| `pom.xml` | 构建入口 | Maven 依赖与构建配置 |
| `scripts/` | 目录入口 | 后端脚本与工具 |
| `setup.sh` | 运维脚本 | 后端初始化脚本 |
| `simulate_webhook.py` | 工具脚本 | 模拟 webhook 调用 |
| `src/` | 目录入口 | 后端源码目录 |
| `startup.log` | 运行日志 | 启动过程日志（可忽略） |
| `target/` | 目录入口 | 构建产物目录 |
| `test-run.log` | 运行日志 | 测试运行日志（可忽略） |
| `walkthrough.md` | 文档 | 后端演示与流程说明 |

# NexusArchive 电子会计档案管理系统 - Java后端

## 项目简介

NexusArchive是一款面向中大型企业的电子会计档案管理系统，完全符合国家标准：
- **DA/T 94-2022** 《电子会计档案管理规范》
- **GB/T 39784-2021** 《电子档案管理系统通用功能要求》

### 技术栈

**后端框架**：
- Spring Boot 3.1.6
- Spring Security 6
- MyBatis-Plus 3.5.5
- JWT (jjwt 0.12.3)

**数据库**：
- 开发环境：PostgreSQL 14+
- 生产环境：达梦数据库 (DM8) / 人大金仓 (KingbaseES)

**构建工具**：
- Maven 3.8+
- Java 17+

---

## 快速开始

### 1. 环境准备

#### 安装PostgreSQL

**macOS**:
```bash
brew install postgresql@14
brew services start postgresql@14
```

**创建数据库**:
```bash
psql postgres
CREATE DATABASE nexusarchive;
\q
```

#### 安装Java 17

**macOS**:
```bash
brew install openjdk@17
```

#### 安装Maven

```bash
brew install maven
```

### 2. 初始化数据库

```bash
cd nexusarchive-java
psql -U postgres -d nexusarchive -f src/main/resources/sql/schema-postgresql.sql
```

### 3. 配置数据库连接

编辑 `src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/nexusarchive
    username: postgres
    password: your_password  # 修改为你的密码
```

### 4. 启动项目

```bash
mvn clean install
mvn spring-boot:run
```

访问: http://localhost:8080/api

---

## 项目结构

```
nexusarchive-java/
├── pom.xml                                 # Maven配置
├── src/main/
│   ├── java/com/nexusarchive/
│   │   ├── NexusArchiveApplication.java   # 主应用类
│   │   ├── common/                         # 公共模块
│   │   │   ├── enums/                      # 枚举类
│   │   │   ├── exception/                  # 异常类
│   │   │   └── result/                     # 响应结果
│   │   ├── config/                         # 配置类
│   │   │   ├── SecurityConfig.java         # 安全配置
│   │   │   ├── MyBatisPlusConfig.java      # MyBatis配置
│   │   │   └── DatabaseConfig.java         # 数据库配置
│   │   ├── entity/                         # 实体类
│   │   │   ├── User.java
│   │   │   ├── Role.java
│   │   │   └── Archive.java
│   │   ├── mapper/                         # MyBatis Mapper
│   │   ├── service/                        # 业务逻辑层
│   │   │   ├── UserService.java
│   │   │   ├── RoleService.java
│   │   │   └── RoleValidationService.java  # 三员互斥校验
│   │   ├── controller/                     # 控制器
│   │   │   ├── AuthController.java
│   │   │   └── AdminController.java
│   │   └── util/                           # 工具类
│   │       ├── JwtUtil.java
│   │       └── PasswordUtil.java
│   └── resources/
│       ├── application.yml                 # 配置文件
│       ├── sql/
│       │   ├── schema-postgresql.sql       # PostgreSQL建表脚本
│       │   ├── schema-dameng.sql           # 达梦数据库脚本
│       │   └── schema-kingbase.sql         # 人大金仓脚本
│       └── mapper/                         # MyBatis XML
└── README.md
```

---

## 核心功能

### 1. 三员管理 (GB/T 39784-2021)

系统实现严格的三员分立：
- **系统管理员**：负责系统运维和配置
- **安全保密员**：负责权限和密钥管理
- **安全审计员**：负责审计日志查看

**三员互斥校验**：
- 三员角色不能分配给同一用户
- 后端自动拦截违规操作
- 前端实时显示警告提示

### 2. 四性检测 (DA/T 94-2022)

自动检测档案的：
- **真实性**：数字签名验证
- **完整性**：SHA256哈希校验
- **可用性**：文件格式验证
- **安全性**：病毒扫描

### 3. 档案关联管理

支持复杂的多对多关联：
- 记账凭证 → 原始凭证
- 红字发票 → 蓝字发票
- 主件 → 附件

### 4. 审计日志

完整记录：
- 用户操作日志
- 敏感操作前后数据对比
- 风险等级评估

---

## 数据库适配

### PostgreSQL (开发环境)

```yaml
spring:
  profiles:
    active: dev
  datasource:
    driver-class-name: org.postgresql.Driver
    url: jdbc:postgresql://localhost:5432/nexusarchive
```

### 达梦数据库 (生产环境)

```yaml
spring:
  profiles:
    active: prod-dameng
  datasource:
    driver-class-name: dm.jdbc.driver.DmDriver
    url: jdbc:dm://localhost:5236/NEXUSARCHIVE
```

### 人大金仓 (生产环境)

```yaml
spring:
  profiles:
    active: prod-kingbase
  datasource:
    driver-class-name: com.kingbase8.Driver
    url: jdbc:kingbase8://localhost:54321/nexusarchive
```

---

## API文档

### 认证接口

**登录**:
```
POST /api/auth/login
{
  "username": "admin",
  "password": "admin123"
}
```

**响应**:
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "user": {
      "id": "user_admin",
      "username": "admin",
      "fullName": "系统管理员"
    }
  }
}
```

### 角色管理

**获取角色列表**:
```
GET /api/admin/roles
Authorization: Bearer {token}
```

**创建角色**:
```
POST /api/admin/roles
{
  "name": "财务专员",
  "code": "FINANCE_STAFF",
  "roleCategory": "business_user",
  "permissions": ["view_archives", "manage_archives"]
}
```

---

## 部署指南

### 开发环境

```bash
mvn clean package
java -jar target/nexusarchive-backend-2.0.0.jar --spring.profiles.active=dev
```

### 生产环境 (达梦数据库)

```bash
mvn clean package -P prod
java -jar target/nexusarchive-backend-2.0.0.jar --spring.profiles.active=prod-dameng
```

### Docker部署

```bash
docker build -t nexusarchive-backend:2.0.0 .
docker run -d -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod-dameng \
  -e SPRING_DATASOURCE_URL=jdbc:dm://db-host:5236/NEXUSARCHIVE \
  nexusarchive-backend:2.0.0
```

---

## 开发指南

### 添加新实体

1. 创建实体类 `entity/YourEntity.java`
2. 创建Mapper接口 `mapper/YourEntityMapper.java`
3. 创建Service `service/YourEntityService.java`
4. 创建Controller `controller/YourEntityController.java`

### 三员互斥校验

使用 `RoleValidationService`:

```java
@Autowired
private RoleValidationService roleValidationService;

public void assignRole(String userId, List<String> roleIds) {
    // 三员互斥校验
    roleValidationService.validateThreeRoleExclusion(userId, roleIds);
    
    // 分配角色
    userRoleService.assignRoles(userId, roleIds);
}
```

---

## 许可证

Copyright © 2024 NexusArchive Team. All rights reserved.

---

## 联系我们

- 项目主页: https://github.com/nexusarchive/nexusarchive-java
- 技术支持: support@nexusarchive.com
