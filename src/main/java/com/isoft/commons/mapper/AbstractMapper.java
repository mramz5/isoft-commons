package com.isoft.commons.mapper;

import com.isoft.commons.entity.Entity;
import com.isoft.commons.model.Model;
import com.isoft.commons.utils.ReflectionUtil;
import lombok.Getter;
import lombok.NonNull;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static com.isoft.commons.utils.ReflectionUtil.*;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Getter
public abstract class AbstractMapper<T extends Entity, S extends Model> {

    private final Class<T> entityClass;
    private final Class<S> modelClass;

    protected AbstractMapper(Class<T> entityClass, Class<S> modelClass) {
        this.entityClass = entityClass;
        this.modelClass = modelClass;
    }

    private static void mapFields(Object model, Class<?> modelClass, Object entity, Class<?> entityClass, Map<Object, Object> cache)
            throws IllegalAccessException,
            ClassNotFoundException,
            InvocationTargetException,
            NoSuchMethodException,
            InstantiationException, IOException {

        do {
            Field[] entityFields = entityClass.getDeclaredFields();
            Field[] modelsFields = modelClass.getDeclaredFields();

            cache.put(model, entity);

            for (int i = 0; i < Math.min(entityFields.length, modelsFields.length); ++i) {
                Field modelField = modelsFields[i];
                Field entityField = entityFields[i];
                Object modelFieldValue = getFieldValue(modelField, model);

                Mapping mappingAnnotation = ReflectionUtil.getMappingAnnotation(modelField);
                if (((nonNull(mappingAnnotation) && !mappingAnnotation.ignoreField()) || nonNull(modelFieldValue))) {

                    if (modelFieldValue.getClass().getTypeParameters().length > 1)
                        throw new IllegalArgumentException("only collection of type generics are allowed");

                    Class<? extends Model> modelGenericClass;
                    if (modelFieldValue instanceof Model)
                        mapSingle(modelFieldValue, entity, entityField, cache);
                    else if (modelFieldValue instanceof Collection<?> && Model.class.isAssignableFrom(modelGenericClass = (Class<? extends Model>) Class.forName((getCollectionParametrizedType(modelField)))))
                        mapCollection(modelFieldValue, modelGenericClass, model, modelField, entity, entityField, cache);
                    else if (ReflectionUtil.isPrimitive(modelFieldValue.getClass()))
                        ReflectionUtil.setField(entityField, entity, ReflectionUtil.deepCopy(modelFieldValue));
                    else
                        ReflectionUtil.setField(entityField, entity, modelFieldValue);
                }
            }
            modelClass = modelClass.getSuperclass();
            entityClass = entityClass.getSuperclass();
        } while (modelClass != Object.class);
    }

    private static void mapSingle(Object fieldValue,
                                  Object entity,
                                  Field entityField,
                                  Map<Object, Object> cache)
            throws ClassNotFoundException,
            InvocationTargetException,
            NoSuchMethodException,
            InstantiationException,
            IllegalAccessException, IOException {

        //avoiding loop
        Object entityValue = cache.get(fieldValue);
        if (nonNull(entityValue)) {
            ReflectionUtil.setField(entityField, entity, entityValue);
            return;
        }

        Class<? extends Entity> entityClassFromModelClass = getEntityClassFromModelClass(fieldValue.getClass());
        Entity newInstance = getNewInstance(entityClassFromModelClass);
        mapFields(fieldValue, fieldValue.getClass(), newInstance, entityClassFromModelClass, cache);
        ReflectionUtil.setField(entityField, entity, newInstance);
    }

    private static void mapCollection(Object collection,
                                      Class<? extends Model> modelGenericClass,
                                      Object model,
                                      Field collectionType,
                                      Object entity,
                                      Field entityField,
                                      Map<Object, Object> cache)
            throws IllegalAccessException,
            ClassNotFoundException,
            InvocationTargetException,
            NoSuchMethodException,
            InstantiationException, IOException {

        //collection impl
        Class<?> collectionImplClass = ReflectionUtil.getFieldValue(collectionType, model).getClass();
        Collection<Entity> collectionImp = (Collection<Entity>) collectionImplClass.getDeclaredConstructor().newInstance();
        Class<Entity> entityClassFromModelClass = (Class<Entity>) getEntityClassFromModelClass(modelGenericClass);

        for (Object value : (Collection<?>) collection) {
            Object entityValue = cache.get(value);
            Entity newInstance = null;
            if (isNull(entityValue)) {
                newInstance = ReflectionUtil.getNewInstance(entityClassFromModelClass);
                //hence we have a collection, and we want to add elements to it, we should not set the entity field
                mapSingle(value, newInstance, entityField, cache);
            }
            collectionImp.add(isNull(entityValue) ? newInstance : (Entity) entityValue);
        }
        ReflectionUtil.setField(entityField, entity, collectionImp);
    }

    public void mergeEntityWithModel(@NonNull S model, @NonNull T entity) {
//        this.mapFields(entity);
    }

    protected T modelToEntity(@NonNull S model) {
        T entity;
        try {
            entity = entityClass.getDeclaredConstructor().newInstance();
            mapFields(model, modelClass, entity, entityClass, new HashMap<>());
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException | InstantiationException |
                 ClassNotFoundException | IOException var5) {
            throw new RuntimeException(var5);
        }
        return entity;
    }

    protected S entityToModel(@NonNull T entity) {
        S model;
        try {
            model = modelClass.getDeclaredConstructor().newInstance();
            mapFields(entity, entityClass, model, model.getClass(), new HashMap<>());
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException | InstantiationException |
                 IOException | ClassNotFoundException var5) {
            throw new RuntimeException(var5);
        }
        return model;
    }
}
