package cn.xu.spring.jdbc;

import cn.xu.spring.annotation.Autowired;
import cn.xu.spring.annotation.Bean;
import cn.xu.spring.annotation.Configuration;
import cn.xu.spring.annotation.Value;
import cn.xu.spring.jdbc.tx.DataSourceTransactionManager;
import cn.xu.spring.jdbc.tx.PlatformTransactionManager;
import cn.xu.spring.jdbc.tx.TransactionalBeanPostProcessor;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

/**
 * created by Xu on 2024/5/24 14:14.
 * jdbc 配置的读取类
 */
@Configuration
public class JdbcConfiguration {
    @Bean(destroyMethod = "close")
    DataSource dataSource(
            // properties:
            @Value("${xu-spring.datasource.url}") String url,
            @Value("${xu-spring.datasource.username}") String username,
            @Value("${xu-spring.datasource.password}") String password,
            @Value("${xu-spring.datasource.driver-class-name:}") String driver,
            @Value("${xu-spring.datasource.maximum-pool-size:20}") int maximumPoolSize,
            @Value("${xu-spring.datasource.minimum-pool-size:1}") int minimumPoolSize,
            @Value("${xu-spring.datasource.connection-timeout:30000}") int connTimeout
    ) {
        var config = new HikariConfig();
        config.setAutoCommit(false);
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        if (driver != null) {
            config.setDriverClassName(driver);
        }
        config.setMaximumPoolSize(maximumPoolSize);
        config.setMinimumIdle(minimumPoolSize);
        config.setConnectionTimeout(connTimeout);
        return new HikariDataSource(config);
    }

    @Bean
    JdbcTemplate jdbcTemplate(@Autowired DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    TransactionalBeanPostProcessor transactionalBeanPostProcessor() {
        return new TransactionalBeanPostProcessor();
    }

    @Bean
    PlatformTransactionManager platformTransactionManager(@Autowired DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
