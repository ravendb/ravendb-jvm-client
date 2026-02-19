package net.ravendb.client.documents.operations.schemaValidation;

import java.util.Map;
import net.ravendb.client.documents.operations.IOperationResult;

public class ValidateSchemaResult extends ValidateSchemaProgress implements IOperationResult {
    //TODO: check interface inheritance
    private Map<String, String> errors;
    private long lastEtag;

    public Map<String, String> getErrors() {
        return errors;
    }

    public void setErrors(Map<String, String> errors) {
        this.errors = errors;
    }

    public long getLastEtag() {
        return lastEtag;
    }

    public void setLastEtag(long lastEtag) {
        this.lastEtag = lastEtag;
    }

    @Override
    public String getMessage() {
        return null;
    }
}