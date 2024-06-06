package aop.around;

import cn.xu.spring.annotation.Bean;
import cn.xu.spring.annotation.ComponentScan;
import cn.xu.spring.annotation.Configuration;
import cn.xu.spring.aop.AroundProxyBeanPostProcessor;

@Configuration
@ComponentScan
public class AroundApplication {

    @Bean
    AroundProxyBeanPostProcessor createAroundProxyBeanPostProcessor() {
        return new AroundProxyBeanPostProcessor();
    }
}
