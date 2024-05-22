package cn.xu.spring.annotation;

import java.lang.annotation.*;

/**
 * created by Xu on 2024/5/6 20:16.
 * spring 中 @Autowired 注解用于对象的自动注入
 */
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Autowired {

    // 自动装配的依赖项通常是必须的
    boolean value() default true;


    String name() default "";
}
