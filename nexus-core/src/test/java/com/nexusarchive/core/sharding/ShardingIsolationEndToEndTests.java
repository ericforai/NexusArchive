// Input: Sharding-JDBC YAML + SQL 审计守卫（YamlShardingSphereDataSourceFactory）
// Output: 端到端隔离验证（阻断缺失 fonds_no/fiscal_year 的查询）
// Pos: NexusCore Sharding POC 测试
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core.sharding;

import com.nexusarchive.core.FondsIsolationException;
import com.nexusarchive.core.SqlAuditGuard;
import com.nexusarchive.core.SqlAuditRules;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Objects;
import java.util.Set;
import javax.sql.DataSource;
import org.apache.shardingsphere.driver.api.yaml.YamlShardingSphereDataSourceFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShardingIsolationEndToEndTests {
    @Test
    void shouldBlockQueryWithoutFondsNo() throws Exception {
        DataSource shardingDataSource = loadShardingDataSource();
        SqlAuditGuard guard = new SqlAuditGuard(SqlAuditRules.defaults());
        DataSource audited = SqlAuditDataSource.wrap(shardingDataSource, guard::check);

        try (Connection connection = audited.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(loadSql("sql/sharding-poc-setup.sql"));
            statement.execute(loadSql("sql/sharding-poc-insert.sql"));

            String selectWithoutFonds = loadSql("sql/sharding-poc-select-no-fonds.sql");
            assertThrows(FondsIsolationException.class,
                    () -> statement.executeQuery(selectWithoutFonds));

            String selectWithoutYear = loadSql("sql/sharding-poc-select-no-year.sql");
            assertThrows(FondsIsolationException.class,
                    () -> statement.executeQuery(selectWithoutYear));

            String selectWithFonds = loadSql("sql/sharding-poc-select-with-fonds.sql");
            try (ResultSet resultSet = statement.executeQuery(selectWithFonds)) {
                assertTrue(resultSet.next());
            }
        }
    }

    private DataSource loadShardingDataSource() throws Exception {
        URL resource = ShardingIsolationEndToEndTests.class
                .getClassLoader()
                .getResource("sharding-poc.yml");
        if (resource == null) {
            throw new IllegalStateException("Missing sharding-poc.yml");
        }
        File yamlFile = toFile(resource);
        return YamlShardingSphereDataSourceFactory.createDataSource(yamlFile);
    }

    private File toFile(URL resource) throws URISyntaxException {
        return new File(resource.toURI());
    }

    private String loadSql(String path) throws IOException {
        URL resource = ShardingIsolationEndToEndTests.class.getClassLoader().getResource(path);
        if (resource == null) {
            throw new IllegalStateException("Missing SQL file: " + path);
        }
        try {
            Path filePath = Path.of(resource.toURI());
            return Files.readString(filePath, StandardCharsets.UTF_8);
        } catch (URISyntaxException ex) {
            throw new IllegalStateException("Invalid SQL URI: " + path, ex);
        }
    }

    private static final class SqlAuditDataSource implements DataSource {
        private static final Set<String> PREPARE_METHODS = Set.of(
                "prepareStatement",
                "prepareCall");
        private static final Set<String> STATEMENT_EXECUTES = Set.of(
                "execute",
                "executeQuery",
                "executeUpdate");

        private final DataSource delegate;
        private final SqlGuard guard;

        private SqlAuditDataSource(DataSource delegate, SqlGuard guard) {
            this.delegate = Objects.requireNonNull(delegate, "delegate");
            this.guard = Objects.requireNonNull(guard, "guard");
        }

        static DataSource wrap(DataSource delegate, SqlGuard guard) {
            return new SqlAuditDataSource(delegate, guard);
        }

        @Override
        public Connection getConnection() throws java.sql.SQLException {
            return wrapConnection(delegate.getConnection(), guard);
        }

        @Override
        public Connection getConnection(String username, String password) throws java.sql.SQLException {
            return wrapConnection(delegate.getConnection(username, password), guard);
        }

        @Override
        public <T> T unwrap(Class<T> iface) throws java.sql.SQLException {
            return delegate.unwrap(iface);
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) throws java.sql.SQLException {
            return delegate.isWrapperFor(iface);
        }

        @Override
        public java.io.PrintWriter getLogWriter() throws java.sql.SQLException {
            return delegate.getLogWriter();
        }

        @Override
        public void setLogWriter(java.io.PrintWriter out) throws java.sql.SQLException {
            delegate.setLogWriter(out);
        }

        @Override
        public void setLoginTimeout(int seconds) throws java.sql.SQLException {
            delegate.setLoginTimeout(seconds);
        }

        @Override
        public int getLoginTimeout() throws java.sql.SQLException {
            return delegate.getLoginTimeout();
        }

        @Override
        public java.util.logging.Logger getParentLogger() {
            try {
                return delegate.getParentLogger();
            } catch (java.sql.SQLFeatureNotSupportedException ex) {
                throw new IllegalStateException(ex);
            }
        }

        private Connection wrapConnection(Connection connection, SqlGuard guard) {
            InvocationHandler handler = new ConnectionHandler(connection, guard);
            return (Connection) Proxy.newProxyInstance(
                    connection.getClass().getClassLoader(),
                    new Class[]{Connection.class},
                    handler);
        }

        private PreparedStatement wrapPreparedStatement(
                PreparedStatement statement,
                SqlGuard guard,
                String sql) {
            InvocationHandler handler = new PreparedStatementHandler(statement, guard, sql);
            return (PreparedStatement) Proxy.newProxyInstance(
                    statement.getClass().getClassLoader(),
                    new Class[]{PreparedStatement.class},
                    handler);
        }

        private Statement wrapStatement(Statement statement, SqlGuard guard) {
            InvocationHandler handler = new StatementHandler(statement, guard);
            return (Statement) Proxy.newProxyInstance(
                    statement.getClass().getClassLoader(),
                    new Class[]{Statement.class},
                    handler);
        }

        private final class ConnectionHandler implements InvocationHandler {
            private final Connection target;
            private final SqlGuard guard;

            private ConnectionHandler(Connection target, SqlGuard guard) {
                this.target = target;
                this.guard = guard;
            }

            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                String name = method.getName();
                if (PREPARE_METHODS.contains(name) && args != null && args.length > 0 && args[0] instanceof String) {
                    String sql = (String) args[0];
                    guard.check(sql);
                    Object prepared = method.invoke(target, args);
                    return wrapPreparedStatement((PreparedStatement) prepared, guard, sql);
                }
                if ("createStatement".equals(name)) {
                    Object created = method.invoke(target, args);
                    return wrapStatement((Statement) created, guard);
                }
                return method.invoke(target, args);
            }
        }

        private static final class StatementHandler implements InvocationHandler {
            private final Statement target;
            private final SqlGuard guard;

            private StatementHandler(Statement target, SqlGuard guard) {
                this.target = target;
                this.guard = guard;
            }

            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                if (STATEMENT_EXECUTES.contains(method.getName())
                        && args != null && args.length > 0 && args[0] instanceof String) {
                    guard.check((String) args[0]);
                }
                return method.invoke(target, args);
            }
        }

        private static final class PreparedStatementHandler implements InvocationHandler {
            private final PreparedStatement target;
            private final SqlGuard guard;
            private final String sql;

            private PreparedStatementHandler(PreparedStatement target, SqlGuard guard, String sql) {
                this.target = target;
                this.guard = guard;
                this.sql = sql;
            }

            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                String name = method.getName();
                if (STATEMENT_EXECUTES.contains(name)) {
                    guard.check(sql);
                }
                return method.invoke(target, args);
            }
        }

        @FunctionalInterface
        private interface SqlGuard {
            void check(String sql);
        }
    }
}
