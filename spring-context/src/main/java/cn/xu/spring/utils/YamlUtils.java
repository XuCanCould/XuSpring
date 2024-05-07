package cn.xu.spring.utils;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.representer.Representer;
import org.yaml.snakeyaml.resolver.Resolver;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * created by Xu on 2024/5/5 15:08.
 * 解析 yaml 文件
 */
public class YamlUtils {
    /**
     * 获取原始的 yml 数据
     * @param path
     * @return
     */
    public static Map<String, Object> readYaml(String path) {
        // 加载 yml
        var loaderOptions = new LoaderOptions();
        // 配置 yml
        var dumperOptions = new DumperOptions();
        // 将 java 对象表示为 yml
        var representer = new Representer(dumperOptions);
        // 禁止隐式解析
        var resolver = new NoImplicitResolver();
        var yaml = new Yaml(new Constructor(loaderOptions), representer, dumperOptions, loaderOptions, resolver);
        // 在文件流的基础上读取 yml
        return ClassPathUtils.readInputStream(path, input -> yaml.load(input));
    }

    /**
     * 将 yml 转换为 1-1 映射
     * @param path
     * @return
     */
    public static Map<String, Object> loadYamlAsPlainMap(String path) {
        Map<String, Object> data = readYaml(path);
        Map<String, Object> plain = new LinkedHashMap<>();
        convertTo(data, "", plain);
        return plain;
    }

    static void convertTo(Map<String, Object> source, String prefix, Map<String, Object> plain) {
        for (String key : source.keySet()) {
            Object value = source.get(key);
            if (value instanceof Map) {
                Map<String, Object> subMap = (Map<String, Object>) value;
                convertTo(subMap, prefix + key + ".", plain);
            } else if (value instanceof List) {
                plain.put(prefix + key, value);
            } else {
                plain.put(prefix + key, value);
            }
        }
    }
}

/**
 * 禁止所有隐式转换并把所有值作为 String
 */
class NoImplicitResolver extends Resolver {
    public NoImplicitResolver() {
        super();
        super.yamlImplicitResolvers.clear();
    }
}