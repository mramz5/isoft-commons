package com.isoft.commons.entity.util;

import com.isoft.commons.entity.Entity;

import java.io.*;

public class MapperUtils {

    public static Object deepCopy(Object model) throws IOException, ClassNotFoundException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(model);
        oos.flush();

        ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bis);
        return ois.readObject();
    }

    public static Class<? extends Entity> getEntityClassFromModelClass(Class<?> modelClass) throws ClassNotFoundException {
        return (Class<? extends Entity>) Class.forName(modelClass.getName()
                .replace("model", "entity")
                .replace("Model", "Entity"));
    }
}
