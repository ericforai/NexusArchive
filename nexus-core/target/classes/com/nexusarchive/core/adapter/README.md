一旦我所属的文件夹有所变化，请更新我。
本目录存放数据库适配层原型。
用于 DDL 生成与类型映射。

## 文件清单

| 文件 | 地位 | 功能 |
| --- | --- | --- |
| `README.md` | 说明文档 | 本目录说明 |
| `BaseDbAdapter.java` | 类 | 通用 DDL 生成逻辑 |
| `ColumnDefinition.java` | 类 | 列定义模型 |
| `DataType.java` | 枚举 | 适配层通用数据类型 |
| `DataTypeMapping.java` | 类 | 数据类型映射 |
| `DbAdapter.java` | 接口 | 适配器定义 |
| `DbAdapters.java` | 类 | 适配器工厂 |
| `DbVendor.java` | 枚举 | 数据库厂商枚举 |
| `SchemaManager.java` | 类 | DDL 生成入口 |
| `TableDefinition.java` | 类 | 表结构模型 |
| `dameng/` | 目录入口 | 达梦适配实现 |
| `kingbase/` | 目录入口 | 金仓适配实现 |
| `postgresql/` | 目录入口 | PostgreSQL 适配实现 |
