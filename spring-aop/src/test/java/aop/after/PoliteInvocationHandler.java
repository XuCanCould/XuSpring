package aop.after;

import cn.xu.spring.annotation.Component;
import cn.xu.spring.aop.AfterInvocationHandlerAdapter;

import java.lang.reflect.Method;

@Component
public class PoliteInvocationHandler extends AfterInvocationHandlerAdapter {

    @Override
    public Object after(Object proxy, Object returnValue, Method method, Object[] args) {
        if (returnValue instanceof String s) {
            if (s.endsWith(".")) {
                return s.substring(0, s.length() - 1) + "!";
            }
        }
        return returnValue;
    }
}
