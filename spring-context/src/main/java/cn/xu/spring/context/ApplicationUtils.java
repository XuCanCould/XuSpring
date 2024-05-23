package cn.xu.spring.context;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Objects;

/**
 * created by Xu on 2024/5/23 9:45.
 * 获取到applicationContext实例
 */
public class ApplicationUtils {

    private static ApplicationContext applicationContext;

    private ApplicationUtils() {}

    @Nonnull
    public static ApplicationContext getRequiredApplicationContext() {
        return Objects.requireNonNull(getApplicationContext(), "ApplicationContext is not initialized yet");
    }

    @Nullable
    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    static void setApplicationContext(ApplicationContext applicationContext) {
        ApplicationUtils.applicationContext = applicationContext;
    }
}
