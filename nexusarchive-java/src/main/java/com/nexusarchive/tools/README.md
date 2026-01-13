一旦我所属的文件夹有所变化，请更新我。

# Nexus Archive Tools
包含用于开发、构建和维护的辅助工具。

## 文件列表
- `SchemaValidator.java`: **Entity-Schema 验证工具** (Java类)。
  - 功能：扫描 Entity 类，解析 MyBatis-Plus 注解，生成预期的数据库列名列表文件。
  - 用途：配合 `scripts/validate-schema.sh` 进行数据库结构一致性检查。
