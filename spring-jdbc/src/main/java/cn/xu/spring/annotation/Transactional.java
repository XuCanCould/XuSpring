package cn.xu.spring.annotation;

import java.lang.annotation.*;

/**
 * created by Xu on 2024/5/24 15:50.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface Transactional {
    String value() default "platformTransactionManager";
}
