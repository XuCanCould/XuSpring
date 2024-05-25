package cn.xu.spring.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * created by Xu on 2024/5/24 14:45.
 */
@FunctionalInterface
public interface PreparedStatementCreator<T> {
    PreparedStatement createPreparedStatement(Connection con) throws SQLException;
}
