package com.xrcgs.iam.handler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;

/**
 * 在Java布尔类型与数值型数据库列（0/1）之间进行转换.
 */
@MappedTypes(Boolean.class)
@MappedJdbcTypes({JdbcType.SMALLINT, JdbcType.TINYINT, JdbcType.INTEGER})
public class BooleanToSmallIntTypeHandler extends BaseTypeHandler<Boolean> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Boolean parameter, JdbcType jdbcType) throws SQLException {
        ps.setShort(i, Boolean.TRUE.equals(parameter) ? (short) 1 : (short) 0);
    }

    @Override
    public Boolean getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return toBoolean(rs.getObject(columnName));
    }

    @Override
    public Boolean getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return toBoolean(rs.getObject(columnIndex));
    }

    @Override
    public Boolean getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return toBoolean(cs.getObject(columnIndex));
    }

    private Boolean toBoolean(Object value) throws SQLException {
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean b) {
            return b;
        }
        if (value instanceof Number n) {
            return n.intValue() != 0;
        }
        if (value instanceof String s) {
            String normalized = s.trim().toLowerCase(Locale.ROOT);
            if (normalized.isEmpty()) {
                return null;
            }
            if ("1".equals(normalized) || "true".equals(normalized) || "t".equals(normalized) || "yes".equals(normalized) || "y".equals(normalized)) {
                return true;
            }
            if ("0".equals(normalized) || "false".equals(normalized) || "f".equals(normalized) || "no".equals(normalized) || "n".equals(normalized)) {
                return false;
            }
            try {
                return Integer.parseInt(normalized) != 0;
            } catch (NumberFormatException ex) {
                throw new SQLException("Cannot convert value to Boolean: " + value, ex);
            }
        }
        throw new SQLException("Unsupported value type for Boolean conversion: " + value.getClass().getName());
    }
}
