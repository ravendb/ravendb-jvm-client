package net.ravendb.client.documents.commands.multiGet;

import com.fasterxml.jackson.core.JsonGenerator;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

public class GetRequest {
    private String url;
    private Map<String, String> headers;
    private String query;
    private String method;

    /**
     * @return Concatenated Url and Query.
     */
    public String getUrlAndQuery() {
        if (query == null) {
            return url;
        }

        if (query.startsWith("?")) {
            return url + query;
        }

        return url + "?" + query;
    }

    private IContent content;

    public GetRequest() {
        headers = new TreeMap<>(String::compareToIgnoreCase);
    }

    /**
     * @return Request url (relative).
     */
    public String getUrl() {
        return url;
    }

    /**
     * @param url Request url (relative).
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * @return Request headers.
     */
    public Map<String, String> getHeaders() {
        return headers;
    }

    /**
     * @param headers Request headers.
     */
    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    /**
     * @return Query information e.g. "?pageStart=10&amp;pageSize=20".
     */
    public String getQuery() {
        return query;
    }

    /**
     * @param query Query information e.g. "?pageStart=10&amp;pageSize=20".
     */
    public void setQuery(String query) {
        this.query = query;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public IContent getContent() {
        return content;
    }

    public void setContent(IContent content) {
        this.content = content;
    }

    public interface IContent {
        void writeContent(JsonGenerator generator) throws IOException;
    }
}
