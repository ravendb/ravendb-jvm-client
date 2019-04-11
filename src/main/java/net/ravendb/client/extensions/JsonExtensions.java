package net.ravendb.client.extensions;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.introspect.AnnotatedField;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.fasterxml.jackson.databind.introspect.AnnotatedParameter;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import net.ravendb.client.Constants;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.queries.IndexQuery;
import net.ravendb.client.documents.session.EntityToJson;
import net.ravendb.client.primitives.NetDateFormat;
import net.ravendb.client.primitives.SharpAwareJacksonAnnotationIntrospector;
import net.ravendb.client.primitives.SharpEnum;
import net.ravendb.client.util.TimeUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.time.Duration;
import java.util.EnumSet;


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


        private Duration parseMiddlePart(String input) {
            String[] tokens = input.split(":");
            int hours = Integer.valueOf(tokens[0]);
            int minutes = Integer.valueOf(tokens[1]);
            int seconds = Integer.valueOf(tokens[2]);

            return Duration.ofHours(hours).plusMinutes(minutes).plusSeconds(seconds);
        }

        @Override
        public Duration deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String text = p.getText();

            boolean hasDays = text.matches("^\\d+\\..*");
            boolean hasMillis = text.matches(".*\\.\\d+");

            if (hasDays && hasMillis) {
                String[] tokens = text.split("\\.");

                int days = Integer.parseInt(tokens[0]);
                int millis = Integer.parseInt(tokens[2]);
                return parseMiddlePart(tokens[1]).plusDays(days).plusMillis(millis);
            } else if (hasDays) {
                String[] tokens = text.split("\\.");
                int days = Integer.parseInt(tokens[0]);
                return parseMiddlePart(tokens[1]).plusDays(days);
            } else if (hasMillis) {
                String[] tokens = text.split("\\.");
                String fractionString = tokens[1];
                fractionString = StringUtils.rightPad(fractionString, 7, '0');
                long value = Long.parseLong(fractionString);

                value *= 100;

                return parseMiddlePart(tokens[0]).plusNanos(value);
            } else {
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

    public static void writeIndexQuery(JsonGenerator generator, DocumentConventions conventions, IndexQuery query) throws IOException {
        generator.writeStartObject();

        generator.writeStringField("Query", query.getQuery());

        if (query.isPageSizeSet() && query.getPageSize() >= 0) {
            generator.writeNumberField("PageSize", query.getPageSize());
        }

        if (query.isWaitForNonStaleResults()) {
            generator.writeBooleanField("WaitForNonStaleResults", query.isWaitForNonStaleResults());
        }

        if (query.getStart() > 0) {
            generator.writeNumberField("Start", query.getStart());
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
            generator.writeObject(EntityToJson.convertEntityToJson(query.getQueryParameters(), conventions));
        } else {
            generator.writeNull();
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
