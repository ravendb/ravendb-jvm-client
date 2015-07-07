package net.ravendb.client;

import net.ravendb.abstractions.closure.Action1;
import net.ravendb.abstractions.connection.OperationCredentials;
import net.ravendb.client.delegates.HttpResponseHandler;
import net.ravendb.client.delegates.HttpResponseWithMetaHandler;
import net.ravendb.client.delegates.RequestCachePolicy;
import net.ravendb.client.document.FailoverBehavior;
import net.ravendb.client.document.FailoverBehaviorSet;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;

public abstract class ConventionBase {

    private RequestCachePolicy shouldCacheRequest;
    private FailoverBehaviorSet failoverBehavior = new FailoverBehaviorSet();
    private double requestTimeThresholdInMiliseconds;
    private HttpResponseHandler handleForbiddenResponse;
    private HttpResponseWithMetaHandler handleUnauthorizedResponse;


    /**
     * Whatever or not RavenDB should cache the request to the specified url.
     * @param url
     */
    public Boolean shouldCacheRequest(String url) {
        return shouldCacheRequest.shouldCacheRequest(url);
    }

    /**
     * @param shouldCacheRequest the shouldCacheRequest to set
     */
    public void setShouldCacheRequest(RequestCachePolicy shouldCacheRequest) {
        this.shouldCacheRequest = shouldCacheRequest;
    }

    /**
     * Whatever or not RavenDB should cache the request to the specified url.
     * @return the shouldCacheRequest
     */
    public RequestCachePolicy getShouldCacheRequest() {
        return shouldCacheRequest;
    }

    /**
     * How should we behave in a replicated environment when we can't
     *  reach the primary node and need to failover to secondary node(s).
     * @return the failoverBehavior
     */
    public FailoverBehaviorSet getFailoverBehavior() {
        return failoverBehavior;
    }

    /**
     * How should we behave in a replicated environment when we can't
     *  reach the primary node and need to failover to secondary node(s).
     * @param failoverBehavior the failoverBehavior to set
     */
    public void setFailoverBehavior(FailoverBehaviorSet failoverBehavior) {
        this.failoverBehavior = failoverBehavior;
    }

    public FailoverBehaviorSet getFailoverBehaviorWithoutFlags() {
        FailoverBehaviorSet result = this.failoverBehavior.clone();
        result.remove(FailoverBehavior.READ_FROM_ALL_SERVERS);
        return result;
    }


    public double getRequestTimeThresholdInMiliseconds() {
        return requestTimeThresholdInMiliseconds;
    }

    public void setRequestTimeThresholdInMiliseconds(double requestTimeThresholdInMiliseconds) {
        this.requestTimeThresholdInMiliseconds = requestTimeThresholdInMiliseconds;
    }

    /**
     *  Handles unauthenticated responses, usually by authenticating against the oauth server
     * @return the handleUnauthorizedResponse
     */
    public HttpResponseWithMetaHandler getHandleUnauthorizedResponse() {
        return handleUnauthorizedResponse;
    }

    /**
     *  Handles unauthenticated responses, usually by authenticating against the oauth server
     * @param handleUnauthorizedResponse the handleUnauthorizedResponse to set
     */
    public void setHandleUnauthorizedResponse(HttpResponseWithMetaHandler handleUnauthorizedResponse) {
        this.handleUnauthorizedResponse = handleUnauthorizedResponse;
    }

    /**
     * Handles forbidden responses
     * @return the handleForbiddenResponse
     */
    public HttpResponseHandler getHandleForbiddenResponse() {
        return handleForbiddenResponse;
    }

    /**
     * Handles forbidden responses
     * @param handleForbiddenResponse the handleForbiddenResponse to set
     */
    public void setHandleForbiddenResponse(HttpResponseHandler handleForbiddenResponse) {
        this.handleForbiddenResponse = handleForbiddenResponse;
    }

    public void handleForbiddenResponse(HttpResponse forbiddenResponse) {
        handleForbiddenResponse.handle(forbiddenResponse);
    }

    public Action1<HttpRequest> handleUnauthorizedResponse(HttpResponse unauthorizedResponse, OperationCredentials credentials) {
        return handleUnauthorizedResponse.handle(unauthorizedResponse, credentials);
    }
}
