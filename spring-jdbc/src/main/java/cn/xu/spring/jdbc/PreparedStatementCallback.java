package cn.xu.spring.jdbc;

import jakarta.annotation.Nullable;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * created by Xu on 2024/5/24 14:45.
 */
@FunctionalInterface
public interface PreparedStatementCallback<T> {
    @Nullable
    T doInPreparedStatement(PreparedStatement ps) throws SQLException;
}
