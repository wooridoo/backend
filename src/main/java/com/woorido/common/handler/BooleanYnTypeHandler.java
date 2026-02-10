package com.woorido.common.handler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@MappedJdbcTypes(JdbcType.CHAR)
@MappedTypes(Boolean.class)
public class BooleanYnTypeHandler extends BaseTypeHandler<Boolean> {

  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, Boolean parameter, JdbcType jdbcType)
      throws SQLException {
    ps.setString(i, parameter ? "Y" : "N");
  }

  @Override
  public Boolean getNullableResult(ResultSet rs, String columnName) throws SQLException {
    String result = rs.getString(columnName);
    return "Y".equals(result);
  }

  @Override
  public Boolean getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
    String result = rs.getString(columnIndex);
    return "Y".equals(result);
  }

  @Override
  public Boolean getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
    String result = cs.getString(columnIndex);
    return "Y".equals(result);
  }
}
