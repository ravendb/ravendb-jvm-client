package net.ravendb.client.documents.session;

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

    public JavaScriptMap(int suffix, String pathToMap) {
        this.suffix = suffix;
        this.pathToMap = pathToMap;
    }

    public JavaScriptMap<TKey, TValue> put(TKey key, TValue value) {
        String argumentName = getNextArgumentName();

        scriptLines.add("this." + pathToMap + "." + key + " = args." + argumentName + ";");
        parameters.put(argumentName, value);
        return this;
    }

    public JavaScriptMap<TKey, TValue> remove(TKey key) {
        scriptLines.add("delete this." + pathToMap + "." + key + ";");
        return this;
    }

    private String getNextArgumentName() {
        return "val_" + argCounter++ + "_" + suffix;
    }

    String getScript() {
        return String.join("\r", scriptLines);
    }

    Map<String, Object> getParameters() {
        return parameters;
    }

}
