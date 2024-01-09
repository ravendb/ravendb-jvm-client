package net.ravendb.client.primitives;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;

/**
 * Performs custom enum serialization for enums annotated with {@link UseSharpEnum}
 * <p>
 * In Java there is convention for enums values: THIS_IS_ENUM, in .NET we have: ThisIsEnum.
 */
public class SharpAwareJacksonAnnotationIntrospector extends JacksonAnnotationIntrospector {

    @Override
    public String[] findEnumValues(MapperConfig<?> config, AnnotatedClass annotatedClass, Enum<?>[] enumValues, String[] names) {
        if (annotatedClass.getAnnotation(UseSharpEnum.class) != null) {
            return SharpEnum.values(enumValues);
        }
        return super.findEnumValues(config, annotatedClass, enumValues, names);
    }

    @Override
    public Version version() {
        return Version.unknownVersion();
    }
}
