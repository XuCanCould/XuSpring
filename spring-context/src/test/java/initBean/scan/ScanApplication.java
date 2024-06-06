package initBean.scan;

import cn.xu.spring.annotation.ComponentScan;
import cn.xu.spring.annotation.Import;
import initBean.imported.LocalDateConfiguration;
import initBean.imported.ZonedDateConfiguration;

@ComponentScan
@Import({ LocalDateConfiguration.class, ZonedDateConfiguration.class })
public class ScanApplication {

}
