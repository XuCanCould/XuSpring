package createBeanInstance.imported;

import cn.xu.spring.annotation.Bean;
import cn.xu.spring.annotation.Configuration;

import java.time.ZonedDateTime;

@Configuration
public class ZonedDateConfiguration {

    @Bean
    ZonedDateTime startZonedDateTime() {
        return ZonedDateTime.now();
    }
}
