package net.ravendb.client.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.ravendb.client.extensions.HttpExtensions;
import net.ravendb.client.primitives.Reference;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.impl.client.CloseableHttpClient;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

public abstract class RavenCommand<TResult> {

    protected Class<TResult> resultClass;
    protected TResult result;
    protected HttpStatus statusCode;
    protected RavenCommandResponseType responseType;
    protected boolean canCache;
    protected boolean canCacheAggressively;

    public abstract boolean isReadRequest();

    public RavenCommandResponseType getResponseType() {
        return responseType;
    }

    public HttpStatus getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(HttpStatus statusCode) {
        this.statusCode = statusCode;
    }

    public TResult getResult() {
        return result;
    }

    public void setResult(TResult result) {
        this.result = result;
    }

    public boolean canCache() {
        return canCache;
    }

    public boolean canCacheAggressively() {
        return canCacheAggressively;
    }

    protected RavenCommand(Class<TResult> resultClass) {
        this.resultClass = resultClass;
        responseType = RavenCommandResponseType.OBJECT;
        this.canCache = true;
        this.canCacheAggressively = true;
    }

    public abstract HttpRequestBase createRequest(ObjectMapper ctx, ServerNode node, Reference<String> url);

    public void setResponse(ObjectMapper mapper, InputStream response, boolean fromCache) throws IOException {
        if (responseType == RavenCommandResponseType.EMPTY || responseType == RavenCommandResponseType.RAW) {
            throwInvalidResponse();
        }

        throw new UnsupportedOperationException(responseType.name() + " command must override the setResponse method which expects response with the following type: " + responseType);
    }

    public CloseableHttpResponse send(CloseableHttpClient client, HttpRequestBase request) throws IOException {
        return client.execute(request);
    }

    public void setResponseRaw(CloseableHttpResponse response, InputStream stream, ObjectMapper context) {
        throw new UnsupportedOperationException("When " + responseType + " is set to Raw then please override this method to handle the response. ");
    }

    private Map<ServerNode, Exception> failedNodes;

    public Map<ServerNode, Exception> getFailedNodes() {
        return failedNodes;
    }

    public void setFailedNodes(Map<ServerNode, Exception> failedNodes) {
        this.failedNodes = failedNodes;
    }

    //TODO: public TimeSpan? Timeout { get; protected set; }

    protected String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public static void encureIsNotNullOrString(String value, String name) {
        if (StringUtils.isEmpty(value)) {
            throw new IllegalArgumentException(name + " cannot be null or empty");
        }
    }

    public boolean isFailedWithNode(ServerNode node) {
        return failedNodes != null && failedNodes.containsKey(node);
    }

    public ResponseDisposeHandling processResponse(ObjectMapper context, CloseableHttpResponse response, String url) { //TODO: http cache
        //TODO: fake impl!
        try {
            HttpEntity entity = response.getEntity();
            setResponse(context, entity.getContent(), false);
            return ResponseDisposeHandling.AUTOMATIC;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            HttpClientUtils.closeQuietly(response);
        }
    }
    /*
        public virtual async Task<ResponseDisposeHandling> ProcessResponse(JsonOperationContext context, HttpCache cache, HttpResponseMessage response, string url)
        {
            if (ResponseType == RavenCommandResponseType.Empty || response.StatusCode == HttpStatusCode.NoContent)
                return ResponseDisposeHandling.Automatic;

            using (var stream = await response.Content.ReadAsStreamAsync().ConfigureAwait(false))
            {
                if (ResponseType == RavenCommandResponseType.Object)
                {
                    var contentLength = response.Content.Headers.ContentLength;
                    if (contentLength.HasValue && contentLength == 0)
                        return ResponseDisposeHandling.Automatic;

                    // we intentionally don't dispose the reader here, we'll be using it
                    // in the command, any associated memory will be released on context reset
                    var json = await context.ReadForMemoryAsync(stream, "response/object").ConfigureAwait(false);
                    if (cache != null) //precaution
                    {
                        CacheResponse(cache, url, response, json);
                    }
                    SetResponse(json, fromCache: false);
                    return ResponseDisposeHandling.Automatic;
                }

                // We do not cache the stream response.
                using (var uncompressedStream = await RequestExecutor.ReadAsStreamUncompressedAsync(response).ConfigureAwait(false))
                    SetResponseRaw(response, uncompressedStream, context);
            }
            return ResponseDisposeHandling.Automatic;
        }

        */

    //TODO: here we have a problem with double reads!
    protected void cacheResponse(HttpCache cache, String url, CloseableHttpResponse response, InputStream responseJson) {
        if (!canCache()) {
            return;
        }

        String changeVector = HttpExtensions.getEtagHeader(response);
        if (changeVector == null) {
            return;
        }

        cache.set(url, changeVector, responseJson);
    }

    protected static void throwInvalidResponse() {
        throw new IllegalStateException("Resposen is invalid");
    }

    protected void addChangeVectorIfNotNull(String changeVector, HttpRequestBase request) {
        if (changeVector != null) {
            request.addHeader("If-Match", "\"" + changeVector + "\"");
        }
    }

    public void onResponseFailure(CloseableHttpResponse response) {

    }
}
