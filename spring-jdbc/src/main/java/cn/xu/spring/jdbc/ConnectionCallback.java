package cn.xu.spring.jdbc;

import jakarta.annotation.Nullable;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * created by Xu on 2024/5/24 14:21.
 */
@FunctionalInterface
public interface ConnectionCallback<T> {
    @Nullable
    T doInConnection(Connection connection) throws SQLException;
}
