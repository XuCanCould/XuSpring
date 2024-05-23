package cn.xu.spring.aop;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * created by Xu on 2024/5/23 14:48.
 * 实现 before 拦截（拦截器模板）
 */
public abstract class BeforeInvocationHandlerAdapter implements InvocationHandler {
    public abstract void before(Object proxy, Method method, Object[] args);

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        before(proxy, method, args);
        return method.invoke(proxy, args);
    }
}
