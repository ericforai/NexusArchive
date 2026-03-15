# Phase 3 后续修复计划

**制定日期**: 2026-03-15
**基于**: SonarQube 分析结果
**目标**: 降低 Bugs 到 <30, Vulnerabilities 到 0, 提升测试覆盖率到 80%

---

## 执行摘要

| 指标 | 当前值 | 目标值 | 差距 |
|------|--------|--------|------|
| **Bugs** | 56 | <30 | -26 |
| **Vulnerabilities** | 2 | 0 | -2 |
| **Security Hotspots** | 29 | 0 | -29 |
| **Code Smells** | 1389 | <1500 | ✅ 已达标 |
| **测试覆盖率** | 0.0% | 80% | +80% |
| **技术债务** | 27 天 | <7 天 | -20 天 |

---

## 问题分布统计

### 按严重程度
| 严重程度 | 数量 |
|----------|------|
| CRITICAL | 295 |
| BLOCKER | 38 |
| MAJOR | 537 |
| MINOR | 440 |
| INFO | 137 |
| **总计** | **1,447** |

### 按类型
| 类型 | 数量 |
|------|------|
| Code Smell | 1,389 |
| Bug | 56 |
| Vulnerability | 2 |

### Top 问题规则
| 规则 | 数量 | 说明 |
|------|------|------|
| java:S1192 | 166 | 字符串字面量重复 |
| java:S112 | 129 | 原始类型使用泛型 |
| java:S1128 | 122 | 数组使用原始类型 |
| java:S3776 | 70 | 认知复杂度过高 |
| java:S5786 | 60 | 未使用的导入 |
| java:S1135 | 60 | TODO 注释 |
| java:S1874 | 55 | 空指针风险 |

---

## 修复计划

### P0 - 安全修复 (预计 2 小时)

| # | 文件 | 问题 | 修复方案 |
|---|------|------|----------|
| 1 | `TimestampService.java:206` | 使用 HTTP Basic 认证 | 改用 OAuth2 或更安全的方式 |
| 2 | `YonSuiteEventCrypto.java:58` | 使用固定 IV | 使用动态生成的随机 IV |

**预计收益**: Vulnerabilities: 2 → 0

---

### P1 - 高优先级 Bug 修复 (预计 4 小时)

| # | 规则 | 数量 | 修复方案 |
|---|------|------|----------|
| 1 | java:S1874 (空指针风险) | 55 | 扩展 RequestContext.getRequiredXxx() 使用 |
| 2 | java:S2229 (@Transactional 冲突) | 1 | 重构事务传播配置 |
| 3 | java:S2647 (安全认证) | 1 | 同 P0-1 |

---

### P2 - Code Smell 批量修复 (预计 6 小时)

#### 2.1 字符串常量提取 (166 处)

**自动化工具**: 使用 IntelliJ IDEA 的 "Replace String with Constant" 或脚本批量处理

| 高频字面量 | 出现次数 | 建议常量名 |
|-----------|----------|------------|
| "Bearer " | 3+ | AUTHORIZATION_BEARER_PREFIX |
| "status" | 16+ | STATUS_FIELD_NAME |
| "message" | 14+ | MESSAGE_FIELD_NAME |
| "COLLECTION_BATCH" | 5+ | COLLECTION_BATCH_TABLE |
| "Authenticity Check" | 4+ | CHECK_TYPE_AUTHENTICITY |
| "Usability Check" | 3+ | CHECK_TYPE_USABILITY |

#### 2.2 复杂度降低 (70 处)

| 文件 | 复杂度 | 建议操作 |
|------|--------|----------|
| `ModuleDiscoveryService.java:162` | 31 | 提取方法，简化控制流 |
| `LegacyImportOrchestrator.java:54` | 27 | 拆分为多个小方法 |
| `FourNatureCheckServiceImpl.java:119` | 16 | 提取子方法 |
| `PoolController.java:208` | 22 | 简化逻辑 |
| `TimestampService.java:185` | 18 | 提取方法 |

#### 2.3 清理未使用代码 (60 处)

| 规则 | 数量 | 工具 |
|------|------|------|
| 未使用的导入 | 60 | IDE 自动清理 + checkstyle |

#### 2.4 TODO 清理 (60 处)

| 操作 | 数量 |
|------|------|
| 转换为 Issue | 高优先级 TODO |
| 删除过时 TODO | 已完成功能的 TODO |
| 添加技术债务标记 | 暂不处理的 TODO |

---

### P3 - 测试覆盖率提升 (预计 10 小时)

#### 3.1 生成覆盖率报告

```bash
# 运行测试并生成 JaCoCo 报告
mvn clean test jacoco:report
```

#### 3.2 覆盖率缺口分析

| 层 | 当前覆盖率 | 目标 | 缺口 |
|----|-----------|------|------|
| Controller | ~5% | 80% | 75% |
| Service | ~15% | 80% | 65% |
| Security/Util | ~40% | 80% | 40% |

#### 3.3 测试优先级

**P0 核心业务** (必须测试):
- 四性检测服务 (`FourNatureCheckService`)
- 销毁审批服务 (`DestructionApprovalService`)
- 档案状态转换 (`ArchiveStateTransitionService`)
- MFA 服务 (`MfaServiceImpl`)

**P1 安全模块** (补充测试):
- JWT 工具 (`JwtUtil`) - 已有 68 测试 ✅
- SM3/SM4 加密工具 - 已有测试
- 密码工具 (`PasswordUtil`) - 已有测试 ✅

**P2 集成测试**:
- API 端点集成测试
- 数据库操作集成测试

---

## 执行顺序

```
Week 1: 安全 + 关键 Bug
├── Day 1-2: P0 安全修复 (Vulnerability → 0)
├── Day 3-5: P1 高优先级 Bug (空指针风险消除)
│
├── Week 2-3: Code Smell 批量修复
├── Week 2: 字符串常量 + 未使用代码清理
├── Week 3: 复杂度降低 + TODO 清理
│
├── Week 4-5: 测试覆盖率提升
├── Week 4: 核心业务单元测试
├── Week 5: 集成测试 + 覆盖率验证
│
└── Week 6: 验证与收尾
    ├── 最终质量扫描
    ├── 遗留问题评估
    └── 文档更新
```

---

## 验证清单

每个阶段完成后执行：

- [ ] `mvn clean compile` - 编译通过
- [ ] `mvn test` - 单元测试通过
- [ ] SonarQube 扫描 - 验证问题减少
- [ ] 覆盖率报告验证 - 目标 80%+

---

## 成功指标

| 指标 | Phase 3 开始 | Phase 3 结束 | 改进 |
|------|-------------|--------------|------|
| Bugs | 56 | <30 | -46% ↓ |
| Vulnerabilities | 2 | 0 | -100% ↓ |
| Security Hotspots | 29 | <10 | -65% ↓ |
| Code Smells | 1389 | <1200 | -14% ↓ |
| 测试覆盖率 | 0% | 80% | +80% ↑ |
| 技术债务 | 27天 | <7天 | -74% ↓ |

---

## 工具配置建议

### JaCoCo 配置 (pom.xml)

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.11</version>
    <executions>
        <execution>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

### SonarQube 覆盖率配置

```properties
<sonar.coverage.jacoco.xmlReportPaths>target/site/jacoco/jacoco.xml</sonar.coverage.jacoco.xmlReportPaths>
<sonar.coverage.jacoco.xmlReportPaths>target/site/jacoco/jacoco.xml</sonar.coverage.jacoco.xmlReportPaths>
```

---

**文档版本**: v1.0
**更新日期**: 2026-03-15
**状态**: 待用户确认
