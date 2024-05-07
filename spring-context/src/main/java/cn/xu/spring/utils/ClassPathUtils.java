package cn.xu.spring.utils;

import cn.xu.spring.io.InputStreamCallback;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;


/**
 * created by Xu on 2024/5/5 15:08.
 * 处理类路径
 */
public class ClassPathUtils {
    /**
     * 从类路径中读取资源文件，允许用户自定义得到的输入流。
     * @param path
     * @param inputStreamCallback
     * @return
     * @param <T>
     */
    public static <T> T readInputStream(String path, InputStreamCallback<T> inputStreamCallback) {
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        try (InputStream input = getContextClassLoader().getResourceAsStream(path)) {
            if (input == null) {
                throw new FileNotFoundException("File not found in classpath: " + path);
            }
            return inputStreamCallback.doWithInputStream(input);
        } catch (IOException e) {
            e.printStackTrace();
            throw new UncheckedIOException(e);
        }
    }

    /**
     * 从类路径中读取资源文件，返回字符串
     * @param path
     * @return
     */
    public static String readString(String path) {
        return readInputStream(path, input -> {
            byte[] bytes = input.readAllBytes();
            return new String(bytes, StandardCharsets.UTF_8);
        });
    }

    /**
     * 获取类加载器，默认为当前线程的类加载器
     * （提供更细粒度的类加载控制，特别是当涉及到类隔离和插件机制）
     * @return
     */
    static ClassLoader getContextClassLoader() {
        ClassLoader cl = null;
        cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = ClassPathUtils.class.getClassLoader();
        }
        return cl;
    }
}
