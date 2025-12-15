package com.nexusarchive.config;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * 多数据库兼容的 JSON 类型处理器
 * 
 * 【信创适配】支持 PostgreSQL JSONB、达梦、人大金仓
 * - PostgreSQL: 使用 Types.OTHER 让驱动自动识别 JSONB
 * - Dameng/Kingbase/其他: 使用 setString 存储为普通文本
 */
@MappedTypes(String.class)
@MappedJdbcTypes(JdbcType.OTHER)
public class PostgresJsonTypeHandler extends BaseTypeHandler<String> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType) throws SQLException {
        String dbType = detectDatabaseType(ps.getConnection());
        
        if ("PostgreSQL".equalsIgnoreCase(dbType)) {
            // PostgreSQL: 使用 Types.OTHER 让驱动自动识别 JSONB
            ps.setObject(i, parameter, Types.OTHER);
        } else {
            // Dameng / Kingbase / 其他数据库: 使用 VARCHAR/TEXT
            ps.setString(i, parameter);
        }
    }

    @Override
    public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return rs.getString(columnName);
    }

    @Override
    public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return rs.getString(columnIndex);
    }

    @Override
    public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return cs.getString(columnIndex);
    }
    
    /**
     * 检测数据库类型
     */
    private String detectDatabaseType(Connection conn) throws SQLException {
        try {
            return conn.getMetaData().getDatabaseProductName();
        } catch (Exception e) {
            return "Unknown";
        }
    }
}
