# 500 错误排查任务清单

- [x] 检查 `mvn spring-boot:run` 进程状态（✅ 端口冲突已解决）
- [x] 读取 `backend.log` 获取详细错误堆栈（✅ 发现 `NoClassDefFoundError` 与编译不完整有关）
- [x] 执行 `mvn clean` 并重新尝试启动（✅ 已执行 `mvn clean compile`）
- [x] 定位错误原因（✅ 1. 端口 19090 被占用；2. `StatusConstants` 编译异常；3. Main 类扫描模糊）
- [x] 实施修复方案（✅ 1. 杀死僵尸进程；2. 修复 `StatusConstants.java`；3. 在 `pom.xml` 指定 Main 类）
- [x] 验证修复结果（✅ `/api/health` 返回 200 OK）
- [x] 附带修复前端编译错误（✅ 1. `ComplianceReport.tsx` 参数匹配；2. `OnlineReceptionView.tsx` 属性名修正）
