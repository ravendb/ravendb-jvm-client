package net.ravendb.client.exceptions.schemavalidation;

import net.ravendb.client.exceptions.RavenException;

public class SchemaValidationException extends RavenException {

    /**
     * Initializes a new instance of the {@link SchemaValidationException} class.
     *
     * @param message The message describing the validation error.
     */
    public SchemaValidationException(String message) {
        super(message);
    }
}
