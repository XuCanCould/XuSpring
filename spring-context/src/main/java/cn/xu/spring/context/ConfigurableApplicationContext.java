package cn.xu.spring.context;

import jakarta.annotation.Nullable;

import java.util.List;

/**
 * created by Xu on 2024/5/23 9:42.
 * 框架级别的接口
 */
public interface ConfigurableApplicationContext extends ApplicationContext {
    List<BeanDefinition> findBeanDefinitions(Class<?> type);

    @Nullable
    BeanDefinition findBeanDefinition(Class<?> type);

    @Nullable
    BeanDefinition findBeanDefinition(String name);

    @Nullable
    BeanDefinition findBeanDefinition(String name, Class<?> requiredType);

    Object createBeanAsEarlySingleton(BeanDefinition def);
}
