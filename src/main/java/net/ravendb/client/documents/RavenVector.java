package net.ravendb.client.documents;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import net.ravendb.client.documents.session.IRavenVector;

import java.io.IOException;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Iterator;

/**
 * Numerical embedding stored by the server in its native vector form.
 * Serialized as <code>{"@vector": [...]}</code>.
 * @param <T> Embedding element type: Float, Double, BigDecimal, Byte, Short, Integer or Long
 */
@JsonSerialize(using = RavenVector.Serializer.class)
@JsonDeserialize(using = RavenVector.Deserializer.class)
public class RavenVector<T extends Number> implements IRavenVector<T> {

    private static final String VECTOR_PROPERTY_NAME = "@vector";

    private T[] embedding;

    public RavenVector(T[] embedding) {
        assertEmbeddingType(embedding);
        this.embedding = embedding;
    }

    public T[] getEmbedding() {
        return embedding;
    }

    public void setEmbedding(T[] embedding) {
        this.embedding = embedding;
    }

    @Override
    public Iterator<T> iterator() {
        return Arrays.asList(embedding).iterator();
    }

    private static boolean isKnownType(Class<?> type) {
        return type == Float.class || type == Double.class || type == BigDecimal.class || type == Byte.class
                || type == Short.class || type == Integer.class || type == Long.class;
    }

    private static void assertEmbeddingType(Object[] embedding) {
        if (embedding == null) {
            return;
        }

        Class<?> type = embedding.getClass().getComponentType();
        if (!isKnownType(type)) {
            throw new IllegalArgumentException("The type of embedding must be numeric. Supported types are: Float, Double, BigDecimal, Byte, Short, Integer, Long. Received: " + type.getName() + ".");
        }
    }

    public static class Serializer extends StdSerializer<RavenVector<?>> {

        @SuppressWarnings("unchecked")
        public Serializer() {
            super((Class<RavenVector<?>>) (Class<?>) RavenVector.class);
        }

        @Override
        public void serialize(RavenVector<?> value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeStartObject();
            gen.writeFieldName(VECTOR_PROPERTY_NAME);
            gen.writeStartArray();

            Object[] values = value.getEmbedding();
            if (values != null) {
                for (Object element : values) {
                    if (element == null) {
                        gen.writeNull();
                    } else if (element instanceof Float) {
                        // the server stores floats natively and returns them widened to double;
                        // write the same (lossless) representation so change tracking sees no difference
                        gen.writeNumber(((Float) element).doubleValue());
                    } else if (element instanceof Double) {
                        gen.writeNumber((Double) element);
                    } else if (element instanceof BigDecimal) {
                        gen.writeNumber((BigDecimal) element);
                    } else {
                        gen.writeNumber(((Number) element).longValue());
                    }
                }
            }

            gen.writeEndArray();
            gen.writeEndObject();
        }
    }

    public static class Deserializer extends StdDeserializer<RavenVector<?>> implements ContextualDeserializer {

        private final Class<?> embeddingType;

        public Deserializer() {
            this(null);
        }

        public Deserializer(Class<?> embeddingType) {
            super(RavenVector.class);
            this.embeddingType = embeddingType;
        }

        @Override
        public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) throws JsonMappingException {
            JavaType type = property != null ? property.getType() : ctxt.getContextualType();
            if (type != null && type.containedTypeCount() == 1) {
                return new Deserializer(type.containedType(0).getRawClass());
            }
            return this;
        }

        @Override
        @SuppressWarnings({"unchecked", "rawtypes"})
        public RavenVector<?> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            if (embeddingType == null || !isKnownType(embeddingType)) {
                throw new IllegalStateException("Type " + (embeddingType != null ? embeddingType.getName() : "<unknown>") + " is not supported.");
            }

            if (p.currentToken() != JsonToken.START_OBJECT) {
                return (RavenVector<?>) ctxt.handleUnexpectedToken(RavenVector.class, p);
            }

            Number[] values = null;

            while (p.nextToken() == JsonToken.FIELD_NAME) {
                String name = p.currentName();
                p.nextToken();

                if (VECTOR_PROPERTY_NAME.equals(name)) {
                    Class<?> arrayType = Array.newInstance(embeddingType, 0).getClass();
                    values = (Number[]) ctxt.readValue(p, arrayType);
                } else {
                    p.skipChildren();
                }
            }

            return new RavenVector(values);
        }
    }
}
