package net.ravendb.client.exceptions;

public final class MissingAiAgentParameterException extends RavenException {

    public MissingAiAgentParameterException(String message) {
        super(message);
    }

    public MissingAiAgentParameterException(String message, Exception e) {
        super(message, e);
    }
}
