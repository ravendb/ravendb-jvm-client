package net.ravendb.client.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Helper class for reflection operations
 */
public class ReflectionUtil {

    private static final Map<Class<?>, String> fullNameCache = new HashMap<>();

    /**
     * Note: we can't fetch generic types information in Java - hence we are limited to simple getName on class object
     * @param entityType Entity class
     * @return full name without version info
     */
    public static String getFullNameWithoutVersionInformation(Class<?> entityType) {
        if (fullNameCache.containsKey(entityType)) {
            return fullNameCache.get(entityType);
        }

        String fullName = entityType.getName();
        fullNameCache.put(entityType, fullName);
        return fullName;
    }
}
