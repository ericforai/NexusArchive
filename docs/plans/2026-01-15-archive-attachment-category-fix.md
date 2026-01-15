# 调整档案附件分类实现计划

档案 `BR-GROUP-2025-30Y-FIN-AC01-1003` 的附件 `电子发票_米山神鸡_201元.pdf` 目前被错误地分类为“银行回单”（`bank_slip`），导致在全景视图中显示在错误的标签页下。本计划旨在通过 SQL 脚本修正该分类，将其移至“原始凭证”（`invoice`）。

## 拟议变更

### 数据库变更

通过 SQL 更新 `acc_archive_attachment` 表中的记录。

#### [MODIFY] `acc_archive_attachment`

- 目标记录 ID: `attach-link-003` (关联档案 `voucher-2024-11-003`)
- 变更字段: `attachment_type` 从 `bank_slip` 修改为 `invoice`
- 变更字段: `relation_desc` 从 `银行回单` 修改为 `原始凭证`

## 验证计划

### 自动化验证
- 执行查询语句确认数据已更新。

### 手动验证
- 刷新全景视图页面 `http://localhost:15175/system/panorama`
- 搜索凭证 `BR-GROUP-2025-30Y-FIN-AC01-1003`
- 确认“原始凭证”标签页下出现 `电子发票_米山神鸡_201元.pdf`
- 确认“银行回单”标签页计数变为 0 或隐藏
