package com.isoft.commons.utils.collections;

import java.util.Collection;

import static java.util.Objects.nonNull;

public class CollectionUtils {
    public static boolean isNotEmpty(Collection<?> collection) {
        return nonNull(collection) && collection.size() != 0;
    }
}
