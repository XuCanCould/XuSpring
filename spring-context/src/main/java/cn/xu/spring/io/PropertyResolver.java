package cn.xu.spring.io;

import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.*;
import java.util.*;
import java.util.function.Function;


/**
 * created by Xu on 2024/5/3 22:26.
 */
public class PropertyResolver {

    Logger logger = LoggerFactory.getLogger(PropertyResolver.class);

    Map<String, String> properties = new HashMap<>();
    // 转化器
    Map<Class<?>, Function<String, Object>> converters = new HashMap<>();

    /**
     * 构造函数初始化，读取配置并注册转换器。（注意在debug模式下的打印输出）
     * @param props
     */
    public PropertyResolver(Properties props) {
        this.properties.putAll(System.getenv());
        Set<String> names = props.stringPropertyNames();
        for (String name : names) {
            this.properties.put(name, props.getProperty(name));
        }

        // debug 模式下打印
        if (logger.isDebugEnabled()) {
//            properties.forEach((k, v) -> {
//                logger.debug("{}:{}", k, v);
//            });
            ArrayList<String> keys = new ArrayList<>(this.properties.keySet());
            Collections.sort(keys);
            for (String key : keys) {
                logger.debug("PropertyResolver: {} = {}", key, this.properties.get(key));
            }
            // 使用 Collections.sort 进行排序，避免输出乱序，并且参考代码指定的模式更统一
        }

        // 注册转化器，基本裂隙类型（基本类型和常用的引用类型）
        // byte、shot、int、float、double、char、String 和 时间相关类型
        converters.put(String.class, s -> s);
        converters.put(boolean.class, s -> Boolean.parseBoolean(s));
        converters.put(Boolean.class, s -> Boolean.valueOf(s));

        converters.put(byte.class, s -> Byte.parseByte(s));
        converters.put(Byte.class, s -> Byte.valueOf(s));

        converters.put(short.class, s -> Short.parseShort(s));
        converters.put(Short.class, s -> Short.valueOf(s));

        converters.put(int.class, s -> Integer.parseInt(s));
        converters.put(Integer.class, s -> Integer.valueOf(s));

        converters.put(long.class, s -> Long.parseLong(s));
        converters.put(Long.class, s -> Long.valueOf(s));

        converters.put(float.class, s -> Float.parseFloat(s));
        converters.put(Float.class, s -> Float.valueOf(s));

        converters.put(double.class, s -> Double.parseDouble(s));
        converters.put(Double.class, s -> Double.valueOf(s));

        converters.put(LocalDate.class, s -> LocalDate.parse(s));
        converters.put(LocalTime.class, s -> LocalTime.parse(s));
        converters.put(LocalDateTime.class, s -> LocalDateTime.parse(s));
        converters.put(ZonedDateTime.class, s -> ZonedDateTime.parse(s));
        converters.put(Duration.class, s -> Duration.parse(s));
        converters.put(ZoneId.class, s -> ZoneId.of(s));
    }

    public boolean containsProperty(String key) {
        return this.properties.containsKey(key);
    }


    /**
     * 获取属性值方法，会进行 value：${key} 和 嵌套引用 的处理
     *
     * @param key
     * @return
     */
    @Nullable
    public String getProperty(String key) {
        PropertyExpr propertyExpr = parsePropertyExpr(key);
        if (propertyExpr != null) {
            if (propertyExpr.defaultValue() != null) {
                return getProperty(propertyExpr.key(), propertyExpr.defaultValue());
            } else {
                return getRequiredProperty(propertyExpr.key());
            }
        }
        String value = this.properties.get(key);
        if (value != null) {
            return parseValue(value);
        }
        return value;
    }



    /**
     * 实际获取属性，并处理默认值
     * @param key
     * @param defaultValue
     * @return
     */
    public String getProperty(String key, String defaultValue) {
        String value = getProperty(key);
        return value == null ? parseValue(defaultValue) : value;
    }

    /**
     * 获取属性值，并转化成指定类型
     */
    @Nullable
    public <T> T getProperty(String key, Class<T> targetType) {
        String value = getProperty(key);
        if (value == null) {
            return null;
        }
        return convert(value, targetType);
    }

    public <T> T getProperty(String key, Class<T> targetType, T defaultValue) {
        String value = getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        return convert(value, targetType);
    }

    String parseValue(String value) {
        PropertyExpr propertyExpr = parsePropertyExpr(value);
        if (propertyExpr == null) {
            return value;
        }
        if (propertyExpr.defaultValue() != null) {
            return properties.getOrDefault(propertyExpr.key(), propertyExpr.defaultValue());
        } else {
            return getRequiredProperty(propertyExpr.key());
        }
    }


    /**
     * 类型转换
     */
    <T> T convert(String value, Class<T> targetType) {
        Function<String, Object> converter = converters.get(targetType);
        if (converter == null) {
            throw new IllegalArgumentException("Unsupported value type: " + targetType);
        }
        return (T) converter.apply(value);
    }

    public String getRequiredProperty(String key) {
        String value = properties.get(key);
        return Objects.requireNonNull(value, "Property '" + key + "' not found.");
    }

    public <T> T getRequiredProperty(String key, Class<T> targetType) {
        T value = getProperty(key, targetType);
        return Objects.requireNonNull(value, "Property '" + key + "' not found.");
    }

    /**
     * 解析属性表达式。
     * @param key
     * @return
     */
    PropertyExpr parsePropertyExpr(String key) {
        if (key.startsWith("${") && key.endsWith("}")) {
            int n = key.indexOf(':');
            if (n == -1) {
                // 值仅为 : ${key}
                String k = notEmpty(key.substring(2, key.length() - 1));
                return new PropertyExpr(k, null);
            } else {
                // 有默认值: ${key:default}
                String k = notEmpty(key.substring(2, n));
                return new PropertyExpr(k, key.substring(n + 1, key.length() - 1));
            }
        }
        return null;
    }

    /**
     * 判断 key 是否为空，返回默认值能够在一行代码中完成 key 有效性的处理
     * String k = notEmpty(key.substring(2, key.length() - 1));
     * @param key
     * @return
     */
    String notEmpty(String key) {
        if (key.isEmpty()) {
            throw new IllegalArgumentException("Invalid key: " + key);
        }
        return key;
    }
}

record PropertyExpr(String key, String defaultValue) {
}
