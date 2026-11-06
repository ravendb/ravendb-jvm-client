package net.ravendb.client.documents.operations;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.ravendb.client.exceptions.commercial.LimitType;

public class OperationExceptionResult {

    private String type;
    private String message;
    private String error;
    private int statusCode;

    @JsonProperty("LimitType")
    private LimitType limitType; // used for waitForCompletion when we check for the operation result

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    LimitType getLimitType() {
        return limitType;
    }

    void setLimitType(LimitType limitType) {
        this.limitType = limitType;
    }
}
