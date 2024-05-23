package cn.xu.spring.context;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Objects;

/**
 * created by Xu on 2024/5/23 9:45.
 * 获取到applicationContext实例
 */
public class ApplicationContextUtils {

    private static ApplicationContext applicationContext;

    private ApplicationContextUtils() {}

    @Nonnull
    public static ApplicationContext getRequiredApplicationContext() {
        return Objects.requireNonNull(getApplicationContext(), "ApplicationContext is not initialized yet");
    }

    @Nullable
    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    static void setApplicationContext(ApplicationContext applicationContext) {
        ApplicationContextUtils.applicationContext = applicationContext;
    }
}
