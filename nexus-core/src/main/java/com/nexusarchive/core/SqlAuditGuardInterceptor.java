// Input: MyBatis-Plus InnerInterceptor + SqlAuditGuard
// Output: SQL 审计守卫拦截器（真实查询链路接入）
// Pos: NexusCore SQL 审计拦截
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core;

import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import java.sql.Connection;
import java.util.Objects;
import org.apache.ibatis.executor.statement.StatementHandler;

public class SqlAuditGuardInterceptor implements InnerInterceptor {
    private final SqlAuditGuard guard;

    public SqlAuditGuardInterceptor(SqlAuditGuard guard) {
        this.guard = Objects.requireNonNull(guard, "guard");
    }

    @Override
    public void beforePrepare(StatementHandler statementHandler, Connection connection, Integer transactionTimeout) {
        guard.check(statementHandler.getBoundSql().getSql());
    }
}
