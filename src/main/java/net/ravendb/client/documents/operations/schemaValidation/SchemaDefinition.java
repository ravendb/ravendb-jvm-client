package net.ravendb.client.documents.operations.schemaValidation;

import java.time.Instant;
import java.util.Date;

public class SchemaDefinition {

    private boolean disabled;
    private String schema;
    private final Date lastModifiedTime = new Date();

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public Date getLastModifiedTime() {
        return lastModifiedTime;
    }
}
