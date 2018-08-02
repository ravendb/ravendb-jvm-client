package net.ravendb.client.exceptions.security;

public class CertificateNameMismatchException extends AuthenticationException {
    public CertificateNameMismatchException() {
    }

    public CertificateNameMismatchException(String message) {
        super(message);
    }

    public CertificateNameMismatchException(String message, Throwable cause) {
        super(message, cause);
    }
}
