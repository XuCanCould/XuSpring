package beanPostProcesser.scan.proxy;

import cn.xu.spring.annotation.Autowired;
import cn.xu.spring.annotation.Component;

@Component
public class InjectProxyOnConstructorBean {

    public final OriginBean injected;

    public InjectProxyOnConstructorBean(@Autowired OriginBean injected) {
        this.injected = injected;
    }
}
