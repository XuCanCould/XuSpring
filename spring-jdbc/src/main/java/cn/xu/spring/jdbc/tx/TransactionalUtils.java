package cn.xu.spring.jdbc.tx;

import jakarta.annotation.Nullable;

import java.sql.Connection;

/**
 * created by Xu on 2024/5/24 15:18.
 * 事务工具类
 */
public class TransactionalUtils {

    /**
     * 获取当前事务的连接
     */
    @Nullable
    public static Connection getCurrentConnection() {
        TransactionStatus ts = DataSourceTransactionManager.transactionStatus.get();
        return ts == null ? null : ts.connection;
    }
}
