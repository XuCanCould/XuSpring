package cn.xu.spring.utils;

import cn.xu.spring.annotation.Bean;
import cn.xu.spring.annotation.Component;
import cn.xu.spring.exception.BeanDefinitionException;
import jakarta.annotation.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * created by Xu on 2024/5/6 20:30.
 */
public class ClassUtils {
    public static <A extends Annotation> A findAnnotation(Class<?> target, Class<A> annoClass) {
        A annotation = target.getAnnotation(annoClass);
        for (Annotation anno : target.getAnnotations()) {
            Class<? extends Annotation> annoType = anno.annotationType();
            // 对于非 java.lang.annotation 的注解，递归查找
            if (!annoType.getPackageName().equals("java.lang.annotation")) {
                // 处理注解的继承和元注解的情况
                A found = findAnnotation(annoType, annoClass);
                if (found != null) {
                    if (annotation != null) {
                        throw new BeanDefinitionException("Duplicate @" + annoClass.getSimpleName() + " found on class " + target.getSimpleName());
                    }
                    annotation = found;
                }
            }
        }
        return annotation;
    }

    /**
     * 获取 (被 @Component 注解在 类 上的) beanName
     */
    public static String getBeanName(Class<?> clazz) {
        String name = "";
        Component component = clazz.getAnnotation(Component.class);
        if (component != null) {
            name = component.value();
        } else {
            // 如果当前类的注解没有 @Component 注解，则遍历所有注解，寻找是否有 @Component 注解
            // 比如当前类被 @Service 注解，而 @Component 是 @Service 的元注解
            for (Annotation annotation : clazz.getAnnotations()) {
                if (findAnnotation(annotation.annotationType(), Component.class) != null) {
                    try {
                        name = (String) annotation.annotationType().getMethod("value").invoke(annotation);
                    } catch (ReflectiveOperationException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        if (name.isEmpty()) {
            name = clazz.getSimpleName();
            name = Character.toLowerCase(name.charAt(0)) + name.substring(1);
        }
        return name;
    }


    /**
     * 获取 (被 @Component 注解在 类 上的) beanName
     */
    public static String getBeanName(Method method) {
        Bean annotation = method.getAnnotation(Bean.class);
        String name = annotation.value();
        if (name.isEmpty()) {
            name = method.getName();
        }
        return name;
    }

    @Nullable
    public static Method findAnnotationMethod(Class<?> classType, Class<? extends Annotation> annoClass) {
        List<Method> methodsCollect = Arrays.stream(classType.getDeclaredMethods()).filter(method -> method.isAnnotationPresent(annoClass)).map(method -> {
            if (method.getParameterCount() != 0) {
                throw new BeanDefinitionException(
                        String.format("Method '%s' with @%s must not have argument: %s", method.getName(), annoClass.getSimpleName(), classType.getName()));
            }
            return method;
        }).collect(Collectors.toList());
        if (methodsCollect.isEmpty()) {
            return null;
        }
        if (methodsCollect.size() == 1) {
            return methodsCollect.get(0);
        }
        throw new BeanDefinitionException(String.format("Multiple methods with @%s found in class: %s", annoClass.getSimpleName(), classType.getName()));
    }


    @Nullable
    public static <A extends Annotation> A getAnnotation(Annotation[] annotations, Class<A> annoClass) {
        for (Annotation annotation : annotations) {
            if (annoClass.isInstance(annotation)) {
                return (A) annotation;
            }
        }
        return null;
    }

    /**
     * 获取指定名称的方法
     */
    public static Method getNamedMethod(Class<?> clazz, String methodName) {
        try {
            return clazz.getDeclaredMethod(methodName);
        } catch (NoSuchMethodException e) {
            throw new BeanDefinitionException(String.format("Method '%s' not found in class: %s", methodName, clazz.getName()));
        }
    }
}
