package beanPostProcesser.scan.proxy;

import cn.xu.spring.annotation.Autowired;
import cn.xu.spring.annotation.Component;

@Component
public class InjectProxyOnPropertyBean {

    @Autowired
    public OriginBean injected;
}
