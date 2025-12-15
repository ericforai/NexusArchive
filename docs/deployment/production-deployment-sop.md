# 私有化电子会计档案系统 — 生产部署标准操作流程 (SOP)

> **版本**：V2.0  
> **更新日期**：2025-12-10  
> **适用范围**：信创环境（国产 CPU/OS/DB）+ 离线私有化部署  
> **合规依据**：GB/T 39362-2020、DA/T 92-2022、《会计档案管理办法》（财政部79号令）

---

## 流程概览

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  1. 变更准备 → 2. 制品治理 → 3. 信创基线检测 → 4. 部署策略选择             │
│                                                                             │
│  5. 数据库迁移 → 6. 部署实施 → 7. 渐进式交付 → 8. 合规验证与审计            │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 1. 变更准备 (Change Preparation)

### 1.1 风险评估

使用**风险评估矩阵**对变更进行分类：

| 风险等级 | 定义 | 审批要求 |
|----------|------|----------|
| 低 | 配置调整、文案修改 | 技术负责人审批 |
| 中 | 功能增强、Bug 修复 | 技术 + 产品审批 |
| 高 | 数据模型变更、核心逻辑修改 | 技术 + 产品 + 合规审批 |
| 紧急 | 安全漏洞、生产故障 | 快速通道 + 事后补审 |

> [!IMPORTANT]
> **DDL 变更（数据库表结构变更）统一按「高风险」处理**，需独立审批通道。

### 1.2 变更请求 (CR) 提交

变更请求必须包含：

- [ ] 变更内容描述
- [ ] 影响范围评估（是否涉及历史归档数据）
- [ ] 回滚方案
- [ ] 预计停机时间（如有）

### 1.3 审批流程

```
提交 CR → 技术评审 → 合规评审（如涉及归档逻辑）→ 审批通过/拒绝
```

---

## 2. 制品治理 (CI Artifact Governance)

### 2.1 代码提交与安全扫描

```
代码提交 → SAST 扫描 → 依赖扫描 → 国密合规扫描 → 构建制品
```

| 扫描类型 | 工具建议 | 检查项 |
|----------|----------|--------|
| SAST | SonarQube / Coverity | 代码漏洞、SQL注入、XSS |
| 依赖扫描 | OWASP Dependency-Check | CVE 漏洞、许可证合规 |
| **国密合规扫描** | 自研检查脚本 | 禁用 MD5/SHA1，必须使用 SM3 |

### 2.2 构建不可变制品

- 使用语义化版本号：`v{主版本}.{次版本}.{修订号}-{build号}`
- 制品签名：使用 SM2 对 JAR/Docker Image 进行签名
- 制品校验：SHA256 + SM3 双校验

### 2.3 离线镜像打包

> [!WARNING]
> **私有化部署必须项**：客户环境无法访问 Docker Hub。

```bash
# 导出镜像
docker save nexusarchive:v2.0 | gzip > nexusarchive-v2.0.tar.gz

# 生成校验文件
sha256sum nexusarchive-v2.0.tar.gz > nexusarchive-v2.0.tar.gz.sha256
sm3sum nexusarchive-v2.0.tar.gz > nexusarchive-v2.0.tar.gz.sm3
```

推送至私有镜像仓库（Harbor / JFrog）。

---

## 3. 信创基线检测 (Xinchuang Baseline Check)

> [!IMPORTANT]
> **部署前必须通过基线检测**，确保目标环境兼容。

### 3.1 硬件与系统检测

| 检测项 | 要求 | 检测命令 |
|--------|------|----------|
| CPU 架构 | x86_64 / aarch64 | `uname -m` |
| 操作系统 | 麒麟 V10 / 统信 UOS 20 以上 | `cat /etc/os-release` |
| 内存 | ≥ 16GB | `free -h` |
| 磁盘空间 | ≥ 500GB（归档存储） | `df -h` |

### 3.2 数据库环境检测

| 检测项 | 要求 |
|--------|------|
| 数据库类型 | 达梦 DM8 / 人大金仓 KingbaseES V8 / PostgreSQL 14+ |
| 字符集 | UTF-8 |
| 连接数限制 | ≥ 200 |

### 3.3 网络隔离确认

- [ ] 确认无外网依赖（所有依赖已离线打包）
- [ ] 确认内网 DNS 解析正常
- [ ] 确认 NTP 时间同步（归档时间戳依赖）

### 3.4 基线检测脚本

```bash
# 运行基线检测
./nexus-installer.sh --check-baseline

# 检测通过后继续
# 检测失败会输出不满足项及修复建议
```

---

## 4. 部署策略选择 (Deployment Strategy)

### 4.1 策略对比

| 策略 | 适用场景 | 风险 | 推荐度 |
|------|----------|------|--------|
| **蓝绿部署** | 会计档案系统（推荐） | 低 | ⭐⭐⭐⭐⭐ |
| 滚动更新 | 无状态服务 | 中 | ⭐⭐⭐ |
| 金丝雀部署 | 流量可分割的系统 | 高 | ⭐⭐ |

> [!CAUTION]
> **金丝雀部署不推荐用于归档业务**：部分用户归档到新版本、部分到旧版本，可能导致数据不一致。

### 4.2 推荐策略：蓝绿部署

```
                    ┌─────────────┐
                    │  负载均衡器  │
                    └──────┬──────┘
                           │
           ┌───────────────┼───────────────┐
           ▼                               ▼
    ┌─────────────┐                 ┌─────────────┐
    │  蓝环境     │                 │  绿环境     │
    │  (当前版本) │                 │  (新版本)   │
    │  100% 流量  │   ──验证后──▶   │  100% 流量  │
    └─────────────┘                 └─────────────┘
```

### 4.3 部署窗口

- **推荐窗口**：工作日 22:00 - 次日 06:00（财务非高峰期）
- **回滚窗口**：部署后 4 小时内可立即回滚

---

## 5. 数据库迁移 (Database Migration)

### 5.1 迁移前检查

- [ ] Flyway 迁移脚本版本校验
- [ ] 生产库完整备份已完成
- [ ] 备份恢复测试已验证
- [ ] DDL 变更已通过双人审批

### 5.2 备份策略

```bash
# 达梦数据库备份示例
dmrman BACKUP DATABASE FULL TO '/backup/nexus_full_$(date +%Y%m%d_%H%M%S).bak'

# PostgreSQL 备份示例
pg_dump -Fc nexusarchive > /backup/nexus_$(date +%Y%m%d_%H%M%S).dump
```

### 5.3 迁移执行

```bash
# 检查待执行迁移
flyway -url=jdbc:dm://localhost:5236/NEXUS info

# 执行迁移（生产环境需审批后执行）
flyway -url=jdbc:dm://localhost:5236/NEXUS migrate
```

### 5.4 迁移回滚

> [!WARNING]
> DDL 变更通常不可在线回滚，需从备份恢复。

```bash
# 从备份恢复（需停机）
dmrman RESTORE DATABASE FROM '/backup/nexus_full_xxxx.bak'
```

---

## 6. 部署实施 (Deployment Execution)

### 6.1 GitOps 同步（如使用 K8s）

```yaml
# Argo CD Application 示例
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: nexusarchive
spec:
  source:
    repoURL: https://git.internal/nexusarchive/deploy
    targetRevision: v2.0.0
    path: manifests/production
  destination:
    server: https://kubernetes.default.svc
    namespace: nexusarchive
  syncPolicy:
    automated:
      prune: false  # 生产环境禁止自动删除
      selfHeal: true
```

### 6.2 传统部署（如使用安装包）

```bash
# 使用一键安装脚本
./nexus-installer.sh --mode=upgrade \
  --version=2.0.0 \
  --backup-before-upgrade=true \
  --config=/etc/nexusarchive/config.yml
```

### 6.3 配置与密钥管理

- [ ] 数据库密码已通过 Vault / 环境变量注入
- [ ] SM4 加密密钥已配置
- [ ] JWT 签名密钥已更新（如有变更）
- [ ] License 文件已验证

### 6.4 OPA 策略验证（如使用 K8s）

```bash
# 验证部署是否符合策略
kubectl get constraintviolations -A
```

---

## 7. 交付验收门禁 (Delivery Acceptance Gate)

> [!IMPORTANT]
> **红线要求**: 每次交付（Delivery）、客户现场部署（Deployment）或升级（Upgrade）前，**必须** 运行门禁脚本并存档记录。

### 7.1 执行门禁

使用提供的自动化验收脚本执行全栈检查：

```bash
# 执行交付门禁
node scripts/delivery_gatekeeper.cjs
```

### 7.2 验收标准 (一票否决)

任何以下项触发 VETO (否决) 时，**严禁交付**：

| 检查项 | VETO 条件 | 风险 |
|--------|-----------|------|
| **DB 宕机启动** | 应用无法启动 / Health 返回 200 (装健康) | 生产事故风险 (假死/无限悬挂) |
| **冷启动延迟** | Health 接口耗时 > 1s | 线程池耗尽风险 |
| **业务门禁** | 初始化期间 (Migrating) 允许业务请求 | 数据不一致风险 |
| **自动恢复** | DB 恢复后应用无法自愈 (需人工重启) | 运维成本过高 |
| **性能基线** | 核心接口 P99 > 1.5s | 客户投诉风险 |
| **日志审计** | 缺失关键生命周期签名 (Start/Migrate/Success) | 无法定责 |

### 7.3 门禁日志归档

```bash
# 归档门禁报告
cp logs/gatekeeper_test.log /opt/nexusarchive/audit/delivery_gate_$(date +%Y%m%d).log
```

---

## 8. 渐进式交付与监控 (Progressive Delivery)

### 8.1 基础设施指标

| 指标 | 告警阈值 | 数据源 |
|------|----------|--------|
| CPU 使用率 | > 80% | Prometheus |
| 内存使用率 | > 85% | Prometheus |
| HTTP 5xx 错误率 | > 1% | Prometheus |
| 响应延迟 P99 | > 2s | Prometheus |

### 8.2 业务指标（归档系统专用）

| 指标 | 告警阈值 | 说明 |
|------|----------|------|
| **归档成功率** | < 99% | 归档 API 成功/总请求 |
| **四性检测通过率** | < 99.5% | 检测通过/总检测数 |
| **OFD 转换成功率** | < 98% | 转换成功/总转换数 |
| **审计日志写入延迟** | > 100ms | 异步日志写入时间 |

### 8.3 SLO 验证检查点

```
部署后 5 分钟  → 第一次 SLO 检查
部署后 30 分钟 → 第二次 SLO 检查
部署后 2 小时  → 第三次 SLO 检查（解除回滚窗口）
```

### 8.4 自动回滚触发条件

满足以下任一条件时自动触发回滚：

- HTTP 5xx 错误率 > 5%（持续 2 分钟）
- 归档成功率 < 95%（持续 3 分钟）
- 服务不可用（健康检查连续失败 3 次）

### 8.5 回滚数据一致性保护

> [!CAUTION]
> **回滚前必须执行以下步骤**，防止归档数据不一致。

```bash
# 1. 暂停 ERP 归档推送
curl -X POST http://localhost:8080/api/system/pause-ingestion

# 2. 等待队列清空
curl http://localhost:8080/api/system/queue-status
# 确认 pending_count = 0

# 3. 记录回滚边界点
echo "回滚边界: $(date -Iseconds)" >> /var/log/nexus/rollback.log

# 4. 执行回滚
./nexus-installer.sh --rollback

# 5. 恢复 ERP 推送并通知重发失败记录
curl -X POST http://localhost:8080/api/system/resume-ingestion
```

---

## 9. 合规验证与审计 (Compliance Validation & Audit)

### 9.1 冒烟测试

| 测试项 | 验证方法 | 预期结果 |
|--------|----------|----------|
| 用户登录 | 管理员账户登录 | 成功进入系统 |
| 档案查询 | 查询历史归档 | 返回正确数据 |
| 新建归档 | 上传测试文件 | 归档成功 |
| OFD 预览 | 预览已归档文件 | 正常显示 |

### 9.2 四性自动化检测

> [!IMPORTANT]
> **合规必须项**：部署后必须执行四性检测验证。

```bash
# 执行四性检测（抽样 100 条最新归档）
./nexus-cli.sh four-nature-check --sample=100

# 检测项：
# - 真实性：Hash 校验
# - 完整性：元数据完整性
# - 可用性：文件可解析
# - 安全性：权限验证
```

### 9.3 抽样完整性校验

```bash
# 随机抽取 50 条归档记录进行 Hash 比对
./nexus-cli.sh integrity-check --random-sample=50

# 输出示例：
# [OK] 50/50 records passed integrity check
# [WARNING] 0 records failed (expected < 0.1%)
```

### 9.4 审计日志连续性验证

```bash
# 验证审计日志链完整性
./nexus-cli.sh audit-chain-verify --from="2025-12-01" --to="2025-12-10"

# 验证项：
# - 日志序号连续
# - 前置 Hash 链接正确
# - 无断链现象
```

### 9.5 License 验证

```bash
# 验证 License 状态
./nexus-cli.sh license-check

# 输出示例：
# License Type: Enterprise
# Valid Until: 2026-12-31
# Node Limit: 5 (Current: 2)
# Status: VALID
```

### 9.6 部署后检查清单

- [ ] 所有服务健康检查通过
- [ ] 冒烟测试全部通过
- [ ] 四性检测通过率 ≥ 99.5%
- [ ] 审计日志链完整
- [ ] License 有效
- [ ] 监控告警配置已更新
- [ ] 通知相关方部署完成

### 9.7 审计记录归档

```bash
# 生成部署审计报告
./nexus-cli.sh generate-deployment-report \
  --version=2.0.0 \
  --operator="张三" \
  --output=/var/log/nexus/deployment-audit-$(date +%Y%m%d).pdf

# 报告内容：
# - 变更请求编号
# - 审批记录
# - 部署时间线
# - 验证结果
# - 操作人签名
```

---

## 附录 A：紧急回滚流程

```
发现问题 → 判断严重性 → 决定回滚 → 暂停归档入口 → 执行回滚 → 验证服务 → 恢复入口 → 事后复盘
```

**紧急联系人**：

| 角色 | 姓名 | 电话 |
|------|------|------|
| 技术负责人 | [待填写] | [待填写] |
| 运维值班 | [待填写] | [待填写] |
| 合规负责人 | [待填写] | [待填写] |

---

## 附录 B：信创兼容性矩阵

| 组件 | x86_64 + CentOS | ARM + 麒麟 | ARM + 统信 | 备注 |
|------|-----------------|------------|------------|------|
| JDK 11 (毕昇) | ✅ | ✅ | ✅ | 推荐使用毕昇 JDK |
| PostgreSQL 14 | ✅ | ✅ | ✅ | |
| 达梦 DM8 | ✅ | ✅ | ✅ | |
| Redis 7 | ✅ | ✅ | ✅ | |
| Nginx | ✅ | ✅ | ✅ | |
| Docker 20.10 | ✅ | ⚠️ | ⚠️ | 部分版本需测试 |
| Kubernetes 1.24 | ✅ | ⚠️ | ⚠️ | 建议使用 iSulad |

---

## 附录 C：检查清单速查表

### 部署前

- [ ] CR 已审批
- [ ] 制品已构建并签名
- [ ] 基线检测已通过
- [ ] 数据库已备份
- [ ] 回滚方案已确认

### 部署中

- [ ] 迁移脚本已执行
- [ ] 服务已启动
- [ ] 健康检查已通过
- [ ] 监控已配置

### 部署后

- [ ] 冒烟测试通过
- [ ] 四性检测通过
- [ ] 审计日志完整
- [ ] License 有效
- [ ] 部署报告已生成

---

> 📋 **文档维护**：本文档由运维团队维护，任何变更需经技术委员会审批。  
> 📅 **下次评审**：2026-06-10
