package cn.xu.spring.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * created by Xu on 2024/5/29 11:45.
 */
@Target(ElementType.METHOD)
public @interface GetMapping {
    // url 映射
    String value();
}
