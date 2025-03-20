package com.isoft.commons.mapper;

import com.isoft.commons.model.Model;
import com.isoft.commons.utils.ReflectionUtils;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static com.isoft.commons.mapper.MapperUtils.getCorrespondingClass;
import static com.isoft.commons.utils.ReflectionUtils.*;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Getter
@Slf4j
public abstract class AbstractMapper<T, S> {
    private final Class<T> entityClass;

    private final Class<S> modelClass;

    protected AbstractMapper(Class<T> entityClass, Class<S> modelClass) {
        this.entityClass = entityClass;
        this.modelClass = modelClass;
    }

    private static void mapFields(Object source,
                                  Class<?> sourceClass,
                                  Object target,
                                  Class<?> targetClass,
                                  Map<Object, Object> cache,
                                  boolean isEntity)
            throws IllegalAccessException,
            ClassNotFoundException,
            NoSuchMethodException,
            InstantiationException, IOException, InvocationTargetException {

        cache.put(source, target);

        do {
            Field[] targetFields = targetClass.getDeclaredFields();
            Field[] sourceFields = sourceClass.getDeclaredFields();

            for (int i = 0; i < Math.min(targetFields.length, sourceFields.length); ++i) {
                Field sourceField = sourceFields[i];
                Field targetField = targetFields[i];
                Object sourceFieldValue = getFieldValue(sourceField, source);

                handleFieldMapping(source, target, cache, sourceField, targetField, sourceFieldValue, isEntity);
            }
            sourceClass = sourceClass.getSuperclass();
            targetClass = targetClass.getSuperclass();
        } while (sourceClass != Object.class);
    }

    private static void handleFieldMapping(Object source,
                                           Object target,
                                           Map<Object, Object> cache,
                                           Field sourceField,
                                           Field targetField,
                                           Object sourceFieldValue,
                                           boolean isEntity)
            throws ClassNotFoundException,
            NoSuchMethodException,
            InstantiationException,
            IllegalAccessException,
            IOException,
            InvocationTargetException {

        Mapping mappingAnnotation = ReflectionUtils.getMappingAnnotation(sourceField);
        if (((nonNull(mappingAnnotation) && !mappingAnnotation.ignoreField()) || nonNull(sourceFieldValue))) {

            Collection<?> collection = sourceFieldValue instanceof Collection<?> ? (Collection<?>) sourceFieldValue : null;
            Class<?> sourceGenericClass;

            if (sourceFieldValue instanceof Model)
                mapSingle(sourceFieldValue, target, targetField, cache, isEntity);

            else if (nonNull(collection) && !collection.isEmpty() &&
                    Model.class.isAssignableFrom(sourceGenericClass = Class.forName(getCollectionParametrizedType(sourceField))))
                mapCollection(collection, sourceGenericClass, source, sourceField, target, targetField, cache, isEntity);

            else if (!ReflectionUtils.isPrimitive(sourceFieldValue.getClass())) {
                if (sourceFieldValue.getClass().getTypeParameters().length > 1)
                    log.warn("only collection of generics support nested map");

                ReflectionUtils.setField(targetField, target, MapperUtils.deepCopy(sourceFieldValue));
            } else
                ReflectionUtils.setField(targetField, target, sourceFieldValue);
        }
    }

    private static Object mapSingle(Object fieldValue,
                                    Object target,
                                    Field targetField,
                                    Map<Object, Object> cache,
                                    boolean isEntity)
            throws ClassNotFoundException,
            NoSuchMethodException,
            InstantiationException,
            IllegalAccessException,
            IOException,
            InvocationTargetException {

        //avoiding loop
        Object targetValue = cache.get(fieldValue);
        if (nonNull(targetValue)) {
            if (nonNull(targetField))
                ReflectionUtils.setField(targetField, target, targetValue);
            return target;
        }

        Class<?> targetClassFromSourceClass = getCorrespondingClass(fieldValue.getClass(), isEntity);
        Object newInstance = getNewInstance(targetClassFromSourceClass);
        mapFields(fieldValue, fieldValue.getClass(), newInstance, targetClassFromSourceClass, cache, isEntity);
        if (nonNull(targetField))
            ReflectionUtils.setField(targetField, target, newInstance);
        return newInstance;
    }

    private static void mapCollection(Object collection,
                                      Class<?> sourceGenericClass,
                                      Object source,
                                      Field collectionType,
                                      Object target,
                                      Field targetField,
                                      Map<Object, Object> cache,
                                      boolean isEntity)
            throws IllegalAccessException,
            ClassNotFoundException,
            NoSuchMethodException,
            InstantiationException,
            IOException,
            InvocationTargetException {

        //collection impl
        Class<?> collectionImplClass = ReflectionUtils.getFieldValue(collectionType, source).getClass();
        Collection<Object> collectionImp = (Collection<Object>) collectionImplClass.getDeclaredConstructor().newInstance();
        Class<?> targetClassFromSourceClass = getCorrespondingClass(sourceGenericClass, isEntity);

        for (Object value : (Collection<?>) collection) {
            Object targetValue = cache.get(value);
            Object mapSingle = null;
            if (isNull(targetValue)) {
                //hence we have a collection, and we want to add elements to it, we should not set the target field
                mapSingle = mapSingle(value, ReflectionUtils.getNewInstance(targetClassFromSourceClass), null, cache, isEntity);
            }
            collectionImp.add(isNull(targetValue) ? mapSingle : targetValue);
        }
        ReflectionUtils.setField(targetField, target, collectionImp);
    }

    protected T toEntity(@NonNull S model) {
        T entity;
        try {
            entity = entityClass.getDeclaredConstructor().newInstance();
            mapFields(model, modelClass, entity, entityClass, new HashMap<>(), true);
        } catch (IllegalAccessException | NoSuchMethodException | InstantiationException |
                 ClassNotFoundException | IOException | InvocationTargetException var5) {
            throw new RuntimeException(var5);
        }
        return entity;
    }

    protected S toModel(@NonNull S entity) {
        S model;
        try {
            model = modelClass.getDeclaredConstructor().newInstance();
            mapFields(entity, entityClass, model, modelClass, new HashMap<>(), false);
        } catch (IllegalAccessException | NoSuchMethodException | InstantiationException | IOException |
                 ClassNotFoundException | InvocationTargetException var5) {
            throw new RuntimeException(var5);
        }
        return model;
    }
}
