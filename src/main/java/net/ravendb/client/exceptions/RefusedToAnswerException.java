package net.ravendb.client.exceptions;

public final class RefusedToAnswerException extends AiException {

    public String refusal;
    public String finishReason;

    public RefusedToAnswerException(String message) {
        super(message);
    }

    public RefusedToAnswerException(String message, Exception e) {
        super(message, e);
    }

    public static void throwException(String refusal, String responseContent, String finishReason, String requestId) {
        RefusedToAnswerException ex =
                new RefusedToAnswerException(
                        "The request was refused by the model: '" + refusal +
                                "', response content: " + responseContent);

        ex.refusal = refusal;
        ex.finishReason = finishReason;
        ex.setRequestId(requestId);

        throw ex;
    }
}
