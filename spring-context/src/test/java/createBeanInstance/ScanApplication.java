package createBeanInstance;


import cn.xu.spring.annotation.ComponentScan;
import cn.xu.spring.annotation.Import;
import createBeanInstance.imported.LocalDateConfiguration;
import createBeanInstance.imported.ZonedDateConfiguration;

@ComponentScan
@Import({ LocalDateConfiguration.class, ZonedDateConfiguration.class })
public class ScanApplication {

}
