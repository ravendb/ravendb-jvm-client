package net.ravendb.client.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class ObjectUtils {
    public static Map<String, Object> transformObjectKeys(Map<String, Object> input, Function<String, String> keyTransform) {
        if (input == null) return null;

        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : input.entrySet()) {
            String transformedKey = keyTransform.apply(entry.getKey());
            Object value = entry.getValue();

            if (value instanceof Map) {
                //noinspection unchecked
                value = transformObjectKeys((Map<String, Object>) value, keyTransform);
            } else if (value instanceof List) {
                value = transformList((List<?>) value, keyTransform);
            }

            result.put(transformedKey, value);
        }
        return result;
    }

    public static String camel(String input) {
        if (input == null || input.isEmpty()) return input;
        if (!input.contains("_") && !input.contains("-") && !input.contains(" ")) {
            return input;
        }
        String[] parts = input.split("[_\\-\\s]+");
        StringBuilder sb = new StringBuilder(parts[0].toLowerCase());

        for (int i = 1; i < parts.length; i++) {
            String part = parts[i];
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0)));
                if (part.length() > 1) {
                    sb.append(part.substring(1).toLowerCase());
                }
            }
        }

        return sb.toString();
    }


    private static List<Object> transformList(List<?> list, Function<String, String> keyTransform) {
        List<Object> result = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map) {
                //noinspection unchecked
                result.add(transformObjectKeys((Map<String, Object>) item, keyTransform));
            } else if (item instanceof List) {
                result.add(transformList((List<?>) item, keyTransform));
            } else {
                result.add(item);
            }
        }
        return result;
    }
}
