package net.ravendb.client.exceptions;

public class UnsuccessfulAiRequestException extends AiException {

    private int statusCode;

    public UnsuccessfulAiRequestException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }

    void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public static void throwException(String message, int statusCode, String requestId) {
        UnsuccessfulAiRequestException ex =
                new UnsuccessfulAiRequestException(
                        "Status Code: " + statusCode + ", Message: " + message,
                        statusCode);

        ex.setRequestId(requestId);

        throw ex;
    }
}
