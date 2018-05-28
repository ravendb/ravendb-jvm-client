package net.ravendb.client.documents.commands.multiGet;

import org.apache.http.HttpStatus;

import java.util.Map;
import java.util.TreeMap;

public class GetResponse {

    public GetResponse() {
        headers = new TreeMap<>(String::compareToIgnoreCase);
    }

    private String result;
    private Map<String, String> headers;
    private int statusCode;
    private boolean forceRetry;

    /**
     * @return Response result as JSON.
     */
    public String getResult() {
        return result;
    }

    /**
     * @param result Response result as JSON.
     */
    public void setResult(String result) {
        this.result = result;
    }

    /**
     * @return Response headers.
     */
    public Map<String, String> getHeaders() {
        return headers;
    }

    /**
     * @param headers Response headers.
     */
    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    /**
     * @return Response HTTP status code.
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * @param statusCode Response HTTP status code.
     */
    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    /**
     * @return Indicates if request should be retried (forced).
     */
    public boolean isForceRetry() {
        return forceRetry;
    }

    /**
     * @param forceRetry Indicates if request should be retried (forced).
     */
    public void setForceRetry(boolean forceRetry) {
        this.forceRetry = forceRetry;
    }

    /**
     * @return Method used to check if request has errors.
     */
    public boolean requestHasErrors() {
        switch (statusCode) {
            case 0:
            case HttpStatus.SC_OK:
            case HttpStatus.SC_CREATED:
            case HttpStatus.SC_NON_AUTHORITATIVE_INFORMATION:
            case HttpStatus.SC_NO_CONTENT:
            case HttpStatus.SC_NOT_MODIFIED:
            case HttpStatus.SC_NOT_FOUND:
                return false;
            default:
                return true;
        }
    }
}
