package cn.xu.spring.boot;

import cn.xu.spring.io.PropertyResolver;
import cn.xu.spring.web.ContextLoaderInitializer;
import cn.xu.spring.web.utils.WebUtils;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Server;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.StandardRoot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.nio.file.Paths;
import java.util.Set;

/**
 * created by Xu on 2024/5/30 20:53.
 * 启动嵌入式Tomcat
 */
public class XuSpringApplication {

    static final String CONFIG_APP_YAML = "/application.yaml";

    static final String CONFIG_APP_PROP = "/application.properties";

    final Logger logger = LoggerFactory.getLogger(XuSpringApplication.class);

    public static void run(String webDir, String baseDir, Class<?> configClass, String ... args) throws Exception {
        new XuSpringApplication().start(webDir, baseDir, configClass, args);
    }

    public void start(String webDir, String baseDir, Class<?> configClass, String... args) throws Exception {
        // banner 就不打印了
        // 启动信息
        final long startTime = System.currentTimeMillis();
        final int javaVersion = Runtime.version().feature();
        final long pid = ManagementFactory.getRuntimeMXBean().getPid(); // 进程号
        final String user = System.getProperty("user.name");    // 炒作系统用户名
        final String pwd = Paths.get("").toAbsolutePath().toString(); // 当前工作目录的绝对路径
        logger.info("Starting {} using Java {} with PID {} (started by {} in {})", configClass.getSimpleName(), javaVersion, pid, user, pwd);

        PropertyResolver propertyResolver = WebUtils.createPropertyResolver();
        Server server = startTomcat(webDir, baseDir, configClass, propertyResolver);

        // 启动结束信息
        final long endTime = System.currentTimeMillis();
        final String appTime = String.format("%.3f", (endTime - startTime) / 1000.0);
        final String jvmTime = String.format("%.3f", ManagementFactory.getRuntimeMXBean().getUptime() / 1000.0);
        logger.info("Started {} in {} seconds (process running for {})", configClass.getSimpleName(), appTime, jvmTime);

        server.await();
    }

    /**
     * 启动嵌入式Tomcat
     */
    protected Server startTomcat(String webDir, String baseDir, Class<?> configClass, PropertyResolver propertyResolver) throws LifecycleException {
        int port = propertyResolver.getProperty("${server.port:8080}", int.class);
        logger.info("starting Tomcat at port {}...", port);
        Tomcat tomcat = new Tomcat();
        tomcat.setPort(port);
        // 失败时抛出异常
        tomcat.getConnector().setThrowOnFailure(true);
        Context context = tomcat.addWebapp("", new File(webDir).getAbsolutePath());
        // 资源路径
        WebResourceRoot resources = new StandardRoot(context);
        resources.addPreResources(new DirResourceSet(resources, "/WEB-INF/classes", new File(baseDir).getAbsolutePath(), "/"));
        context.setResources(resources);
        context.addServletContainerInitializer(new ContextLoaderInitializer(configClass, propertyResolver), Set.of());
        tomcat.start();
        logger.info("Tomcat started at port {}...", port);
        return tomcat.getServer();
    }
}
