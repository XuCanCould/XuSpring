package cn.xu.spring.web;

import cn.xu.spring.annotation.Autowired;
import cn.xu.spring.annotation.Bean;
import cn.xu.spring.annotation.Configuration;
import cn.xu.spring.annotation.Value;
import jakarta.servlet.ServletContext;

import java.util.Objects;

/**
 * created by Xu on 2024/5/28 12:02.
 * 简化Web应用程序配置
 */
@Configuration
public class WebMvcConfiguration {
    private static ServletContext servletContext = null;

    public static void setServletContext(ServletContext stc) {
        servletContext = stc;
    }

    @Bean(initMethod = "init")
    ViewResolver viewResolver(
                               @Autowired ServletContext servletContext,
                               @Value("${xu-spring.web.freemarker.template-path:/WEB-INF/templates}") String templatePath, //
                               @Value("${xu-spring.web.freemarker.template-encoding:UTF-8}") String templateEncoding) {
        return new FreeMarkerViewResolver(servletContext, templatePath, templateEncoding);
    }

    @Bean
    ServletContext servletContext() {
        return Objects.requireNonNull(servletContext, "ServletContext is not set.");
    }
}
