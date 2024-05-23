package cn.xu.spring.aop;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.InvocationHandlerAdapter;
import net.bytebuddy.matcher.ElementMatchers;


/**
 * created by Xu on 2024/5/23 10:58.
 *
 */
public class ProxyResolver {
    final Logger logger = LoggerFactory.getLogger(getClass());

    final ByteBuddy byteBuddy = new ByteBuddy();

    private static ProxyResolver INSTANCE = null;

    public static ProxyResolver getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ProxyResolver();
        }
        return INSTANCE;
    }

    private ProxyResolver() {}

    @SuppressWarnings("unchecked")
    public <T> T createProxy(T bean, InvocationHandler handler) {
        Class<?> targetClass = bean.getClass();
        logger.atDebug().log("create proxy for bean {} @{}", targetClass.getName(), Integer.toHexString(bean.hashCode()));
        // 动态创建代理类
        Class<?> proxyClass = this.byteBuddy
                // 这个代理类是 targetClass 的子类，子类使用默认的构造方法
                .subclass(targetClass, ConstructorStrategy.Default.DEFAULT_CONSTRUCTOR)
                // 处理 target 所有 public 的方法的调用
                .method(ElementMatchers.isPublic())
                // 拦截器，将所有对子类的 public 方法调用转发到原本的 bean 中执行
                .intercept(InvocationHandlerAdapter.of(
                        new InvocationHandler() {
                            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                                // 将方法的调用转发到原本的bean上
                                return handler.invoke(bean, method, args);
                            }
                        }
                )).make()
                .load(targetClass.getClassLoader())
                .getLoaded();
        Object proxy;
        try {
            proxy = proxyClass.getConstructor().newInstance();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return (T) proxy;
    }
}
