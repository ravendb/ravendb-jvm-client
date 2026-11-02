package net.ravendb.client.documents.commands.batches;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * An RFC 6902 JSON Patch document: an ordered list of operations applied to a single document.
 * Paths are JSON pointers (e.g. "/address/city", "/lines/0", "/lines/-"); '~' and '/' inside a member name
 * are escaped as "~0" and "~1".
 */
public class JsonPatchDocument {

    private final List<Operation> operations = new ArrayList<>();

    public List<Operation> getOperations() {
        return operations;
    }

    /**
     * Adds a value at the given JSON pointer. For an object member it creates or replaces the member,
     * for an array index it inserts before that index ("-" appends to the end).
     * @param path JSON pointer
     * @param value value to add
     * @return this document
     */
    public JsonPatchDocument add(String path, Object value) {
        operations.add(new Operation(Operation.ADD, path, null, value));
        return this;
    }

    /**
     * Removes the value at the given JSON pointer.
     * @param path JSON pointer
     * @return this document
     */
    public JsonPatchDocument remove(String path) {
        operations.add(new Operation(Operation.REMOVE, path, null, null));
        return this;
    }

    /**
     * Replaces the existing value at the given JSON pointer.
     * @param path JSON pointer
     * @param value new value
     * @return this document
     */
    public JsonPatchDocument replace(String path, Object value) {
        operations.add(new Operation(Operation.REPLACE, path, null, value));
        return this;
    }

    /**
     * Removes the value at one JSON pointer and adds it at another.
     * @param from JSON pointer of the value to move
     * @param path target JSON pointer
     * @return this document
     */
    public JsonPatchDocument move(String from, String path) {
        operations.add(new Operation(Operation.MOVE, path, from, null));
        return this;
    }

    /**
     * Copies the value at one JSON pointer to another.
     * @param from JSON pointer of the value to copy
     * @param path target JSON pointer
     * @return this document
     */
    public JsonPatchDocument copy(String from, String path) {
        operations.add(new Operation(Operation.COPY, path, from, null));
        return this;
    }

    /**
     * Tests that the value at the given JSON pointer equals the given value.
     * When the test fails, none of the document's operations are applied.
     * @param path JSON pointer
     * @param value expected value
     * @return this document
     */
    public JsonPatchDocument test(String path, Object value) {
        operations.add(new Operation(Operation.TEST, path, null, value));
        return this;
    }

    /**
     * Writes the document as {"Operations": [...]}, serializing values with the given entity mapper.
     * @param generator JSON generator
     * @param entityMapper mapper used for operation values
     * @throws IOException if writing fails
     */
    public void serialize(JsonGenerator generator, ObjectMapper entityMapper) throws IOException {
        generator.writeStartObject();
        generator.writeFieldName("Operations");
        generator.writeStartArray();
        for (Operation operation : operations) {
            generator.writeStartObject();
            generator.writeStringField("op", operation.getOp());
            generator.writeStringField("path", operation.getPath());
            if (operation.getFrom() != null) {
                generator.writeStringField("from", operation.getFrom());
            }
            if (operation.hasValue()) {
                // the server requires "value" for add/replace/test, even when it is null
                generator.writeFieldName("value");
                entityMapper.writeValue(generator, operation.getValue());
            }
            generator.writeEndObject();
        }
        generator.writeEndArray();
        generator.writeEndObject();
    }

    public static class Operation {
        static final String ADD = "add";
        static final String REMOVE = "remove";
        static final String REPLACE = "replace";
        static final String MOVE = "move";
        static final String COPY = "copy";
        static final String TEST = "test";

        private final String op;
        private final String path;
        private final String from;
        private final Object value;

        public Operation(String op, String path, String from, Object value) {
            this.op = op;
            this.path = path;
            this.from = from;
            this.value = value;
        }

        public String getOp() {
            return op;
        }

        public String getPath() {
            return path;
        }

        public String getFrom() {
            return from;
        }

        public Object getValue() {
            return value;
        }

        boolean hasValue() {
            return ADD.equals(op) || REPLACE.equals(op) || TEST.equals(op);
        }
    }
}
