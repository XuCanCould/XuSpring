package cn.xu.spring.web;

import cn.xu.spring.context.AnnotationConfigApplicationContext;
import cn.xu.spring.io.PropertyResolver;
import cn.xu.spring.context.ApplicationContext;
import cn.xu.spring.web.utils.WebUtils;
import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * created by Xu on 2024/5/30 21:04.
 * 处理IoC容器，以及注册Servlet、Filter
 */
public class ContextLoaderInitializer implements ServletContainerInitializer {
    final Logger logger = LoggerFactory.getLogger(getClass());

    final Class<?> configClass;

    final PropertyResolver propertyResolver;

    public ContextLoaderInitializer(Class<?> configClass, PropertyResolver propertyResolver) {
        this.configClass = configClass;
        this.propertyResolver = propertyResolver;
    }

    @Override
    public void onStartup(Set<Class<?>> set, ServletContext ctx) throws ServletException {
        logger.info("Servlet container start. ServletContext = {}", ctx);

        // 设置请求、响应的编码
        String encoding = propertyResolver.getProperty("${xu-spring.web.character-encoding:UTF-8}");
        ctx.setRequestCharacterEncoding(encoding);
        ctx.setResponseCharacterEncoding(encoding);

        // 得到 ServletContext
        WebMvcConfiguration.setServletContext(ctx);
        ApplicationContext context = new AnnotationConfigApplicationContext(this.configClass, this.propertyResolver);
        logger.info("Application context created: {} ", context);

        // 注册过滤器
        WebUtils.registerFilters(ctx);
        // 注册路由
        WebUtils.registerDispatcherServlet(ctx, this.propertyResolver);
    }
}
