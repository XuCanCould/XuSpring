package beanDefinition.scan;


import beanDefinition.imported.ZonedDateConfiguration;
import beanDefinition.imported.LocalDateConfiguration;
import cn.xu.spring.annotation.ComponentScan;
import cn.xu.spring.annotation.Import;

@ComponentScan
@Import({ LocalDateConfiguration.class, ZonedDateConfiguration.class })
public class ScanApplication {

}
