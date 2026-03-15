# SonarQube 代码分析配置指南

**更新日期**: 2026-03-15
**推荐方案**: 本地 SonarQube Docker（无限制）
**在线方案**: SonarCloud（有代码行数限制）

---

## 重要提示

⚠️ **SonarCloud 免费版限制**：
- 免费组织最多 750,000 行代码
- 如果遇到 "maximum allowed lines limit" 错误，请使用本地 SonarQube

---

## 一、本地 SonarQube（推荐，免费无限制）

### 启动本地 SonarQube

```bash
# 使用 Docker Compose 启动
docker-compose -f docker-compose.sonarqube.yml up -d

# 等待启动完成（约 1-2 分钟）
# 访问 http://localhost:9000
# 默认账号: admin / admin
```

### 首次登录配置

1. 访问 http://localhost:9000
2. 使用默认账号登录：`admin` / `admin`
3. 创建或更新 token：
   - **My Account** → **Security** → **Tokens**
   - 点击 "Generate token"
   - 名称：`nexusarchive-local`
   - 复制生成的 token

### 运行本地分析

```bash
cd nexusarchive-java

# 方法 1: 使用环境变量
export SONAR_TOKEN=你的本地token
mvn clean verify sonar:sonar \
  -Dsonar.host.url=http://localhost:9000

# 方法 2: 直接指定 token
mvn clean verify sonar:sonar \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.token=你的本地token
```

### 查看

访问 http://localhost:9000/dashboard?id=nexusarchive 查看分析报告。

---

## 二、SonarCloud 在线版（有代码行数限制）

### Step 1: 注册 SonarCloud 账号

1. 访问 https://sonarcloud.io
2. 点击 **Sign Up**，使用 GitHub 账号登录（推荐）
3. 授权 SonarCloud 访问你的 GitHub 账号

### Step 2: 创建新项目

1. 登录后，点击 **"+" → "Analyze new project"**
2. 选择 **"With GitHub Actions"**
3. 选择 `nexusarchive` 仓库
4. 填写项目信息：
   - **Organization**: 你的组织名
   - **Project key**: nexusarchive
   - **Display name**: NexusArchive

### Step 3: 获取 Sonar Token

1. 点击右上角头像 → **My Account**
2. 在左侧菜单找到 **Security**
3. 点击 **Create new token**
4. 填写信息并生成 token（只显示一次，请立即复制）

### Step 4: 配置 GitHub Secrets

在 GitHub 仓库中添加 Secret：

1. **Settings** → **Secrets and variables** → **Actions**
2. **New repository secret**
   - Name: `SONAR_TOKEN`
   - Value: 你的 SonarCloud token

注意：`GITHUB_TOKEN` 由 GitHub 自动提供，无需手动配置。

### ⚠️ 代码行数限制

如果遇到以下错误：
```
This analysis will make your organization to reach
the maximum allowed lines limit
```

说明 SonarCloud 免费额度已用完。解决方案：
1. 使用本地 SonarQube（推荐）
2. 升级到 SonarCloud 付费版
3. 联系 SonarCloud 支持增加额度

---

## 二、本地运行分析

### 方法 1: Maven 命令

```bash
cd nexusarchive-java

# 运行 SonarQube 分析
mvn clean verify sonar:sonar \
  -Dsonar.host.url=https://sonarcloud.io \
  -Dsonar.token=YOUR_SONAR_TOKEN
```

### 方法 2: 使用环境变量（推荐）

```bash
# 设置环境变量
export SONAR_TOKEN=YOUR_SONAR_TOKEN

# 运行分析
mvn clean verify sonar:sonar
```

### 方法 3: Maven settings.xml（最安全）

在 `~/.m2/settings.xml` 中添加：

```xml
<settings>
  <servers>
    <server>
      <id>sonarcloud</id>
      <username>YOUR_SONAR_TOKEN</username>
    </server>
  </servers>
</settings>
```

然后运行：
```bash
mvn clean verify sonar:sonar
```

---

## 三、自动分析（CI/CD）

配置完成后，每次推送代码或创建 PR 时会自动运行分析：

### 触发条件

| 事件 | 分支 | 说明 |
|------|------|------|
| Push | main, develop | 完整分析 |
| Pull Request | main, develop | 增量分析 |

### 查看结果

1. 在 PR 页面会显示 **SonarQube Comment**，包含：
   - 代码质量变化
   - 新增 Bug/Code Smell 数量
   - 覆盖率变化

2. 完整报告：访问 https://sonarcloud.io/project/overview?id=nexusarchive

---

## 四、质量门禁配置

### 默认规则

SonarCloud 会为项目设置默认的质量门禁：

| 条件 | 状态 | 要求 |
|------|------|------|
| Bug on New Code | 🔴 阻止 | = 0 |
| Vulnerability on New Code | 🟡 警告 | = 0 |
| Code Smell on New Code | 🟡 警告 | ≤ 100 |
| Coverage on New Code | 🟡 警告 | ≥ 80% |

### 自定义质量门禁

1. 访问项目页面：**Quality Gates**
2. 点击 **"Create"** 创建自定义规则
3. 设置适合项目的阈值

---

## 五、本地分析配置

项目已包含 `sonar-project.properties` 配置文件，定义了：

- 代码扫描范围
- 排除文件（DTO、实体类等）
- 覆盖率配置
- 规则排除

### 排除的规则

```properties
# e1: DTO 中的 getter/setter 不需要检测复杂度
sonar.issue.ignore.multicriteria.e1.ruleKey=java:S1172
sonar.issue.ignore.multicriteria.e1.resourceKey=**/dto/**

# e2: Lombok 生成的方法参数未使用警告
sonar.issue.ignore.multicriteria.e2.ruleKey=java:S1172
sonar.issue.ignore.multicriteria.e2.resourceKey=**/entity/**
```

---

## 六、常见问题

### Q1: 分析失败，提示 "Unauthorized"

**原因**: SONAR_TOKEN 未配置或已过期

**解决**:
1. 检查 GitHub Secrets 中的 `SONAR_TOKEN` 是否正确
2. 重新生成 token（SonarCloud → My Account → Security）

### Q2: 覆盖率报告未生成

**原因**: JaCoCo 插件未正确配置

**解决**:
```bash
# 确保运行测试
mvn clean test jacoco:report sonar:sonar
```

### Q3: PR 中没有显示 SonarQube 评论

**原因**: GitHub Actions 权限不足

**解决**:
1. 检查 workflow 文件中的 `permissions` 配置
2. 确保 `pull-requests: write` 权限已启用

### Q4: 分析超时

**原因**: 项目过大或网络问题

**解决**:
1. 增加超时时间：在 workflow 中添加 `SONAR_TIMEOUT` 环境变量
2. 使用并行分析

---

## 七、最佳实践

### 1. PR 前本地检查

```bash
# 提交 PR 前，先在本地运行分析
mvn clean verify sonar:sonar
```

### 2. 关注增量指标

优先修复 **New Code** 中的问题，而非历史遗留问题。

### 3. 定期审查质量门禁

每季度审查并更新质量门禁规则，确保与项目目标一致。

---

## 八、相关文档

- [SonarCloud 官方文档](https://docs.sonarcloud.io/)
- [SonarQube Maven 插件文档](https://docs.sonarqube.org/latest/analysis/scan/sonarscanner-for-maven/)
- [质量门禁最佳实践](https://docs.sonarcloud.io/getting-started/quality-gates/)
