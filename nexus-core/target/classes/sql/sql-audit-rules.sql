-- Input: SQL 审计字典查询模板
-- Output: 规则查询 SQL
-- Pos: NexusCore SQL 审计
-- 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。
SELECT ${keyColumn}, ${valueColumn}
FROM ${table}
WHERE ${keyColumn} IN (?, ?);
