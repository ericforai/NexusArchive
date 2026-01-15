# 归档审批功能修复记录

**日期**: 2026-01-15
**修复人员**: Claude Code
**影响范围**: 预归档池提交、归档审批页面

---

## 问题现象

### 用户报告
1. 在预归档池 (\`/system/pre-archive/pool\`) 提交归档审批后
2. 归档审批页面 (\`/system/operations/approval\`) 看不到提交的记录
3. 提交时显示成功，但数据未出现

---

## 根因分析

### 问题 1: API 响应格式解析错误
- 后端返回: \`{ code: 200, data: { records: [], total: 0 } }\`
- axios 包装: \`{ data: { code: 200, data: { records: [], total: 0 } } }\`
- 正确访问路径: \`response.data.data.records\`

### 问题 2: 档号生成器使用内存计数器
- \`ArchivalCodeGenerator.generate()\` 使用**内存 AtomicInteger 计数器**
- 应用重启后计数器清零，导致档号重复
- **解决方案**: 改用数据库持久化的 \`ArchivalCodeGeneratorImpl\`

### 问题 3: SQL 列名错误
\`\`\`
ERROR: column "updated_at" of relation "sys_archival_code_sequence" does not exist
\`\```
- Mapper 使用 \`updated_at\`，但数据库列名是 \`updated_time\`

### 问题 4: 更新记录时 title 不更新
- 当 \`acc_archive\` 表已存在 ID 相同记录时，只更新 \`archive_code\` 和 \`status\`
- **不更新 \`title\` 和 \`summary\` 字段**

---

## 修复内容

### 1. API 响应格式修复
**文件**: \`src/pages/operations/ArchiveApprovalView.tsx\`

\`\`typescript
// 修复后
setApprovals(response?.data?.data?.records || []);
setStatusCounts({
    PENDING: pendingRes?.data?.data?.total || 0,
    ...
});
\`\`\`

### 2. 档号生成器修复
**文件**: \`src/main/java/com/nexusarchive/service/PreArchiveSubmitService.java\`

\`\`java
// 修复后
private final com.nexusarchive.service.strategy.ArchivalCodeGenerator archivalCodeGeneratorStrategy;

private String generateArchiveCode(ArcFileContent file) {
    Archive tempArchive = new Archive();
    tempArchive.setFondsNo(...);
    return archivalCodeGeneratorStrategy.generateNextCode(tempArchive);
}
\`\`\`

### 3. SQL 列名修复
**文件**: \`src/main/java/com/nexusarchive/mapper/ArchivalCodeSequenceMapper.java\`

\`\`java
// updated_at → updated_time
\`\`\`

### 4. 记录更新时 title 修复
\`\`java
String properTitle = extractTitle(file);
updateWrapper.set(Archive::getTitle, properTitle)
            .set(Archive::getSummary, file.getFileName());
\`\`\`

---

## 验证步骤

### 数据库初始化
\`\`\`sql
INSERT INTO sys_archival_code_sequence (fonds_code, fiscal_year, category_code, current_val)
VALUES ('BR-GROUP', '2020', 'AC01', 2);
\`\`\`

### 后端测试
\`\`\`bash
mvn compile && mvn spring-boot:run
\`\`\`

---

## 相关文件清单

| 文件 | 修改类型 |
|------|---------|
| \`src/pages/operations/ArchiveApprovalView.tsx\` | 修复 |
| \`src/api/archiveApproval.ts\` | 修复 |
| \`src/main/java/com/nexusarchive/service/PreArchiveSubmitService.java\` | 修复 |
| \`src/main/java/com/nexusarchive/mapper/ArchivalCodeSequenceMapper.java\` | 修复 |
