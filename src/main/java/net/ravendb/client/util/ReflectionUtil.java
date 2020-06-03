package net.ravendb.client.util;

import net.ravendb.client.exceptions.RavenException;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

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

    public static List<Field> getFieldsFor(Class<?> clazz) {
        try {
            return Arrays.stream(Introspector.getBeanInfo(clazz).getPropertyDescriptors())
                    .map(x -> getField(clazz, x.getName()))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (IntrospectionException e) {
            throw new RavenException("Unable to find fields for: " + clazz, e);
        }
    }

    private static Field getField(Class<?> clazz, String name) {
        Field field = null;
        while (clazz != null && field == null) {
            try {
                field = clazz.getDeclaredField(name);
            } catch (Exception ignored) {
            }
            clazz = clazz.getSuperclass();
        }
        return field;
    }
}
