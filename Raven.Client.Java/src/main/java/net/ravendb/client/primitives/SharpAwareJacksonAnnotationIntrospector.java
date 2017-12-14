package net.ravendb.client.primitives;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;

/**
 * Performs custom enum serialization for enums annotated with {@link UseSharpEnum}
 * <p>
 * In Java there is convention for enums values: THIS_IS_ENUM, in .NET we have: ThisIsEnum.
 */
public class SharpAwareJacksonAnnotationIntrospector extends JacksonAnnotationIntrospector {


    @Override
    public String[] findEnumValues(Class<?> enumType, Enum<?>[] enumValues, String[] names) {
        if (enumType.getAnnotation(UseSharpEnum.class) != null) {
            return SharpEnum.values(enumValues);
        }
        return super.findEnumValues(enumType, enumValues, names);
    }

    @Override
    public Version version() {
        return Version.unknownVersion();
    }
}
