package cn.xu.spring.annotation;

import cn.xu.spring.web.utils.WebUtils;

import java.lang.annotation.*;

/**
 * created by Xu on 2024/5/29 15:10.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequestParam {

    String value();

    String defaultValue() default WebUtils.DEFAULT_PARAM_VALUE;
}
