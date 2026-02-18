package net.ravendb.client.exceptions.schemavalidation;

import net.ravendb.client.exceptions.RavenException;

public class SchemaValidationException extends RavenException {

    public SchemaValidationException(String message) {
        super(message);
    }
}
