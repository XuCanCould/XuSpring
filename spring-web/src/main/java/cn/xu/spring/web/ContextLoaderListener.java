package cn.xu.spring.web;

import cn.xu.spring.context.AnnotationConfigApplicationContext;
import cn.xu.spring.context.ApplicationContext;
import cn.xu.spring.exception.NestedRuntimeException;
import cn.xu.spring.io.PropertyResolver;
import cn.xu.spring.web.utils.WebUtils;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * created by Xu on 2024/5/26 21:21.
 */
public class ContextLoaderListener implements ServletContextListener {
    Logger logger = LoggerFactory.getLogger(getClass());


    @Override
    public void contextInitialized(ServletContextEvent sce) {
        logger.info("init {}", this.getClass().getSimpleName());
        var servletContext = sce.getServletContext();
        var propertyResolver = WebUtils.createPropertyResolver();
        String encoding = propertyResolver.getProperty("${xu-spring.web.character-encoding:UTF-8}");
        servletContext.setRequestCharacterEncoding(encoding);
        servletContext.setResponseCharacterEncoding(encoding);
        // 获取 web.xml 中的 configuration
        var applicationContext = createApplicationContext(servletContext.getInitParameter("configuration"), propertyResolver);
        WebUtils.registerDispatcherServlet(servletContext, propertyResolver);

        servletContext.setAttribute("applicationContext", applicationContext);
    }

    ApplicationContext createApplicationContext(String configClassName, PropertyResolver propertyResolver) {
        logger.info("init ApplicationContext by configuration: {}", configClassName);
        if (configClassName == null || configClassName.isEmpty()) {
            throw new NestedRuntimeException("Cannot init ApplicationContext for missing init param name: configuration");
        }
        Class<?> configClass;
        try {
            configClass = Class.forName(configClassName);
        } catch (ClassNotFoundException e) {
            throw new NestedRuntimeException("Could not load class from init param 'configuration': " + configClassName);
        }
        return new AnnotationConfigApplicationContext(configClass, propertyResolver);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (sce.getServletContext().getAttribute("applicationContext") instanceof ApplicationContext applicationContext) {
            // 关闭容器。IOC容器提供的接口，会进行bean的销毁。
            applicationContext.close();
        }
    }
}
