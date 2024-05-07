package cn.xu.spring.io;

import java.io.IOException;
import java.io.InputStream;

/**
 * created by Xu on 2024/5/5 15:09.
 * 为什么定义这个只在工具类使用的接口？
 * 1、提高扩展性，允许调用者自定义实现
 * 2、解耦，在工具类中只关心接口，不关心实现
 */
@FunctionalInterface
public interface InputStreamCallback<T> {
    T doWithInputStream(InputStream stream) throws IOException;
}
