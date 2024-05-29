package cn.xu.spring.web;

import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Map;

/**
 * created by Xu on 2024/5/28 12:05.
 */
public interface View {
    @Nullable
    default String getContentType() {
        return null;
    }

    void render(@Nullable Map<String, Object> module, HttpServletRequest request, HttpServletResponse response) throws Exception;
}
