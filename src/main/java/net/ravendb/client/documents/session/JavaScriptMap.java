package net.ravendb.client.documents.session;

import com.fasterxml.jackson.core.JsonProcessingException;
import net.ravendb.client.extensions.JsonExtensions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JavaScriptMap<TKey, TValue> {

    private final int suffix;
    private int argCounter = 0;

    private final String pathToMap;

    private final List<String> scriptLines = new ArrayList<>();
    private final Map<String, Object> parameters = new HashMap<>();
    private final List<Operation> operations = new ArrayList<>();

    public JavaScriptMap(int suffix, String pathToMap) {
        this.suffix = suffix;
        this.pathToMap = pathToMap;
    }

    public JavaScriptMap<TKey, TValue> put(TKey key, TValue value) {
        String argumentName = getNextArgumentName();

        scriptLines.add("this." + pathToMap + "[" + formatKeyForJavaScript(key) + "] = args." + argumentName + ";");
        parameters.put(argumentName, value);
        operations.add(new Operation(key, value, false));
        return this;
    }

    public JavaScriptMap<TKey, TValue> remove(TKey key) {
        scriptLines.add("delete this." + pathToMap + "[" + formatKeyForJavaScript(key) + "];");
        operations.add(new Operation(key, null, true));
        return this;
    }

    private String getNextArgumentName() {
        return "val_" + argCounter++ + "_" + suffix;
    }

    private static String formatKeyForJavaScript(Object key) {
        if (key == null) {
            throw new IllegalArgumentException("Dictionary key cannot be null");
        }

        try {
            return JsonExtensions.getDefaultMapper().writeValueAsString(key.toString());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to format dictionary key for JavaScript", e);
        }
    }

    String getScript() {
        return String.join("\r", scriptLines);
    }

    Map<String, Object> getParameters() {
        return parameters;
    }

    List<Operation> getOperations() {
        return operations;
    }

    static final class Operation {
        private final Object key;
        private final Object value;
        private final boolean remove;

        private Operation(Object key, Object value, boolean remove) {
            this.key = key;
            this.value = value;
            this.remove = remove;
        }

        Object getKey() {
            return key;
        }

        Object getValue() {
            return value;
        }

        boolean isRemove() {
            return remove;
        }
    }
}
