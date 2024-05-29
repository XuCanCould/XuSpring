package cn.xu.spring.web.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * created by Xu on 2024/5/29 19:33.
 */
public class JsonUtils {

    public static final ObjectMapper OBJECT_MAPPER = createObjectMapper();

    public static ObjectMapper createObjectMapper() {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.ALWAYS);
        // 忽略未知属性
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        // 忽略空bean的报错
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        // 禁用序列化时将日期写为时间戳
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    public static String writeJson(Object o) {
        try {
            return OBJECT_MAPPER.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }


    public static void writeJson(Writer writer, Object obj) throws IOException {
        try {
            OBJECT_MAPPER.writeValue(writer, obj);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void writeJson(OutputStream output, Object obj) throws IOException {
        try {
            OBJECT_MAPPER.writeValue(output, obj);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static <T> T readJson(String str, Class<T> clazz) {
        try {
            return OBJECT_MAPPER.readValue(str, clazz);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static <T> T readJson(Reader reader, Class<T> clazz) {
        try {
            return OBJECT_MAPPER.readValue(reader, clazz);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static <T> T readJson(Reader reader, TypeReference<T> ref) {
        try {
            return OBJECT_MAPPER.readValue(reader, ref);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static <T> T readJson(InputStream input, Class<T> clazz) {
        try {
            return OBJECT_MAPPER.readValue(input, clazz);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static <T> T readJson(InputStream input, TypeReference<T> ref) {
        try {
            return OBJECT_MAPPER.readValue(input, ref);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static <T> T readJson(String str, TypeReference<T> ref) {
        try {
            return OBJECT_MAPPER.readValue(str, ref);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static <T> T readJson(byte[] src, TypeReference<T> ref) {
        try {
            return OBJECT_MAPPER.readValue(src, ref);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static Map<String, Object> readJsonAsMap(String str) {
        try {
            return OBJECT_MAPPER.readValue(str, new TypeReference<HashMap<String, Object>>() {
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
