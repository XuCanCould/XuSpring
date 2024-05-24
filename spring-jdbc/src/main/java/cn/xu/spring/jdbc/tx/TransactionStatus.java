package cn.xu.spring.jdbc.tx;

import java.sql.Connection;

/**
 * created by Xu on 2024/5/24 15:21.
 */
public class TransactionStatus {
    final Connection connection;

    public TransactionStatus(Connection connection) {
        this.connection = connection;
    }
}
