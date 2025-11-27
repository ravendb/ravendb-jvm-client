package net.ravendb.client.documents.operations;

import java.util.List;
import com.github.fge.jackson.jsonpointer.JsonPointer;
import com.github.fge.jsonpatch.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jsonpatch.JsonPatchOperation;

import java.util.ArrayList;

public class JsonPatchDocument {

    private final List<JsonPatchOperation> operations = new ArrayList<>();

    public List<JsonPatchOperation> getOperations() {
        return operations;
    }

    public void add(JsonPointer path, JsonNode value) {
        operations.add(new AddOperation(path, value));
    }

    public void remove(JsonPointer path) {
        operations.add(new RemoveOperation(path));
    }

    public void replace(JsonPointer path, JsonNode value) {
        operations.add(new ReplaceOperation(path, value));
    }

    public void move(JsonPointer from, JsonPointer path) {
        operations.add(new MoveOperation(from, path));
    }

    public void test(JsonPointer path, JsonNode value) {
        operations.add(new TestOperation(path, value));
    }

    public JsonNode apply(JsonNode target) throws Exception {
        JsonNode result = target;
        for (JsonPatchOperation op : operations) {
            result = op.apply(result);
        }
        return result;
    }
}
