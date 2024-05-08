package cn.xu.spring.annotation;

import java.lang.annotation.*;

/**
 * created by Xu on 2024/5/6 19:55.
 * 定义为 bean，将这个对象的实例化交给spring
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Bean {
    // bean 名称默认为方法名称
    String value() default "";

    String initMethod() default "";

    String destroyMethod() default "";
}
