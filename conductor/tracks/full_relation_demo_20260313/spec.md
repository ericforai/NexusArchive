# 任务规约 (Spec) - 全链路业务演示数据构建

## 目标
为 `seed-voucher-001` (JZ-202311-0052) 构建完整的上下游业务链，消除前端关联图谱中的“主线缺口”提示，提供给客户一个完美的穿透联查演示环境。

## 演示链路定义
根据前端 `detectPaymentMainline` 期望的 `MAINLINE_FLOW` 顺序：
1. **凭证 (Voucher)**: `seed-voucher-001` (已存在)
2. **付款单 (Payment)**: `demo-payment-001` (FK-202311-001)
3. **付款申请 (Application)**: `demo-app-001` (SQ-202311-001)
4. **合同 (Contract)**: `demo-con-001` (HT-202311-001)
5. **发票 (Invoice)**: `demo-inv-001` (FP-202311-001)
6. **回单 (Receipt)**: `demo-rec-001` (HD-202311-001)

## 成功标准
1. ✅ 以上 5 个新节点在 `acc_archive` 表中存在，且 `archive_code` 匹配对应前缀。
2. ✅ 建立了 5 条 `acc_archive_relation` 记录，形成连续的 source -> target 链路。
3. ✅ 每个节点都关联了真实的 PDF 物理文件（可预览）。
4. ✅ 前端 Relationship 视图不再显示“主线缺口”警告。

## 实施路径
1. **数据准备**: 编写 SQL 插入脚本。
2. **物理文件准备**: 生成或复制 5 个演示 PDF 文件到存储目录。
3. **数据执行**: 注入数据库。
4. **验证**: 检查前端图谱展示效果。
