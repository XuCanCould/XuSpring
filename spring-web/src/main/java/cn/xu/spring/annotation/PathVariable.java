package cn.xu.spring.annotation;

import java.lang.annotation.*;

/**
 * created by Xu on 2024/5/29 15:12.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PathVariable {
    String value();
}
