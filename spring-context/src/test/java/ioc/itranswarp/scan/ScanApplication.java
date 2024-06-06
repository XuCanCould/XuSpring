package ioc.itranswarp.scan;

import cn.xu.spring.annotation.ComponentScan;
import cn.xu.spring.annotation.Import;
import ioc.itranswarp.imported.LocalDateConfiguration;
import ioc.itranswarp.imported.ZonedDateConfiguration;

@ComponentScan
@Import({ LocalDateConfiguration.class, ZonedDateConfiguration.class })
public class ScanApplication {

}
