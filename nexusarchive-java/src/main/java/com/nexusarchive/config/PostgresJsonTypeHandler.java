package com.nexusarchive.config;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * PostgreSQL JSONB 类型处理器
 * 用于将 String 类型正确映射到 PostgreSQL 的 JSONB 列
 * 使用 setObject + Types.OTHER 让 PostgreSQL 自动识别 JSON 类型
 */
@MappedTypes(String.class)
@MappedJdbcTypes(JdbcType.OTHER)
public class PostgresJsonTypeHandler extends BaseTypeHandler<String> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType) throws SQLException {
        // 使用 Types.OTHER 和 PGobject 兼容方式
        // PostgreSQL JDBC 驱动会自动将 String 转换为 JSONB
        ps.setObject(i, parameter, Types.OTHER);
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
}
