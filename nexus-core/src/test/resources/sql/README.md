一旦我所属的文件夹有所变化，请更新我。
本目录存放 Sharding POC SQL 脚本。
用于端到端测试的建表与查询。

## 文件清单

| 文件 | 地位 | 功能 |
| --- | --- | --- |
| `README.md` | 说明文档 | 本目录说明 |
| `sharding-poc-setup.sql` | SQL | 建表脚本 |
| `sharding-poc-insert.sql` | SQL | 插入样例数据 |
| `sharding-poc-select-no-fonds.sql` | SQL | 缺失 fonds_no 查询 |
| `sharding-poc-select-no-year.sql` | SQL | 缺失 fiscal_year 查询 |
| `sharding-poc-select-with-fonds.sql` | SQL | 带 fonds_no + fiscal_year 查询 |
| `sql-audit-rule-setup.sql` | SQL | SQL 审计字典建表 |
| `sql-audit-rule-insert.sql` | SQL | SQL 审计字典插入样例 |
