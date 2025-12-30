# Agent A: 后端安全工程师任务书

> **角色**: 后端安全工程师
> **技术栈**: Java 17, Spring Boot 3.1.6, Spring Security
> **负责阶段**: 第一阶段 - 安全加固
> **优先级**: ⚠️ 紧急（其他 Agent 依赖此阶段完成）

---

## 📋 项目背景

NexusArchive 是一个电子会计档案管理系统，采用**模块化单体架构**，用于私有化/本地化部署。

### 关键约束
- **合规性 > 性能**：必须符合 DA/T 94-2022、DA/T 92-2022 等档案法规
- **信创适配**：支持国产 CPU（鲲鹏）、国产数据库（达梦）、国密算法（SM2/3/4）
- **私有部署**：不依赖公网服务，支持离线环境

### 项目结构
```
nexusarchive/
├── nexusarchive-java/          # 后端 Spring Boot
│   └── src/main/java/com/nexusarchive/
│       ├── config/             # 配置类（SecurityConfig, RedisConfig 等）
│       ├── service/            # 业务服务
│       └── controller/         # REST 控制器
├── src/                        # 前端 React
└── docs/                       # 文档
```

---

## 🔐 必读规则

执行任务前，请阅读以下规则文件：

1. **[.agent/rules/general.md](file:///Users/user/nexusarchive/.agent/rules/general.md)** - 核心编码规范
2. **[.agent/rules/expert-group.md](file:///Users/user/nexusarchive/.agent/rules/expert-group.md)** - 专家审查机制

---

## ✅ 任务清单

### 1.1 敏感信息外置

| 序号 | 任务 | 文件 | 操作 | 验收标准 |
|------|------|------|------|----------|
| 1.1.1 | JWT密钥外置 | `application.yml:47` | 改为 `${JWT_SECRET:}` 无默认值 | 启动时无 JWT_SECRET 应报错 |
| 1.1.2 | YonSuite密钥外置 | `application.yml:82-83` | app-key/secret 用环境变量 | 配置移到 .env |
| 1.1.3 | 创建环境变量模板 | 新建 `.env.template` | 列出所有必需变量 | 文件存在且完整 |
| 1.1.4 | 更新部署文档 | `docs/DEPLOY.md` | 添加环境变量配置说明 | 文档包含配置步骤 |

**代码示例：**
```yaml
# application.yml 修改后
jwt:
  secret: ${JWT_SECRET:}  # 生产环境必须设置，启动检查

yonsuite:
  app-key: ${YONSUITE_APP_KEY:}
  app-secret: ${YONSUITE_APP_SECRET:}
```

```java
// 新增启动检查（可选，在 NexusArchiveApplication.java）
@PostConstruct
public void validateConfig() {
    if (jwtSecret == null || jwtSecret.isBlank()) {
        throw new IllegalStateException("JWT_SECRET 环境变量必须设置！");
    }
}
```

---

### 1.2 安全配置强化

| 序号 | 任务 | 文件 | 操作 | 验收标准 |
|------|------|------|------|----------|
| 1.2.1 | 限制 frameOptions | `SecurityConfig.java:65` | 改为 `sameOrigin()` | 检查响应头 |
| 1.2.2 | 收紧 CORS 配置 | `CorsConfig.java` | 限定允许的域名列表，从配置读取 | CORS 配置可外部化 |
| 1.2.3 | 添加安全响应头 | `SecurityConfig.java` | 添加 CSP、X-XSS-Protection | curl 检查响应头 |
| 1.2.4 | 请求频率限制 | 新建 `RateLimitFilter.java` | IP 维度限流，使用 Bucket4j 或手写 | 连续请求被拦截 |

**代码示例：**
```java
// SecurityConfig.java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .headers(headers -> headers
            .frameOptions(frame -> frame.sameOrigin())  // 仅允许同源 iframe
            .contentSecurityPolicy(csp -> 
                csp.policyDirectives("default-src 'self'; script-src 'self' 'unsafe-inline'"))
            .xssProtection(xss -> xss.disable()) // 现代浏览器用 CSP 替代
            .permissionsPolicyHeader(pp -> 
                pp.policy("camera=(), microphone=(), geolocation=()"))
        )
        // ... 其他配置
}
```

```java
// RateLimitFilter.java (简易实现)
@Component
@Order(1)
public class RateLimitFilter extends OncePerRequestFilter {
    
    private final Map<String, RateLimiter> limiters = new ConcurrentHashMap<>();
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
            HttpServletResponse response, FilterChain chain) {
        String clientIp = request.getRemoteAddr();
        RateLimiter limiter = limiters.computeIfAbsent(clientIp, 
            k -> RateLimiter.create(10.0)); // 每秒10次
        
        if (!limiter.tryAcquire()) {
            response.setStatus(429);
            response.getWriter().write("{\"error\":\"请求过于频繁\"}");
            return;
        }
        chain.doFilter(request, response);
    }
}
```

---

### 1.3 认证安全增强

| 序号 | 任务 | 文件 | 操作 | 验收标准 |
|------|------|------|------|----------|
| 1.3.1 | 完善登录锁定 | `LoginAttemptService.java` | 5次失败锁定15分钟 | 模拟测试锁定 |
| 1.3.2 | Token 黑名单 | ~~已完成~~ | - | ✅ |
| 1.3.3 | 密码强度校验 | 新建 `PasswordPolicyValidator.java` | 强制8位+大小写+数字+特殊字符 | 弱密码被拒绝 |
| 1.3.4 | 登录日志增强 | `AuthService.java` | 记录设备指纹（User-Agent, IP） | 日志包含信息 |

**代码示例：**
```java
// LoginAttemptService.java
@Service
public class LoginAttemptService {
    
    private static final int MAX_ATTEMPTS = 5;
    private static final int LOCK_MINUTES = 15;
    
    private final StringRedisTemplate redisTemplate;
    
    public void loginFailed(String username) {
        String key = "login:attempts:" + username;
        Long attempts = redisTemplate.opsForValue().increment(key);
        if (attempts == 1) {
            redisTemplate.expire(key, Duration.ofMinutes(LOCK_MINUTES));
        }
    }
    
    public boolean isLocked(String username) {
        String key = "login:attempts:" + username;
        String val = redisTemplate.opsForValue().get(key);
        return val != null && Integer.parseInt(val) >= MAX_ATTEMPTS;
    }
    
    public void loginSuccess(String username) {
        redisTemplate.delete("login:attempts:" + username);
    }
}
```

```java
// PasswordPolicyValidator.java
@Component
public class PasswordPolicyValidator {
    
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
        "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$"
    );
    
    public void validate(String password) {
        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            throw new IllegalArgumentException(
                "密码必须包含大小写字母、数字和特殊字符，且长度至少8位");
        }
    }
}
```

---

### 1.4 输入验证加固

| 序号 | 任务 | 文件 | 操作 | 验收标准 |
|------|------|------|------|----------|
| 1.4.1 | 全局 XSS 过滤 | 新建 `XssFilter.java` | 过滤 `<script>` 等危险字符 | XSS 注入被过滤 |
| 1.4.2 | 参数校验注解 | 所有 Request DTO | 添加 @NotBlank @Size @Pattern | 无效参数 400 返回 |
| 1.4.3 | 文件上传校验 | `IngestController.java` | 校验 MIME 类型和文件大小 | 非法文件被拒绝 |
| 1.4.4 | 路径遍历防护 | 文件操作相关 | 规范化路径，禁止 `..` | 路径遍历被阻止 |

**代码示例：**
```java
// XssFilter.java
@Component
@Order(2)
public class XssFilter extends OncePerRequestFilter {
    
    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response, FilterChain chain) {
        chain.doFilter(new XssRequestWrapper(request), response);
    }
}

// XssRequestWrapper.java
public class XssRequestWrapper extends HttpServletRequestWrapper {
    
    private static final Pattern[] XSS_PATTERNS = {
        Pattern.compile("<script>(.*?)</script>", Pattern.CASE_INSENSITIVE),
        Pattern.compile("javascript:", Pattern.CASE_INSENSITIVE),
        Pattern.compile("on\\w+\\s*=", Pattern.CASE_INSENSITIVE)
    };
    
    @Override
    public String getParameter(String name) {
        return sanitize(super.getParameter(name));
    }
    
    private String sanitize(String value) {
        if (value == null) return null;
        for (Pattern pattern : XSS_PATTERNS) {
            value = pattern.matcher(value).replaceAll("");
        }
        return value;
    }
}
```

---

## 🧪 验证步骤

### 1. 编译验证
```bash
cd nexusarchive-java
mvn clean compile -DskipTests
```

### 2. 安全头验证
```bash
# 启动服务后
curl -I http://localhost:8080/api/health
# 应看到: X-Frame-Options, Content-Security-Policy 等头
```

### 3. 限流验证
```bash
# 连续快速请求
for i in {1..15}; do curl -s http://localhost:8080/api/health; done
# 应看到 429 响应
```

### 4. 密码策略验证
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"weak"}'
# 应返回密码强度错误
```

---

## 📝 完成标志

任务完成后，请在 `docs/优化计划.md` 中勾选以下项目：

- [ ] 1.1.1 JWT密钥外置
- [ ] 1.1.2 YonSuite密钥外置
- [ ] 1.1.3 创建环境变量模板
- [ ] 1.1.4 更新部署文档
- [ ] 1.2.1 限制 frameOptions
- [ ] 1.2.2 收紧 CORS 配置
- [ ] 1.2.3 添加安全响应头
- [ ] 1.2.4 请求频率限制
- [ ] 1.3.1 完善登录锁定
- [ ] 1.3.3 密码强度校验
- [ ] 1.3.4 登录日志增强
- [ ] 1.4.1 全局 XSS 过滤
- [ ] 1.4.2 参数校验注解
- [ ] 1.4.3 文件上传校验
- [ ] 1.4.4 路径遍历防护

---

*Agent A 任务书 - 由 Claude 于 2025-12-07 生成*
