package net.ravendb.client.primitives;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.CaseFormat;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility class for inter-language enum conversion
 */
public class SharpEnum {

    private static final Map<Enum<?>, String> EXPLICIT_NAMES_CACHE = new ConcurrentHashMap<>();

    public static String value(Enum<?> enumValue) {
        // an explicit @JsonProperty on the constant wins - used for .NET names that can't be derived (e.g. PostgreSQLIntegration)
        String explicitName = EXPLICIT_NAMES_CACHE.computeIfAbsent(enumValue, SharpEnum::explicitName);
        if (!explicitName.isEmpty()) {
            return explicitName;
        }

        return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, enumValue.name());
    }

    public static String[] values(Enum<?>[] enumValues) {
        return Arrays.stream(enumValues).map(SharpEnum::value).toArray(String[]::new);
    }

    private static String explicitName(Enum<?> enumValue) {
        try {
            JsonProperty jsonProperty = enumValue.getDeclaringClass().getField(enumValue.name()).getAnnotation(JsonProperty.class);
            if (jsonProperty != null && !jsonProperty.value().isEmpty()) {
                return jsonProperty.value();
            }
        } catch (NoSuchFieldException ignored) {
        }

        return "";
    }
}
