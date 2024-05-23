package cn.xu.spring.aop;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * created by Xu on 2024/5/23 14:51.
 * 实现 after 拦截器模板
 */
public abstract class AfterInvocationHandlerAdapter implements InvocationHandler {
    // after 拦截可以修改返回值
    public abstract Object after(Object proxy, Object returnValue, Method method, Object[] args);

    @Override
    public final Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Object ret = method.invoke(proxy, args);
        return after(proxy, ret, method, args);
    }
}
