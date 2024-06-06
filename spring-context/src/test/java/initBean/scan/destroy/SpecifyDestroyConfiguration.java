package initBean.scan.destroy;

import cn.xu.spring.annotation.Bean;
import cn.xu.spring.annotation.Configuration;
import cn.xu.spring.annotation.Value;

@Configuration
public class SpecifyDestroyConfiguration {

    @Bean(destroyMethod = "destroy")
    SpecifyDestroyBean createSpecifyDestroyBean(@Value("${app.title}") String appTitle) {
        return new SpecifyDestroyBean(appTitle);
    }
}
