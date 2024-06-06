package aop.before;

import cn.xu.spring.annotation.Bean;
import cn.xu.spring.annotation.ComponentScan;
import cn.xu.spring.annotation.Configuration;
import cn.xu.spring.aop.AroundProxyBeanPostProcessor;

@Configuration
@ComponentScan
public class BeforeApplication {

    @Bean
    AroundProxyBeanPostProcessor createAroundProxyBeanPostProcessor() {
        return new AroundProxyBeanPostProcessor();
    }
}
