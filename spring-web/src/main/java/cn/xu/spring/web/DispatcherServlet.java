package cn.xu.spring.web;

import cn.xu.spring.annotation.*;
import cn.xu.spring.context.ApplicationContext;
import cn.xu.spring.context.ConfigurableApplicationContext;
import cn.xu.spring.exception.ErrorResponseException;
import cn.xu.spring.exception.NestedRuntimeException;
import cn.xu.spring.exception.ServerErrorException;
import cn.xu.spring.exception.ServerWebInputException;
import cn.xu.spring.io.PropertyResolver;
import cn.xu.spring.utils.ClassUtils;
import cn.xu.spring.web.utils.WebUtils;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * created by Xu on 2024/5/26 20:55.
 */
public class DispatcherServlet extends HttpServlet {
    Logger logger = LoggerFactory.getLogger(getClass());

    ApplicationContext applicationContext;

    ViewResolver viewResolver;

    String resourcePath;
    String faviconPath;

    List<Dispatcher> getDispatchers = new ArrayList<>();
    List<Dispatcher> postDispatchers = new ArrayList<>();

    public DispatcherServlet(ApplicationContext applicationContext, PropertyResolver propertyResolver) {
        this.applicationContext = applicationContext;
        this.viewResolver = applicationContext.getBean(ViewResolver.class);
        this.resourcePath = propertyResolver.getProperty("${xu-spring.web.static-path:/static/}");
        this.faviconPath = propertyResolver.getProperty("${xu-spring.web.favicon-path:/favicon.ico}");
        if (resourcePath.endsWith("/")) {
            this.resourcePath += "/";
        }
    }

    @Override
    public void init() throws ServletException {
        logger.info("init {}.", getClass().getName());
        // 扫描全部bean检查是否被 @Controller 或者 @RestController 标记
        for (var def : ((ConfigurableApplicationContext) this.applicationContext).findBeanDefinitions(Object.class)) {
            Class<?> beanClass = def.getBeanClass();
            Object instance = def.getInstance();
            Controller controller = beanClass.getAnnotation(Controller.class);
            RestController restController = beanClass.getAnnotation(RestController.class);
            if (controller != null && restController != null) {
                throw new ServletException("Found @Controller and @RestController on class: " + beanClass.getName());
            }
            if (controller != null) {
                addController(false, def.getName(), instance);
            }
            if (restController != null) {
                addController(true, def.getName(), instance);
            }
        }
    }

    @Override
    public void destroy() {
        this.applicationContext.close();
    }

    public void addController(boolean isRest, String name, Object instance) throws ServletException {
        logger.info("add {} controller '{}': {}", isRest ? "REST" : "MVC", name, instance.getClass().getName());
        addMethods(isRest, name, instance, instance.getClass());
    }

    /**
     * 添加路由处理方法
     * 重点在于检查是否存在GetMapping和PostMapping注解。如果存在，它会创建一个新的Dispatcher对象并将其添加到相应的调度器列
     */
    public void addMethods(boolean isRest, String name, Object instance, Class<?> clazz) throws ServletException {
        for (Method method : clazz.getDeclaredMethods()) {
            GetMapping getMapping = method.getAnnotation(GetMapping.class);
            if (getMapping != null) {
                checkMethod(method);
                this.getDispatchers.add(new Dispatcher("GET", isRest, instance, method, getMapping.value()));
            }
            PostMapping postMapping = method.getAnnotation(PostMapping.class);
            if (postMapping != null) {
                checkMethod(method);
                this.postDispatchers.add(new Dispatcher("POST", isRest, instance, method, postMapping.value()));
            }
        }
        Class<?> superclass = clazz.getSuperclass();
        if (superclass != null) {
            addMethods(isRest, name, instance, superclass);
        }
    }

    /**
     * 方法的检查和设置访问权限
     */
    void checkMethod(Method method) throws ServletException {
        int modifiers = method.getModifiers();
        if (Modifier.isStatic(modifiers)) {
            throw new ServletException("Cannot do URL mapping to static method: " + method);
        }
        method.setAccessible(true);
    }

    /**
     * 处理get请求需要处理是获取静态资源还是get类型的请求
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String url = req.getRequestURI();
        if (url.equals(this.faviconPath) || url.equals(this.resourcePath)) {
            doResource(url, req, resp);
        } else {
            doService(url, req, resp, this.getDispatchers);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doService(req, resp, this.postDispatchers);
    }


    void doService(HttpServletRequest req, HttpServletResponse resp, List<Dispatcher> dispatchers) throws ServletException, IOException {
        String url = req.getRequestURI();
        try {
            doService(url, req, resp, dispatchers);
        } catch (ErrorResponseException e) {
            logger.warn("process request failed with status " + e.statusCode + ": " + url, e);
            if (!resp.isCommitted()) {
                resp.resetBuffer();
                resp.sendError(e.statusCode);
            }

        } catch (RuntimeException | ServletException | IOException e) {
            logger.warn("process request failed: " + url, e);
            throw e;
        } catch (Exception e) {
            logger.warn("process request failed: " + url, e);
            throw new NestedRuntimeException(e);
        }

    }

    void doService(String url, HttpServletRequest req, HttpServletResponse resp, List<Dispatcher> dispatchers) throws ServletException, IOException {
        for (Dispatcher dispatcher : dispatchers) {
            Result result = dispatcher.process(url, req, resp);

        }


    }

    protected void doResource(String url, HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        ServletContext ctx = req.getServletContext();
        try(InputStream input = ctx.getResourceAsStream(url)) {
            if (input == null) {
                resp.sendError(404, "Not Found");
            } else {
                // 将资源内容以ServletContext自动推断的MIME类型设置响应的内容类型，输出到响应输出流中
                String file = url;
                int n = url.lastIndexOf("/");
                if (n >= 0) {
                    file = file.substring(n + 1);
                }
                String mime = ctx.getMimeType(file);
                if (mime == null) {
                    mime = "application/octet-stream";
                }
                resp.setContentType(mime);
                ServletOutputStream output = resp.getOutputStream();
                input.transferTo(output);
                output.flush();
            }
        }
    }


    static class Dispatcher {
        final static Result NOT_PROCESSED = new Result(false, null);
        final Logger logger = LoggerFactory.getLogger(getClass());

        boolean isRest;
        boolean isResponseBody;
        boolean isVoid;
        Pattern urlPattern;
        Object controller;
        Method handlerMethod;
        Param[] methodParameters;

        public Dispatcher(String httpMethod, boolean isRest, Object controller, Method method, String urlPattern) throws ServletException {
            this.isRest = isRest;
            this.isResponseBody = method.isAnnotationPresent(ResponseBody.class);
            this.isVoid = method.getReturnType() == void.class;
            this.urlPattern = Pattern.compile(urlPattern);
            this.controller = controller;
            this.handlerMethod = method;
            Parameter[] params = method.getParameters();
            Annotation[][] paramsAnnos = method.getParameterAnnotations();
            this.methodParameters = new Param[params.length];
            for (int i = 0; i < params.length; i++) {
                this.methodParameters[i] = new Param(httpMethod, method, params[i], paramsAnnos[i]);
            }
            logger.atDebug().log("mapping {} to handler {}.{}", urlPattern, controller.getClass().getSimpleName(), method.getName());
            if (logger.isDebugEnabled()) {
                for (var p : this.methodParameters) {
                    logger.debug("> parameter: {}", p);
                }
            }
        }

        Result process(String url, HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            // todo
            return null;
        }

        Object convertToType(Class<?> classType, String s) {
            if (classType == String.class) {
                return s;
            } else if (classType == boolean.class || classType == Boolean.class) {
                return Boolean.valueOf(s);
            } else if (classType == int.class || classType == Integer.class) {
                return Integer.valueOf(s);
            } else if (classType == long.class || classType == Long.class) {
                return Long.valueOf(s);
            } else if (classType == byte.class || classType == Byte.class) {
                return Byte.valueOf(s);
            } else if (classType == short.class || classType == Short.class) {
                return Short.valueOf(s);
            } else if (classType == float.class || classType == Float.class) {
                return Float.valueOf(s);
            } else if (classType == double.class || classType == Double.class) {
                return Double.valueOf(s);
            } else {
                throw new ServerErrorException("Could not determine argument type: " + classType);
            }
        }

        String getOrDefault(HttpServletRequest request, String name, String defaultValue) {
            String s = request.getParameter(name);
            if (s != null) {
                if (WebUtils.DEFAULT_PARAM_VALUE.equals(defaultValue)) {
                    throw new ServerWebInputException("Request parameter '" + name + "' not found.");
                }
                return defaultValue;
            }
            return s;
        }

    }


    static enum ParamType {
        PATH_VARIABLE, REQUEST_PARAM, REQUEST_BODY, SERVLET_VARIABLE;
    }

    static class Param {
        String name;
        ParamType paramType;
        Class<?> classType;
        String defaultValue;

        public Param(String methodName, Method method, Parameter parameter, Annotation[] annotations) throws ServletException {
            PathVariable pv = ClassUtils.getAnnotation(annotations, PathVariable.class);
            RequestParam rp = ClassUtils.getAnnotation(annotations, RequestParam.class);
            RequestBody rb = ClassUtils.getAnnotation(annotations, RequestBody.class);
            int total = (pv == null ? 0 : 1) + (rp == null ? 0 : 1) + (rb == null ? 0 : 1);
            if (total > 1) {
                throw new ServletException("Annotation @PathVariable, @RequestParam and @RequestBody cannot be combined at method: " + method);
            }
            this.classType = parameter.getType();
            if (pv != null) {
                this.name = pv.value();
                this.paramType = ParamType.PATH_VARIABLE;
            } else if (rp != null) {
                this.name = rp.value();
                this.paramType = ParamType.REQUEST_PARAM;
            } else if (rb != null) {
                this.paramType = ParamType.REQUEST_BODY;
            } else {
                this.paramType = ParamType.SERVLET_VARIABLE;
                // 检查参数类型是否为合法的Servlet变量类型
                if (this.classType != HttpServletRequest.class && this.classType != HttpServletResponse.class &&
                        this.classType != HttpSession.class && this.classType != ServletContext.class) {
                    throw new ServerErrorException("(Missing annotation?) Unsupported argument type: " + classType + " at method: " + method);
                }
            }
        }

        @Override
        public String toString() {
            return "Param [name=" + name + ", paramType=" + paramType + ", classType=" + classType + ", defaultValue=" + defaultValue + "]";
        }

    }

    static record Result(boolean processed, Object result) {}
}
