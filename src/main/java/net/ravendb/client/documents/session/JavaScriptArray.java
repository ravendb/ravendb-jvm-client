package net.ravendb.client.documents.session;

import java.util.*;
import java.util.stream.Collectors;

public class JavaScriptArray<U> {

    private final int suffix;
    private int argCounter = 0;

    private final String pathToArray;

    private final List<String> scriptLines = new ArrayList<>();
    private final Map<String, Object> parameters = new HashMap<>();
    private final List<Operation> operations = new ArrayList<>();

    public JavaScriptArray(int suffix, String pathToArray) {
        this.suffix = suffix;
        this.pathToArray = pathToArray;
    }

    public JavaScriptArray<U> add(U u) {
        String argumentName = getNextArgumentName();

        scriptLines.add("this." + pathToArray + ".push(args." + argumentName + ");");
        parameters.put(argumentName, u);
        operations.add(Operation.add(Collections.singletonList(u)));

        return this;
    }

    @SuppressWarnings("unchecked")
    public JavaScriptArray<U> add(U... u) {
        String args = Arrays.stream(u).map(value -> {
            String argumentName = getNextArgumentName();
            parameters.put(argumentName, value);
            return "args." + argumentName;
        }).collect(Collectors.joining(","));

        scriptLines.add("this." + pathToArray + ".push(" + args + ");");
        operations.add(Operation.add(Arrays.asList(u)));
        return this;
    }

    public JavaScriptArray<U> add(Collection<U> u) {
        String args = u.stream().map(value -> {
            String argumentName = getNextArgumentName();
            parameters.put(argumentName, value);
            return "args." + argumentName;
        }).collect(Collectors.joining(","));

        scriptLines.add("this." + pathToArray + ".push(" + args + ");");
        operations.add(Operation.add(new ArrayList<>(u)));
        return this;
    }

    public JavaScriptArray<U> removeAt(int index) {
        String argumentName = getNextArgumentName();

        scriptLines.add("this." + pathToArray + ".splice(args." + argumentName + ", 1);");
        parameters.put(argumentName, index);
        operations.add(Operation.removeAt(index));

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

    List<Operation> getOperations() {
        return operations;
    }

    static final class Operation {
        private final List<?> valuesToAdd;
        private final int indexToRemove;

        private Operation(List<?> valuesToAdd, int indexToRemove) {
            this.valuesToAdd = valuesToAdd;
            this.indexToRemove = indexToRemove;
        }

        static Operation add(List<?> values) {
            return new Operation(values, -1);
        }

        static Operation removeAt(int index) {
            return new Operation(null, index);
        }

        boolean isAdd() {
            return valuesToAdd != null;
        }

        List<?> getValuesToAdd() {
            return valuesToAdd;
        }

        int getIndexToRemove() {
            return indexToRemove;
        }
    }
}
