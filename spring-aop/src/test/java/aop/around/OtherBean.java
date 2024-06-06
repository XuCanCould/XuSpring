package aop.around;

import cn.xu.spring.annotation.Autowired;
import cn.xu.spring.annotation.Component;
import cn.xu.spring.annotation.Order;

@Order(0)
@Component
public class OtherBean {

    public OriginBean origin;

    public OtherBean(@Autowired OriginBean origin) {
        this.origin = origin;
    }
}
