package com.isoft.commons.utils;

import com.isoft.commons.mapper.Mapping;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class ReflectionUtils {

    private static final Map<Class<?>, Class<?>> WRAPPER_TYPE_MAP;

    static {
        WRAPPER_TYPE_MAP = new HashMap<>();
        WRAPPER_TYPE_MAP.put(Integer.class, int.class);
        WRAPPER_TYPE_MAP.put(Byte.class, byte.class);
        WRAPPER_TYPE_MAP.put(Character.class, char.class);
        WRAPPER_TYPE_MAP.put(Boolean.class, boolean.class);
        WRAPPER_TYPE_MAP.put(Double.class, double.class);
        WRAPPER_TYPE_MAP.put(Float.class, float.class);
        WRAPPER_TYPE_MAP.put(Long.class, long.class);
        WRAPPER_TYPE_MAP.put(Short.class, short.class);
        WRAPPER_TYPE_MAP.put(Void.class, void.class);
    }

    public static boolean isPrimitive(Class<?> clazz) {
        return !WRAPPER_TYPE_MAP.containsKey(clazz);
    }

    public static String getCollectionParametrizedType(Field field) {
        Type genericType = field.getGenericType();
        if (genericType instanceof ParameterizedType) {
            return ((ParameterizedType) genericType).getActualTypeArguments()[0].getTypeName();
        }
        return null;
    }

    public static <T> T getNewInstance(Class<T> clazz) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        return clazz.getDeclaredConstructor().newInstance();
    }

    public static Mapping getMappingAnnotation(Field field) {
        Mapping[] declaredAnnotationsByType = field.getDeclaredAnnotationsByType(Mapping.class);
        return declaredAnnotationsByType.length == 0 ? null : declaredAnnotationsByType[0];
    }

    public static void setField(Field field, Object o, Object fieldValue) throws IllegalAccessException {
        field.setAccessible(true);
        field.set(o, fieldValue);
    }

    public static Object getFieldValue(Field field, Object o) throws IllegalAccessException {
        field.setAccessible(true);
        return field.get(o);
    }
}