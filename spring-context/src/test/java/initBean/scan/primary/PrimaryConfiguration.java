package initBean.scan.primary;

import cn.xu.spring.annotation.Bean;
import cn.xu.spring.annotation.Configuration;
import cn.xu.spring.annotation.Primary;

@Configuration
public class PrimaryConfiguration {

    @Primary
    @Bean
    DogBean husky() {
        return new DogBean("Husky");
    }

    @Bean
    DogBean teddy() {
        return new DogBean("Teddy");
    }
}
