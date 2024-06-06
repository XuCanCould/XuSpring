package cn.xu.spring.jdbc.with.tx;

import cn.xu.spring.annotation.ComponentScan;
import cn.xu.spring.annotation.Configuration;
import cn.xu.spring.annotation.Import;
import cn.xu.spring.jdbc.JdbcConfiguration;

@ComponentScan
@Configuration
@Import(JdbcConfiguration.class)
public class JdbcWithTxApplication {

}
