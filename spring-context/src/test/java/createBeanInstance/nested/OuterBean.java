package createBeanInstance.nested;

import cn.xu.spring.annotation.Component;


@Component
public class OuterBean {

    @Component
    public static class NestedBean {

    }
}
