// Input: SQL 审计规则来源
// Output: 规则快照加载接口
// Pos: NexusCore SQL 审计规则扩展点
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core;

import java.util.Optional;

public interface SqlAuditRulesProvider {
    Optional<SqlAuditRules> load();
}
