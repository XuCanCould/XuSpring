package cn.xu.spring.jdbc.without.tx;

import cn.xu.spring.annotation.*;
import cn.xu.spring.jdbc.JdbcTemplate;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

@ComponentScan
@Configuration
public class JdbcWithoutTxApplication {

    @Bean(destroyMethod = "close")
    DataSource dataSource(
            // properties:
            @Value("${xu-spring.datasource.url}") String url, //
            @Value("${xu-spring.datasource.username}") String username, //
            @Value("${xu-spring.datasource.password}") String password, //
            @Value("${xu-spring.datasource.driver-class-name:}") String driver, //
            @Value("${xu-spring.datasource.maximum-pool-size:20}") int maximumPoolSize, //
            @Value("${xu-spring.datasource.minimum-pool-size:1}") int minimumPoolSize, //
            @Value("${xu-spring.datasource.connection-timeout:30000}") int connTimeout //
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
}
