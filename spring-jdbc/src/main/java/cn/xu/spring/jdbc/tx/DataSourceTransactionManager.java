package cn.xu.spring.jdbc.tx;

import cn.xu.spring.exception.TransactionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * created by Xu on 2024/5/24 15:20.
 */
public class DataSourceTransactionManager implements PlatformTransactionManager, InvocationHandler {
    static final ThreadLocal<TransactionStatus> transactionStatus = new ThreadLocal<>();

    final Logger logger = LoggerFactory.getLogger(getClass());

    final DataSource dataSource;

    public DataSourceTransactionManager(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        TransactionStatus ts = transactionStatus.get();
        if (ts == null) {
            // 如果无事务，则开启事务
            try (Connection connection = dataSource.getConnection()) {
                final boolean autoCommit = connection.getAutoCommit();
                if (autoCommit) {
                    connection.setAutoCommit(false);
                }
                try {
                    // 设置事务状态并调用业务方法
                    transactionStatus.set(new TransactionStatus(connection));
                    Object res = method.invoke(proxy, args);
                    connection.commit();
                    return res;
                } catch (InvocationTargetException e) {
                    logger.warn("will rollback transaction for caused exception: {}", e.getCause() == null ? "null" : e.getCause().getClass().getName());
                    var te = new TransactionException(e.getCause());
                    try {
                        connection.rollback();
                    } catch (SQLException sqle) {
                        te.addSuppressed(sqle);
                    }
                    throw te;
                } finally {
                    // 移除线程变量，并恢复连接的提交默认状态
                    transactionStatus.remove();
                    if (autoCommit) {
                        connection.setAutoCommit(true);
                    }
                }
            }
        } else {
            return method.invoke(proxy, args);
        }
    }

}
