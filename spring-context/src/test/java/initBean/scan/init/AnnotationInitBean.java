package initBean.scan.init;

import cn.xu.spring.annotation.Component;
import cn.xu.spring.annotation.Value;
import jakarta.annotation.PostConstruct;

@Component
public class AnnotationInitBean {

    @Value("${app.title}")
    String appTitle;

    @Value("${app.version}")
    String appVersion;

    public String appName;

    @PostConstruct
    void init() {
        this.appName = this.appTitle + " / " + this.appVersion;
    }
}
