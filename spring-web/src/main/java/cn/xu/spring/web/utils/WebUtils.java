package cn.xu.spring.web.utils;

import cn.xu.spring.context.ApplicationContext;
import cn.xu.spring.context.ApplicationContextUtils;
import cn.xu.spring.io.PropertyResolver;
import cn.xu.spring.utils.ClassPathUtils;
import cn.xu.spring.utils.YamlUtils;
import cn.xu.spring.web.DispatcherServlet;
import cn.xu.spring.web.FilterRegistrationBean;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.ServletContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.UncheckedIOException;
import java.util.*;

/**
 * created by Xu on 2024/5/28 15:53.
 */
public class WebUtils {
    private WebUtils() {}

    public static final String DEFAULT_PARAM_VALUE = "\0\t\0\t\0";

    static final Logger logger = LoggerFactory.getLogger(WebUtils.class);

    static final String CONFIG_APP_YAML = "/application.yaml";

    static final String CONFIG_APP_PROP = "/application.properties";

    /**
     * 注册核心组件 DispatcherServlet 并将其添加到 ServletContext 中
     * @param servletContext
     * @param propertyResolver
     */
    public static void registerDispatcherServlet(ServletContext servletContext, PropertyResolver propertyResolver) {
        DispatcherServlet dispatcherServlet = new DispatcherServlet(ApplicationContextUtils.getApplicationContext(), propertyResolver);
        logger.info("register servlet {} for URL '/'", dispatcherServlet.getClass().getName());
        var dispatcherReg = servletContext.addServlet("dispatcherServlet", dispatcherServlet);
        dispatcherReg.addMapping("/");
        dispatcherReg.setLoadOnStartup(0);
    }

    public static PropertyResolver createPropertyResolver() {
        final Properties properties = new Properties();
        // 尝试加载 yml 配置
        try {
            Map<String, Object> ymlMap = YamlUtils.readYaml(CONFIG_APP_YAML);
            logger.info("load config: {}", CONFIG_APP_YAML);
            for (String key : ymlMap.keySet()) {
                Object value = ymlMap.get(key);
                if (value instanceof String strValue) {
                    properties.put(key, strValue);
                }
            }
        } catch (UncheckedIOException e) {
            if (e.getCause() instanceof FileNotFoundException) {
                ClassPathUtils.readInputStream(CONFIG_APP_PROP, input -> {
                    logger.info("load config: {}", CONFIG_APP_PROP);
                    properties.load(input);
                    return true;
                });
            }
        }
        return new PropertyResolver(properties);
    }

    /**
     * 注册过滤器
     */
    public static void registerFilters(ServletContext servletContext) {
        ApplicationContext context = ApplicationContextUtils.getRequiredApplicationContext();
        for (var registrationBean : context.getBeans(FilterRegistrationBean.class)) {
            // 处理接口对象的url和过滤方法
            List<String> urlPatterns = registrationBean.getUrlPatterns();
            if (urlPatterns== null || urlPatterns.isEmpty()) {
                throw new IllegalArgumentException("no url patterns for {}" + registrationBean.getClass().getName());
            }
            // 将filter添加到ServletContext中
            Filter filter = Objects.requireNonNull(registrationBean.getFilter(), "FilterRegistrationBean.getFilter() must not return null.");
            logger.info("register filter '{}' {} for URLs: {}", registrationBean.getName(), filter.getClass().getName(), String.join(", ", urlPatterns));
            var filterReg = servletContext.addFilter(registrationBean.getName(), filter);
            // java.util.EnumSet<jakarta.servlet.DispatcherType>enumSet, 请求调度时应用过滤器
            // boolean b, 过滤器在所有子请求前应用
            // string...strings, 将URL转为字符串数组并添加
            // String[]::new等价于匿名函数 (String[] args) -> new String[args.length]
            filterReg.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, urlPatterns.toArray(String[]::new));
        }
    }
}
