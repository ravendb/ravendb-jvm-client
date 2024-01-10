package net.ravendb.client.http.behaviors;

import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.primitives.Reference;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;

import java.io.IOException;

public abstract class AbstractCommandResponseBehavior {

    public abstract <TResult> void handleNotModified(RavenCommand<TResult> command, CloseableHttpResponse response, Reference<String> cachedValue) throws IOException;

    public abstract <TResult> boolean tryHandleNotFound(RavenCommand<TResult> command, CloseableHttpResponse response) throws IOException;

    public abstract <TResult> boolean tryHandleConflict(RavenCommand<TResult> command, CloseableHttpResponse response);

    public abstract <TResult> boolean tryHandleUnsuccessfulResponse(RavenCommand<TResult> command, CloseableHttpResponse response);
}
