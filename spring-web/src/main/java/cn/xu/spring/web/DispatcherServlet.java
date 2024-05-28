package cn.xu.spring.web;

import cn.xu.spring.context.ApplicationContext;
import cn.xu.spring.io.PropertyResolver;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * created by Xu on 2024/5/26 20:55.
 */
public class DispatcherServlet extends HttpServlet {
    Logger logger = LoggerFactory.getLogger(getClass());

    ApplicationContext applicationContext;

    public DispatcherServlet(ApplicationContext applicationContext, PropertyResolver propertyResolver) {
        this.applicationContext = applicationContext;
    }

    @Override
    public void destroy() {
        this.applicationContext.close();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doGet(req, resp);
    }
}
