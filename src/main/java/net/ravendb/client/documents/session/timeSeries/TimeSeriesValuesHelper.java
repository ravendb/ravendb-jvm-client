package net.ravendb.client.documents.session.timeSeries;

import net.ravendb.client.exceptions.RavenException;
import net.ravendb.client.primitives.Tuple;
import net.ravendb.client.util.ReflectionUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class TimeSeriesValuesHelper {

    private static final ConcurrentMap<Class<?>, SortedMap<Byte, Tuple<Field, String>>> _cache = new ConcurrentHashMap<>();

    public static SortedMap<Byte, Tuple<Field, String>> getFieldsMapping(Class<?> clazz) {
        return _cache.computeIfAbsent(clazz, c -> {
            SortedMap<Byte, Tuple<Field, String>> mapping = null;
            for (Field field : ReflectionUtil.getFieldsFor(clazz)) {
                TimeSeriesValue annotation = field.getAnnotation(TimeSeriesValue.class);
                if (annotation == null) {
                    continue;
                }

                if (!Double.class.equals(field.getType()) && !Double.TYPE.equals(field.getType())) {
                    throw new IllegalStateException("Cannot create a mapping for '" + clazz.getSimpleName() + "' class, because field '" + field.getName() + "' is not a double.");
                }

                byte i = annotation.idx();
                if (mapping == null) {
                    mapping = new TreeMap<>();
                }

                if (mapping.containsKey(i)) {
                    throw new IllegalStateException("Cannot map '" + field.getName() + " to " + i + ", since '" + mapping.get(i).first.getName() + "' already mapped to it.");
                }

                String name = StringUtils.isNotEmpty(annotation.name()) ? annotation.name() : field.getName();

                mapping.put(i, Tuple.create(field, name));
            }

            if (mapping == null) {
                return null;
            }

            if (mapping.firstKey() != 0 ||  mapping.lastKey() != mapping.size() - 1) {
                throw new IllegalStateException("The mapping of '" + clazz.getSimpleName() + "' must contain consecutive values starting from 0.");
            }

            return mapping;
        });
    }

    public static <T> double[] getValues(Class<T> clazz, T obj) {
        SortedMap<Byte, Tuple<Field, String>> mapping = getFieldsMapping(clazz);
        if (mapping == null) {
            return null;
        }

        try {
            double[] values = new double[mapping.size()];
            for (Map.Entry<Byte, Tuple<Field, String>> kvp : mapping.entrySet()) {
                byte index = kvp.getKey();
                values[index] = (double) FieldUtils.readField(kvp.getValue().first, obj, true);
            }

            return values;
        } catch (IllegalAccessException e) {
            throw new RavenException("Unable to read time series values.", e);
        }
    }

    public static <T> T setFields(Class<T> clazz, double[] values) {
        return setFields(clazz, values, false);
    }

    @SuppressWarnings("deprecation")
    public static <T> T setFields(Class<T> clazz, double[] values, boolean asRollup) {
        if (values == null) {
            return null;
        }

        SortedMap<Byte, Tuple<Field, String>> mapping = getFieldsMapping(clazz);
        if (mapping == null) {
            return null;
        }

        try {
            T obj = clazz.newInstance();
            for (Map.Entry<Byte, Tuple<Field, String>> kvp : mapping.entrySet()) {
                byte index = kvp.getKey();
                double value = Double.NaN;
                if (index < values.length) {
                    if (asRollup) {
                        index *= 6;
                    }

                    value = values[index];
                }

                Field field = kvp.getValue().first;
                FieldUtils.writeField(field, obj, value, true);
            }

            return obj;
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RavenException("Unable to read time series values.", e);
        }

    }
}
