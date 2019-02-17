package net.ravendb.client.exceptions.commercial;

import net.ravendb.client.exceptions.RavenException;

public class LicenseActivationException extends RavenException {
    public LicenseActivationException() {
    }

    public LicenseActivationException(String message) {
        super(message);
    }

    public LicenseActivationException(String message, Throwable cause) {
        super(message, cause);
    }
}
