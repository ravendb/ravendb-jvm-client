package net.ravendb.client.documents.operations.schemaValidation;

public class ValidateSchemaProgress {

    private long errorCount;
    private long validatedCount;

    public long getErrorCount() {
        return errorCount;
    }

    public void setErrorCount(long errorCount) {
        this.errorCount = errorCount;
    }

    public long getValidatedCount() {
        return validatedCount;
    }

    public void setValidatedCount(long validatedCount) {
        this.validatedCount = validatedCount;
    }
}
