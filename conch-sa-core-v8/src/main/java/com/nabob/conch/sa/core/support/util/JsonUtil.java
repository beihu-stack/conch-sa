package com.nabob.conch.sa.core.support.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

/**
 * V2版本 使用集团提供的 序列化配置
 */
public class JsonUtil {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
        objectMapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
//        JavaTimeModule javaTimeModule = new JavaTimeModule();
//        javaTimeModule.addSerializer(Calendar.class, new TrnCalendarSerializer(_jsonConfig.getCalendarSerializer()));
//        javaTimeModule.addDeserializer(Calendar.class, new TrnCalendarDeserializer(_jsonConfig));
//        objectMapper.registerModule(javaTimeModule);
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.setTimeZone(TimeZone.getDefault());
    }

    /**
     * 序列化
     */
    public static String object2Json(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    /**
     * 反序列化
     */
    public static <T> T json2Object(String jsonStr, Class<T> targetClz) {
        try {
            return objectMapper.readValue(jsonStr,targetClz);
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * 反序列化泛型实体
     */
    public static <T> T json2Object(String value, TypeReference<T> valueTypeRef) {
        try {
            return objectMapper.readValue(value, valueTypeRef);
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * 序列化为 list<T> 对象
     */
    public static <T> List<T> json2List(String value, Class<T> valueType) {
        try {
            JavaType javaType = objectMapper.getTypeFactory().constructCollectionType(ArrayList.class, valueType);
            return objectMapper.readValue(value, javaType);
        }
        catch (Exception ex) {
            return null;
        }
    }
}
