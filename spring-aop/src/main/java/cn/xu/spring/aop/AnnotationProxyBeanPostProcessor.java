package cn.xu.spring.aop;

import cn.xu.spring.context.*;
import cn.xu.spring.exception.AopConfigException;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * created by Xu on 2024/5/23 13:25.
 */
public abstract class AnnotationProxyBeanPostProcessor<A extends Annotation> implements BeanPostProcessor {
    // 保存原始Bean的引用
    Map<String, Object> originBeans = new HashMap<>();

    Class<A> annotationClass;

    public AnnotationProxyBeanPostProcessor() {
        this.annotationClass = getParameterizedType();
    }

    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        Class<?> beanClass = bean.getClass();
        A annotation = beanClass.getAnnotation(annotationClass);
        if (annotation != null) {
            String handlerName;
            try {
                handlerName = (String) annotation.annotationType().getMethod("value").invoke(annotation);
            } catch (ReflectiveOperationException e) {
                throw new AopConfigException(String.format("@%s must have value() returned String type.", this.annotationClass.getSimpleName()), e);
            }
            Object proxy = createProxy(beanClass, bean, handlerName);
            originBeans.put(beanName, bean);
            return proxy;
        } else {
            return bean;
        }
    }

    Object createProxy(Class<?> beanClass, Object bean, String handlerName) {
        ConfigurableApplicationContext context = (ConfigurableApplicationContext) ApplicationContextUtils.getRequiredApplicationContext();
        BeanDefinition definition = context.findBeanDefinition(handlerName);
        if (definition == null) {
            throw new AopConfigException(String.format("@%s proxy handler '%s' not found.", this.annotationClass.getSimpleName(), handlerName));
        }
        Object handlerBean = definition.getInstance();
        if (handlerBean == null) {
            handlerBean = context.createBeanAsEarlySingleton(definition);
        }
        if (handlerBean instanceof InvocationHandler handler) {
            return ProxyResolver.getInstance().createProxy(bean, handler);
        } else {
            throw new AopConfigException(String.format("@%s proxy handler '%s' is not type of %s.", this.annotationClass.getSimpleName(), handlerName,
                    InvocationHandler.class.getName()));
        }
    }


    @Override
    public Object postProcessOnSetProperty(Object bean, String beanName) {
        Object origin = this.originBeans.get(beanName);
        return origin != null ? origin : bean;
    }

    /**
     * 获取当前类的泛型父类类型
     */
    @SuppressWarnings("unchecked")
    private Class<A> getParameterizedType() {
        Type type = getClass().getGenericSuperclass();
        // 要求type是泛型类型
        if (!(type instanceof ParameterizedType)) {
            throw new IllegalArgumentException("Class " + getClass().getName() + " does not have parameterized type.");
        }
        ParameterizedType pt = (ParameterizedType) type;
        // 得到实际参数类型
        Type[] arguments = pt.getActualTypeArguments();
        // 限制参数个数
        if (arguments.length != 1) {
            throw new IllegalArgumentException("Class " + getClass().getName() + " has more than 1 parameterized types.");
        }
        Type r = arguments[0];
        if (!(r instanceof Class<?>)) {
            throw new IllegalArgumentException("Class " + getClass().getName() + " does not have parameterized type of class.");
        }
        return (Class<A>) r;
    }

}
