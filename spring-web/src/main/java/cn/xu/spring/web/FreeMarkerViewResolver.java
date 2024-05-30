package cn.xu.spring.web;

import cn.xu.spring.exception.ServerErrorException;
import freemarker.cache.TemplateLoader;
import freemarker.core.HTMLOutputFormat;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import freemarker.template.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Map;
import java.util.Objects;

/**
 * created by Xu on 2024/5/29 16:36.
 * spring内置FreeMarker引擎
 */
public class FreeMarkerViewResolver implements ViewResolver {

    final Logger logger = LoggerFactory.getLogger(getClass());

    final String templatePath;
    final String templateEncoding;
    final ServletContext servletContext;

    Configuration config;

    public FreeMarkerViewResolver(ServletContext servletContext, String templatePath, String templateEncoding) {
        this.servletContext = servletContext;
        this.templatePath = templatePath;
        this.templateEncoding = templateEncoding;
    }

    @Override
    public void init() {
        logger.info("init {}, set template path: {}", getClass().getSimpleName(), this.templatePath);
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_32);
        cfg.setOutputFormat(HTMLOutputFormat.INSTANCE);
        cfg.setDefaultEncoding(this.templateEncoding);
        cfg.setTemplateLoader(new ServletTemplateLoader(this.servletContext, this.templatePath));
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.HTML_DEBUG_HANDLER);
        cfg.setAutoEscapingPolicy(Configuration.ENABLE_IF_DEFAULT_AUTO_ESCAPING_POLICY);
        cfg.setLocalizedLookup(false);
        // 默认对象包装器：将Java对象转换成 Free marker模板可以理解的数据模型时使用的类
        var ow = new DefaultObjectWrapper(Configuration.VERSION_2_3_32);
        ow.setExposeFields(true);   // 允许在Freemarker模板中直接访问Java对象的字段
        cfg.setObjectWrapper(ow);   // 设置为Configuration对象的默认对象包装器
        this.config = cfg;
    }

    /**
     * 渲染视图。根据给定的视图名称获取模板，结合模型数据，然后将渲染结果写入HTTP响应
     */
    @Override
    public void render(String viewName, Map<String, Object> module, HttpServletRequest request, HttpServletResponse response) throws IOException {
        Template template = null;
        try {
            template = this.config.getTemplate(viewName);
        } catch (Exception e) {
            throw new ServerErrorException("View not found: " + viewName);
        }
        PrintWriter pw = response.getWriter();
        try {
            template.process(module, pw);
        } catch (Exception e) {
            throw new ServerErrorException("View rendering failed: " + viewName, e);
        }
        pw.flush();
    }
}

class ServletTemplateLoader implements TemplateLoader {

    final Logger logger = LoggerFactory.getLogger(getClass());

    private final ServletContext servletContext;

    private final String subdirPath;

    public ServletTemplateLoader(ServletContext servletContext, String subdirPath) {
        Objects.requireNonNull(servletContext);
        Objects.requireNonNull(subdirPath);

        // 保证跨平台环境下路径一致（Windows \ Linux /）
        subdirPath = subdirPath.replace('\\', '/');
        // 资源访问路径必须以 / 开头
        if (!subdirPath.endsWith("/")) {
            subdirPath += "/";
        }
        // 简化路径拼接
        if (subdirPath.startsWith("/")) {
            subdirPath = "/" + subdirPath;
        }

        this.servletContext = servletContext;
        this.subdirPath = subdirPath;
    }


    @Override
    public Object findTemplateSource(String name) throws IOException {
        String fullPath = subdirPath + name;

        try {
            String realPath = servletContext.getRealPath(fullPath);
            logger.atDebug().log("load template {}: real path: {}", name, realPath);
            if (realPath != null) {
                File file = new File(realPath);
                if (file.canRead() && file.isFile()) {
                    return file;
                }
            }
        } catch (SecurityException e) {
            // ignore
        }
        return null;
    }

    @Override
    public long getLastModified(Object templateSource) {
        if (templateSource instanceof File) {
            return ((File) templateSource).lastModified();
        }
        return 0;
    }

    @Override
    public Reader getReader(Object templateSource, String encoding) throws IOException {
        if (templateSource instanceof File) {
            return new InputStreamReader(new FileInputStream((File) templateSource), encoding);
        }
        throw new IOException("File not found.");
    }

    @Override
    public void closeTemplateSource(Object templateSource) throws IOException {
    }

    public Boolean getURLConnectionUsesCaches() {
        return Boolean.FALSE;
    }
}