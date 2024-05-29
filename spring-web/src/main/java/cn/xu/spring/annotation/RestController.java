package cn.xu.spring.annotation;

import java.lang.annotation.*;

/**
 * created by Xu on 2024/5/29 11:44.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface RestController {
    String value() default "";
}