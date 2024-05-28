package cn.xu.spring.web.utils;

import cn.xu.spring.context.ApplicationContextUtils;
import cn.xu.spring.io.PropertyResolver;
import cn.xu.spring.utils.ClassPathUtils;
import cn.xu.spring.utils.YamlUtils;
import cn.xu.spring.web.DispatcherServlet;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.Properties;

/**
 * created by Xu on 2024/5/28 15:53.
 */
public class WebUtils {
    private WebUtils() {}

    static final Logger logger = LoggerFactory.getLogger(WebUtils.class);

    static final String CONFIG_APP_YAML = "/application.yaml";

    static final String CONFIG_APP_PROP = "/application.properties";

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
}
