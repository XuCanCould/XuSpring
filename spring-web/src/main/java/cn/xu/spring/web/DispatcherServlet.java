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
import cn.xu.spring.web.utils.JsonUtils;
import cn.xu.spring.web.utils.PathUtils;
import cn.xu.spring.web.utils.WebUtils;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
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
            doService(req, resp, this.getDispatchers);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doService(req, resp, this.postDispatchers);
    }

    /**
     * 参数和异常处理
     */
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

    /**
     * 通过 dispatcher.process() 处理请求，重点是处理返回值
     */
    void doService(String url, HttpServletRequest req, HttpServletResponse resp, List<Dispatcher> dispatchers) throws Exception {
        for (Dispatcher dispatcher : dispatchers) {
            Result result = dispatcher.process(url, req, resp);
            if (result.processed()) {
                Object r = result.returnObject();

                if (dispatcher.isRest) {
                    // 调度器为REST类型，返回值直接写入HTTP响应体或抛出异常。
                    if (!resp.isCommitted()) {
                        resp.setContentType("application/json");
                    }
                    if (dispatcher.isResponseBody) {
                        // 处理 String 和 byte[] 类型
                        if (r instanceof String s) {
                            PrintWriter pw = resp.getWriter();
                            pw.write(s);
                            pw.flush();
                        } else if (r instanceof byte[] data) {
                            ServletOutputStream outputStream = resp.getOutputStream();
                            outputStream.write(data);
                            outputStream.flush();
                        } else {
                            throw new ServletException("Unable to process REST result when handle url: " + url);
                        }
                    } else if (!dispatcher.isVoid) {
                        PrintWriter writer = resp.getWriter();
                        JsonUtils.writeJson(writer, r);
                        writer.flush();
                    }
                } else {
                    // 调度器为MVC类型，处理服务器端请求并返回响应视图
                    if (!resp.isCommitted()) {
                        resp.setContentType("text/html");
                    }
                    // 处理String、byte[]和 ModelAndView
                    if (r instanceof String s) {
                        // 写入响应 or 页面跳转
                        if (dispatcher.isResponseBody) {
                            PrintWriter pw = resp.getWriter();
                            pw.write(s);
                            pw.flush();
                        } else if (s.startsWith("redirect:")) {
                            resp.sendRedirect(s.substring(9));
                        } else {
                            // error:
                            throw new ServletException("Unable to process String result when handle url: " + url);
                        }
                    } else if (r instanceof byte[] data) {
                        if (dispatcher.isResponseBody) {
                            ServletOutputStream outputStream = resp.getOutputStream();
                            outputStream.write(data);
                            outputStream.flush();
                        } else {
                            throw new ServletException("Unable to process byte[] result when handle url: " + url);
                        }
                    } else if (r instanceof ModelAndView mv) {
                        String viewName = mv.getViewName();
                        // 跳转到其他视图或者渲染
                        if (viewName.startsWith("redirect:")) {
                            resp.sendRedirect(viewName.substring(9));
                        } else {
                            this.viewResolver.render(viewName, mv.getModel(), req, resp);
                        }
                    } else if (!dispatcher.isVoid && r != null) {
                        throw new ServletException("Unable to process " + r.getClass().getName() + " result when handle url: " + url);
                    }
                }
                // 特别重要的返回，到这里请求已经处理完了
                return;
            }
        }
        resp.sendError(404, "Not Found");
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
            this.urlPattern = PathUtils.compile(urlPattern);
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

        /**
         * 处理http请求。
         * 根据给定的URL模式匹配请求，并根据匹配结果提取相应的参数，然后调用对应的方法处理请求，并返回处理结果
         */
        Result process(String url, HttpServletRequest req, HttpServletResponse resp) throws Exception {
            Matcher matcher = urlPattern.matcher(url);
            if (matcher.matches()) {
                Object[] arguments = new Object[methodParameters.length];
                for (int i = 0; i < arguments.length; i++) {
                    Param param = methodParameters[i];
                    arguments[i] = switch (param.paramType) {
                        case PATH_VARIABLE -> {
                            try {
                                String s = matcher.group(param.name);
                                yield convertToType(param.classType, s);
                            } catch (IllegalArgumentException e) {
                                throw new ServerWebInputException("Path variable '" + param.name + "' not found.");
                            }
                        }
                        case REQUEST_BODY -> {
                            // 从请求的正文部分读取JSON数据
                            BufferedReader reader = req.getReader();
                            yield JsonUtils.readJson(reader, param.classType);
                        }
                        case REQUEST_PARAM -> {
                            // 得到请求参数为空时返回默认值，然后转换为制定的参数类型
                            String s = getOrDefault(req, param.name, param.defaultValue);
                            yield convertToType(param.classType, s);
                        }
                        case SERVLET_VARIABLE -> {
                            // 获取相应的HTTP请求、响应、会话或ServletContext对象
                            Class<?> classType = param.classType;
                            if (classType == HttpServletRequest.class) {
                                yield req;
                            } else if (classType == HttpServletResponse.class) {
                                yield resp;
                            } else if (classType == HttpSession.class) {
                                yield req.getSession();
                            } else if (classType == ServletContext.class) {
                                yield req.getServletContext();
                            } else {
                                throw new ServerErrorException("Could not determine argument type: " + classType);
                            }
                        }
                    };
                }
                Object result = null;
                try {
                    result = this.handlerMethod.invoke(this.controller, arguments);
                } catch (InvocationTargetException e) {
                    // 可能是业务逻辑中抛出的具体异常类型&&调用栈中保留了原始异常信息
                    Throwable cause = e.getCause();
                    if (cause instanceof Exception ex) {
                        throw ex;
                    }
                    throw e;
                } catch (ReflectiveOperationException e) {
                    throw new ServerErrorException(e);
                }
                return new Result(true, result);
            }
            return NOT_PROCESSED;
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
            if (s == null) {
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
                this.defaultValue = rp.defaultValue();
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

    static record Result(boolean processed, Object returnObject) {}
}
