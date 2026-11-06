package net.ravendb.client.exceptions.commercial;

import net.ravendb.client.exceptions.RavenException;

public final class LicenseLimitException extends RavenException {

    private LimitType limitType;

    public LicenseLimitException() {
    }

    public LicenseLimitException(String message) {
        super(message);
    }

    public LicenseLimitException(LimitType limitType, String message) {
        super(message);
        this.limitType = limitType;
    }

    public LicenseLimitException(String message, Throwable cause) {
        super(message, cause);
    }

    public LimitType getLimitType() {
        return limitType;
    }

    public void setLimitType(LimitType limitType) {
        this.limitType = limitType;
    }
}
