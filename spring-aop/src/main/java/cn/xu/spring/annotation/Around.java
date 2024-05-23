package cn.xu.spring.annotation;

import java.lang.annotation.*;

/**
 * created by Xu on 2024/5/23 13:13.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface Around {
    // 调用处理器的名称
    String value();
}
