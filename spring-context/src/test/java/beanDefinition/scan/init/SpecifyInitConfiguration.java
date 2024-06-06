package beanDefinition.scan.init;


import cn.xu.spring.annotation.Bean;
import cn.xu.spring.annotation.Configuration;
import cn.xu.spring.annotation.Value;

@Configuration
public class SpecifyInitConfiguration {

    @Bean(initMethod = "init")
    SpecifyInitBean createSpecifyInitBean(@Value("${app.title}") String appTitle, @Value("${app.version}") String appVersion) {
        return new SpecifyInitBean(appTitle, appVersion);
    }
}
