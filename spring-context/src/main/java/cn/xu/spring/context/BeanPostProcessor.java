package cn.xu.spring.context;

/**
 * created by Xu on 2024/5/21 20:31.
 * spring 中实现动态代理
 */
public interface BeanPostProcessor {

    /**
     * 在 new Bean() 之后执行
     * @param bean 注入依赖时使用的原始的bean
     * @param beanName
     * @return
     */
    default Object postProcessBeforeInitialization(Object bean, String beanName) {
        return bean;
    }

    /**
     * 在 bean.init() 之后执行
     */
    default Object postProcessAfterInitialization(Object bean, String beanName) {
        return bean;
    }

    /**
     * 在调用 setter 方法注入前调用 / 注入依赖时使用的bean实例
     */
    default Object postProcessOnSetProperty(Object bean, String beanName) {
        return bean;
    }
}
