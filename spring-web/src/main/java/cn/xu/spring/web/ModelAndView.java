package cn.xu.spring.web;

import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletResponse;

import java.util.HashMap;
import java.util.Map;

/**
 * created by Xu on 2024/5/29 16:34.
 * MVC响应，包含Model和View名称，后续用模板引擎处理后写入响应
 */
public class ModelAndView {

    private String view;

    private Map<String, Object> model;

    int status;

    public ModelAndView(String view) {
        // 快捷调用版本：ModelAndView(String viewName, int status, @Nullable Map<String, Object> model)
        this(view, HttpServletResponse.SC_OK, null);
    }

    public ModelAndView(String viewName, @Nullable Map<String, Object> model) {
        this(viewName, HttpServletResponse.SC_OK, model);
    }

    public ModelAndView(String viewName, int status) {
        this(viewName, status, null);
    }


    public ModelAndView(String viewName, int status, @Nullable Map<String, Object> model) {
        this.view = viewName;
        this.status = status;
        if (model != null) {
            addModel(model);
        }
    }

    public void addModel(Map<String, Object> model) {
        if (this.model == null) {
            model = new HashMap<>();
        }
        model.putAll(model);
    }

    public void addModel(String name, Object value) {
        if (this.model == null) {
            model = new HashMap<>();
        }
        model.put(name, value);
    }

    public Map<String, Object> getModel() {
        if (this.model == null) {
            model = new HashMap<>();
        }
        return this.model;
    }

    public String getViewName() {
        return this.view;
    }

    public int getStatus() {
        return this.status;
    }

}
