package cn.xu.spring.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

/**
 * created by Xu on 2024/5/28 12:05.
 * 模板引擎接口
 */
public interface ViewResolver {
    // 初始化 ViewResolver
    void init();

    // 渲染
    void render(String viewName, Map<String, Object> module, HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException;
}
