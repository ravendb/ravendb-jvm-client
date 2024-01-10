package net.ravendb.client.extensions;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.databind.type.CollectionLikeType;
import net.ravendb.client.Constants;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.queries.IndexQuery;
import net.ravendb.client.documents.queries.ProjectionBehavior;
import net.ravendb.client.documents.session.EntityToJson;
import net.ravendb.client.primitives.NetDateFormat;
import net.ravendb.client.primitives.SharpAwareJacksonAnnotationIntrospector;
import net.ravendb.client.primitives.SharpEnum;
import net.ravendb.client.util.TimeUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;


public class JsonExtensions {

    private static volatile ObjectMapper _defaultMapper;

    public static ObjectMapper getDefaultMapper() {
        if (_defaultMapper == null) {
            synchronized (JsonExtensions.class) {
                if (_defaultMapper == null) {
                    _defaultMapper = createDefaultJsonSerializer();
                }
            }
        }

        return _defaultMapper;
    }

    public static byte[] writeValueAsBytes(Object value) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JsonExtensions.getDefaultMapper().writeValue(new OutputStreamWriter(baos, StandardCharsets.UTF_8), value);
        return baos.toByteArray();
    }

    public static class SharpEnumSetSerializer extends StdSerializer<EnumSet<?>> {
        @SuppressWarnings("unchecked")
        public SharpEnumSetSerializer() {
            super((Class<EnumSet<?>>)(Class<?>)EnumSet.class);
        }

        @Override
        public void serialize(EnumSet<?> value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            StringBuilder sb = new StringBuilder();

            boolean first = true;
            for (Enum<?> anEnum : value) {
                if (!first) {
                    sb.append(",");
                }
                sb.append(SharpEnum.value(anEnum));

                first = false;
            }

            gen.writeString(sb.toString());
        }
    }

    public static class SharpEnumSetDeserializer<T extends Enum<T>> extends StdDeserializer<EnumSet<T>> implements ContextualDeserializer {
        private Class<T> enumType;
        private Map<String, T> mapping;

        public SharpEnumSetDeserializer() {
            super(EnumSet.class);
        }

        @Override
        public EnumSet<T> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            final String string = p.getValueAsString();
            final EnumSet<T> enumSet = EnumSet.noneOf(enumType);

            for (final String name : string.split(",")) {
                enumSet.add(mapping.get(name));
            }
            return enumSet;
        }

        @SuppressWarnings("unchecked")
        @Override
        public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) throws JsonMappingException {
            final CollectionLikeType type = (CollectionLikeType)property.getType();
            final SharpEnumSetDeserializer<T> enumSetDeserializer = new SharpEnumSetDeserializer<>();
            enumSetDeserializer.enumType = (Class<T>) type.getContentType().getRawClass();

            Map<String, T> mapping = new HashMap<>();

            for (T t : EnumSet.allOf(enumSetDeserializer.enumType)) {
                mapping.put(SharpEnum.value(t), t);
            }

            enumSetDeserializer.mapping = mapping;

            return enumSetDeserializer;
        }
    }

    public static class DurationSerializer extends StdSerializer<Duration> {
        public DurationSerializer() {
            super(Duration.class);
        }

        @Override
        public void serialize(Duration value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            if (value == null) {
                gen.writeNull();
            } else {
                gen.writeString(TimeUtils.durationToTimeSpan(value));
            }
        }
    }

    public static class DurationDeserializer extends StdDeserializer<Duration> {
        public DurationDeserializer() {
            super(Duration.class);
        }

        @Override
        public Duration deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String text = p.getText();

            try {
                return TimeUtils.timeSpanToDuration(text);
            } catch (IllegalArgumentException e) {
                throw new JsonParseException(p, "Unexpected Duration format:" + text);
            }
        }
    }

    public static ObjectMapper createDefaultJsonSerializer() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setPropertyNamingStrategy(new DotNetNamingStrategy());
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        objectMapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
        objectMapper.setConfig(objectMapper.getSerializationConfig().with(new NetDateFormat()));
        objectMapper.setConfig(objectMapper.getDeserializationConfig().with(new NetDateFormat()));
        objectMapper.setAnnotationIntrospector(new SharpAwareJacksonAnnotationIntrospector());

        SimpleModule durationModule = new SimpleModule();
        durationModule.addSerializer(new DurationSerializer());
        durationModule.addDeserializer(Duration.class, new DurationDeserializer());

        objectMapper.registerModule(durationModule);

        return objectMapper;
    }

    public static ObjectMapper getDefaultEntityMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        objectMapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
        objectMapper.setConfig(objectMapper.getSerializationConfig().with(new NetDateFormat()));
        objectMapper.setConfig(objectMapper.getDeserializationConfig().with(new NetDateFormat()));
        objectMapper.setAnnotationIntrospector(new SharpAwareJacksonAnnotationIntrospector());
        return objectMapper;
    }

    public static class DotNetNamingStrategy extends PropertyNamingStrategies.NamingBase {

        @Override
        public String translate(String propertyName) {
            return StringUtils.capitalize(propertyName);
        }

    }

    public static void writeIndexQuery(JsonGenerator generator, DocumentConventions conventions, IndexQuery query) throws IOException {
        generator.writeStartObject();

        generator.writeStringField("Query", query.getQuery());

        if (query.isWaitForNonStaleResults()) {
            generator.writeBooleanField("WaitForNonStaleResults", query.isWaitForNonStaleResults());
        }

        if (query.getWaitForNonStaleResultsTimeout() != null) {
            generator.writeStringField("WaitForNonStaleResultsTimeout", TimeUtils.durationToTimeSpan(query.getWaitForNonStaleResultsTimeout()));
        }

        if (query.isDisableCaching()) {
            generator.writeBooleanField("DisableCaching", query.isDisableCaching());
        }

        if (query.isSkipDuplicateChecking()) {
            generator.writeBooleanField("SkipDuplicateChecking", query.isSkipDuplicateChecking());
        }

        generator.writeFieldName("QueryParameters");
        if (query.getQueryParameters() != null) {
            generator.writeObject(EntityToJson.convertEntityToJson(query.getQueryParameters(), conventions, null));
        } else {
            generator.writeNull();
        }

        if (query.getProjectionBehavior() != null && query.getProjectionBehavior() != ProjectionBehavior.DEFAULT) {
            generator.writeStringField("ProjectionBehavior", SharpEnum.value(query.getProjectionBehavior()));
        }

        generator.writeEndObject();
    }

    public static boolean tryGetConflict(JsonNode metadata) {
        if (metadata.has(Constants.Documents.Metadata.CONFLICT)) {
            return metadata.get(Constants.Documents.Metadata.CONFLICT).asBoolean();
        }

        return false;
    }
}
