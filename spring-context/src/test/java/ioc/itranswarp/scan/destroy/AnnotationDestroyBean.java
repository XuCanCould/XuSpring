package ioc.itranswarp.scan.destroy;

import cn.xu.spring.annotation.Component;
import cn.xu.spring.annotation.Value;
import jakarta.annotation.PreDestroy;

@Component
public class AnnotationDestroyBean {

    @Value("${app.title}")
    public String appTitle;

    @PreDestroy
    void destroy() {
        this.appTitle = null;
    }
}
