package net.ravendb.client.http.behaviors;

import net.ravendb.client.exceptions.ExceptionDispatcher;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.RavenCommandResponseType;
import net.ravendb.client.primitives.Reference;
import org.apache.http.client.methods.CloseableHttpResponse;

import java.io.IOException;

public class DefaultCommandResponseBehavior extends AbstractCommandResponseBehavior {

    public static final DefaultCommandResponseBehavior INSTANCE = new DefaultCommandResponseBehavior();

    private DefaultCommandResponseBehavior() {
    }

    @Override
    public <TResult> void handleNotModified(RavenCommand<TResult> command, CloseableHttpResponse response, Reference<String> cachedValue) throws IOException {
        if (RavenCommandResponseType.OBJECT == command.getResponseType()) {
            command.setResponse(cachedValue.value, true);
        }
    }

    @Override
    public <TResult> boolean tryHandleNotFound(RavenCommand<TResult> command, CloseableHttpResponse response) throws IOException {
        switch (command.getResponseType()) {
            case EMPTY:
                break;
            case OBJECT:
                command.setResponse(null, false);
                break;
            case RAW:
            default:
                command.setResponseRaw(response, null);
        }

        return true;
    }

    @Override
    public <TResult> boolean tryHandleConflict(RavenCommand<TResult> command, CloseableHttpResponse response) {
        ExceptionDispatcher.throwException(response);
        return false;
    }

    @Override
    public <TResult> boolean tryHandleUnsuccessfulResponse(RavenCommand<TResult> command, CloseableHttpResponse response) {
        command.onResponseFailure(response);

        ExceptionDispatcher.throwException(response);

        return false;
    }
}
