package net.ravendb.client.extensions;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.AnnotatedField;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.fasterxml.jackson.databind.introspect.AnnotatedParameter;
import net.ravendb.client.primitives.NetDateFormat;
import net.ravendb.client.primitives.SharpAwareJacksonAnnotationIntrospector;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;


public class JsonExtensions {

    //TODO: it is temporary!

    private static ObjectMapper _defaultMapper;

    public static ObjectMapper getMapper() {
        if (_defaultMapper == null) {
            _defaultMapper = createDefaultJsonSerializer();
        }

        return _defaultMapper;
    }

    public static ObjectMapper createDefaultJsonSerializer() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setPropertyNamingStrategy(new DotNetNamingStrategy());
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        objectMapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
        objectMapper.setConfig(objectMapper.getSerializationConfig().with(new NetDateFormat()));
        objectMapper.setConfig(objectMapper.getDeserializationConfig().with(new NetDateFormat()));
        objectMapper.setAnnotationIntrospector(new SharpAwareJacksonAnnotationIntrospector());
        return objectMapper;
    }

    public static class DotNetNamingStrategy extends PropertyNamingStrategy {

        @Override
        public String nameForField(MapperConfig<?> config, AnnotatedField field, String defaultName) {
            return StringUtils.capitalize(defaultName);
        }

        @Override
        public String nameForGetterMethod(MapperConfig<?> config, AnnotatedMethod method, String defaultName) {
            return StringUtils.capitalize(defaultName);
        }

        @Override
        public String nameForSetterMethod(MapperConfig<?> config, AnnotatedMethod method, String defaultName) {
            return StringUtils.capitalize(defaultName);
        }

        @Override
        public String nameForConstructorParameter(MapperConfig<?> config, AnnotatedParameter ctorParam, String defaultName) {
            return StringUtils.capitalize(defaultName);
        }
    }

}
