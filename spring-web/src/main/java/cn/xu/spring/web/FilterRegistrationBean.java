package cn.xu.spring.web;

import java.util.List;
import jakarta.servlet.Filter;

/**
 * created by Xu on 2024/5/29 19:12.
 */
public abstract class FilterRegistrationBean {
    public abstract List<String> getUrlPatterns();

    public abstract Filter getFilter();

    /**
     * 通过类名得到名称，比如：
     * ApiFilterRegistrationBean -> apiFilter
     * ApiFilterRegistration -> apiFilter
     * ApiFilterReg -> apiFilterReg
     */
    public String getName() {
        String name = getClass().getSimpleName();
        name = Character.toLowerCase(name.charAt(0)) + name.substring(1);
        if (name.endsWith("FilterRegistrationBean") && name.length() > "FilterRegistrationBean".length()) {
            return name.substring(0, name.length() - "FilterRegistrationBean".length());
        }
        if (name.endsWith("FilterRegistration") && name.length() > "FilterRegistration".length()) {
            return name.substring(0, name.length() - "FilterRegistration".length());
        }
        return name;
    }

}
