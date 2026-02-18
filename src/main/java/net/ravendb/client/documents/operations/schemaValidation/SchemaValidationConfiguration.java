package net.ravendb.client.documents.operations.schemaValidation;

import java.util.Map;
import java.util.TreeMap;

public final class SchemaValidationConfiguration {

    private Map<String, SchemaDefinition> validatorsPerCollection;
    private boolean disabled;

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public Map<String, SchemaDefinition> getValidatorsPerCollection() {
        return validatorsPerCollection;
    }

    public void setValidatorsPerCollection(Map<String, SchemaDefinition> value) {
        if (value == null) {
            this.validatorsPerCollection = null;
            return;
        }

        Map<String, SchemaDefinition> map =
                new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        map.putAll(value);
        this.validatorsPerCollection = map;
    }
}

