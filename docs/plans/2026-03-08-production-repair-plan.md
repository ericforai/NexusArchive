# 实现计划：修复生产环境部署并集成百度统计

## 目标
解决后端编译依赖问题，并按计划将百度统计集成到生产环境的公开页面中。

## 当前问题分析
1. **后端编译失败**：缺少 `com.dbappsecurity.aitrust.appSecSso` 包，导致 `nexus-backend` 镜像无法构建。
2. **生产容器缺失**：由于构建失败，目前的生产环境仅运行了数据库和 Redis。
3. **环境差异**：本地开发环境尝试调用 Maven 编译时报错，需要正确配置本地 JAR 路径。

## 拟议更改

### 1. 后端依赖修复 (nexusarchive-java)

#### [修改] [pom.xml](file:///Users/user/nexusarchive/nexusarchive-java/pom.xml)
- 添加本地 JAR 依赖：
  ```xml
  <dependency>
      <groupId>com.dbappsecurity</groupId>
      <artifactId>app-sec-sso</artifactId>
      <version>1.0.0</version>
      <scope>system</scope>
      <systemPath>${project.basedir}/lib/app-sec-sso-1.0.0.jar</systemPath>
  </dependency>
  ```

#### [操作] 引入 JAR 文件
- 将 `java-sdk-test/lib/app-sec-sso-1.0.0.jar` 复制到 `nexusarchive-java/lib/` 目录下。

### 2. 通过 GitHub 提交与部署 (CI/CD)

#### [操作] 提交代码变更
- 将以下文件加入 Git 暂存区：
  - `nexusarchive-java/lib/app-sec-sso-1.0.0.jar`
  - `nexusarchive-java/pom.xml`
  - `src/components/common/BaiduAnalytics.tsx`
  - `src/routes/index.tsx`
- 提交信息：`fix: 修复生产环境构建依赖并集成百度统计 (按需加载)`

#### [操作] 推送至 GitHub
- 执行 `git push`。
- 这将自动触发 `.github/workflows/deploy-prod-via-ssh.yml` 或类似的 Actions 流程进行生产环境部署。

## 验证计划

### 自动化验证
- 在 GitHub Actions 页面监控流水线进度，确保构建和部署步骤全部通过。
- 部署完成后，在服务器上运行健康检查：`bash deploy/health-check.sh --verbose`。

### 手动验证
- 访问生产域名，验证百度统计脚本是否仅在登录前页面加载。
- 登录系统，确认功能正常。
