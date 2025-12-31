# 适配层 DDL 示例清单（PG / 达梦 / 金仓）

> 目的：提供适配层输出的 DDL 示例，便于对照 `docs/database/` 中现有脚本。

## 一、类型映射清单（摘要）

| 逻辑类型 | PostgreSQL | 达梦 (DM8) | 金仓 (Kingbase) |
| --- | --- | --- | --- |
| STRING | VARCHAR(n) | VARCHAR(n) | VARCHAR(n) |
| TEXT | TEXT | CLOB | TEXT |
| INTEGER | INTEGER | INTEGER | INTEGER |
| BIGINT | BIGINT | BIGINT | BIGINT |
| DECIMAL | NUMERIC(p,s) | DECIMAL(p,s) | NUMERIC(p,s) |
| BOOLEAN | BOOLEAN | SMALLINT (0/1) | BOOLEAN |
| DATE | DATE | DATE | DATE |
| TIMESTAMP | TIMESTAMP | TIMESTAMP | TIMESTAMP |
| JSON | JSONB | CLOB | JSON |
| BLOB | BYTEA | BLOB | BYTEA |

> 注：达梦/金仓的 JSON 支持与索引能力需以生产环境版本为准。

## 二、示例 DDL：arc_account_item

### PostgreSQL
```sql
CREATE TABLE arc_account_item (
  fonds_no VARCHAR(32) NOT NULL,
  archive_year INTEGER NOT NULL,
  item_id VARCHAR(64) NOT NULL,
  title VARCHAR(255) NOT NULL,
  metadata JSONB,
  amount NUMERIC(18,2),
  created_time TIMESTAMP NOT NULL,
  PRIMARY KEY (fonds_no, archive_year, item_id)
);
```

### 达梦 (DM8)
```sql
CREATE TABLE arc_account_item (
  fonds_no VARCHAR(32) NOT NULL,
  archive_year INTEGER NOT NULL,
  item_id VARCHAR(64) NOT NULL,
  title VARCHAR(255) NOT NULL,
  metadata CLOB,
  amount DECIMAL(18,2),
  created_time TIMESTAMP NOT NULL,
  PRIMARY KEY (fonds_no, archive_year, item_id)
);
```

### 金仓 (Kingbase)
```sql
CREATE TABLE arc_account_item (
  fonds_no VARCHAR(32) NOT NULL,
  archive_year INTEGER NOT NULL,
  item_id VARCHAR(64) NOT NULL,
  title VARCHAR(255) NOT NULL,
  metadata JSON,
  amount NUMERIC(18,2),
  created_time TIMESTAMP NOT NULL,
  PRIMARY KEY (fonds_no, archive_year, item_id)
);
```

## 三、示例 DDL：sys_fonds_history

### PostgreSQL
```sql
CREATE TABLE sys_fonds_history (
  history_id VARCHAR(64) NOT NULL,
  fonds_no VARCHAR(32) NOT NULL,
  change_type VARCHAR(32) NOT NULL,
  change_reason TEXT,
  effective_date DATE NOT NULL,
  active_flag BOOLEAN NOT NULL,
  created_time TIMESTAMP NOT NULL,
  PRIMARY KEY (history_id, fonds_no)
);
```

### 达梦 (DM8)
```sql
CREATE TABLE sys_fonds_history (
  history_id VARCHAR(64) NOT NULL,
  fonds_no VARCHAR(32) NOT NULL,
  change_type VARCHAR(32) NOT NULL,
  change_reason CLOB,
  effective_date DATE NOT NULL,
  active_flag SMALLINT NOT NULL,
  created_time TIMESTAMP NOT NULL,
  PRIMARY KEY (history_id, fonds_no)
);
```

### 金仓 (Kingbase)
```sql
CREATE TABLE sys_fonds_history (
  history_id VARCHAR(64) NOT NULL,
  fonds_no VARCHAR(32) NOT NULL,
  change_type VARCHAR(32) NOT NULL,
  change_reason TEXT,
  effective_date DATE NOT NULL,
  active_flag BOOLEAN NOT NULL,
  created_time TIMESTAMP NOT NULL,
  PRIMARY KEY (history_id, fonds_no)
);
```

## 四、对照建议
1. 与 `docs/database/auth_schema*.sql` 中涉及的字段类型进行逐项比对。
2. 如出现 JSON/文本类型不一致，优先按目标库兼容性调整适配层映射。
3. 若需索引 JSON 字段，必须通过适配层显式声明并在脚本中单独维护。
