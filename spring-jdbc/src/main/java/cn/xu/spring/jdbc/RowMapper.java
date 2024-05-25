package cn.xu.spring.jdbc;

import jakarta.annotation.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * created by Xu on 2024/5/24 16:29.
 */
@FunctionalInterface
public interface RowMapper<T> {
    @Nullable
    T mapRow(ResultSet rs, int rowNum) throws SQLException;
}
