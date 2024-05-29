package cn.xu.spring.web.utils;

import jakarta.servlet.ServletException;

import java.util.regex.Pattern;

/**
 * created by Xu on 2024/5/29 19:34.
 */
public class PathUtils {

    public static Pattern compile(String path) throws ServletException {
        String regPath = path.replaceAll("\\{([a-zA-Z][a-zA-Z0-9]*)\\}", "(?<$1>[^/]*)");
        if (regPath.indexOf('{') >= 0 || regPath.indexOf('}') >= 0) {
            throw new ServletException("Invalid path: " + path);
        }
        return Pattern.compile("^" + regPath + "$");
    }
}
