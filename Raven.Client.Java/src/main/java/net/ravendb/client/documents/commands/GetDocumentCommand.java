package net.ravendb.client.documents.commands;

import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.primitives.Reference;
import net.ravendb.client.util.UrlUtils;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;

import java.io.IOException;

public class GetDocumentCommand extends RavenCommand<GetDocumentResult> {

    private String _id;

    private String[] _ids;
    private String[] _includes;

    private boolean _metadataOnly;

    private String _startWith;
    private String _matches;
    private int _start;
    private int _pageSize;
    private String _exclude;
    private String _startAfter;

    public GetDocumentCommand(int start, int pageSize) {
        super(GetDocumentResult.class);
        _start = start;
        _pageSize = pageSize;
    }

    public GetDocumentCommand(String id, String[] includes, boolean metadataOnly) {
        super(GetDocumentResult.class);
        _id = id;
        _includes = includes;
        _metadataOnly = metadataOnly;
    }

    public GetDocumentCommand(String[] ids, String[] includes, boolean metadataOnly) {
        super(GetDocumentResult.class);
        if (ids == null || ids.length == 0) {
            throw new IllegalArgumentException("Please supply at least one id");
        }

        _ids = ids;
        _includes = includes;
        _metadataOnly = metadataOnly;
    }

    public GetDocumentCommand(String startWith, String startAfter, String matches, String exclude, int start, int pageSize) {
        super(GetDocumentResult.class);
        if (startWith == null) {
            throw new IllegalArgumentException("startWith cannot be null");
        }
        _startWith = startWith;
        _startAfter = startAfter;
        _matches = matches;
        _exclude = exclude;
        _start = start;
        _pageSize = pageSize;
    }

    @Override
    public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
        StringBuilder pathBuilder = new StringBuilder(node.getUrl());
        pathBuilder.append("/databases/")
                .append(node.getDatabase())
                .append("/docs?");

        if (_metadataOnly) {
            pathBuilder.append("&metadata-only=true");
        }

        if (_startWith != null) {
            pathBuilder.append("startsWith=");
            pathBuilder.append(UrlUtils.escapeDataString(_startWith));
            pathBuilder.append("&start=");
            pathBuilder.append(_start);
            pathBuilder.append("&pageSize=");
            pathBuilder.append(_pageSize);

            if (_matches != null) {
                pathBuilder.append("&matches=");
                pathBuilder.append(_matches);
            }

            if (_exclude != null) {
                pathBuilder.append("&exclude=");
                pathBuilder.append(_exclude);
            }

            if (_startAfter != null) {
                pathBuilder.append("&startAfter=");
                pathBuilder.append(_startAfter);
            }
        }

        if (_includes != null) {
            for (String include : _includes) {
                pathBuilder.append("&include=");
                pathBuilder.append(_includes);
            }
        }

        HttpGet request = new HttpGet();

        if (_id != null) {
            pathBuilder.append("&id=");
            pathBuilder.append(UrlUtils.escapeDataString(_id));
        } else if (_ids != null) {
            prepareRequestWithMultipleIds(pathBuilder, request, _ids);
        }

        url.value = pathBuilder.toString();
        return request;
    }

    public static void prepareRequestWithMultipleIds(StringBuilder pathBuilder, HttpRequestBase request, String[] ids) {
        /*
        TODO
         var uniqueIds = new HashSet<string>(ids);
            // if it is too big, we drop to POST (note that means that we can't use the HTTP cache any longer)
            // we are fine with that, requests to load > 1024 items are going to be rare
            var isGet = uniqueIds.Sum(x => x.Length) < 1024;
            if (isGet)
            {
                uniqueIds.ApplyIfNotNull(id => pathBuilder.Append($"&id={Uri.EscapeDataString(id)}"));
            }
            else
            {
                request.Method = HttpMethod.Post;

                request.Content = new BlittableJsonContent(stream =>
                {
                    using (var writer = new BlittableJsonTextWriter(context, stream))
                    {
                        writer.WriteStartObject();
                        writer.WriteArray("Ids", uniqueIds);
                        writer.WriteEndObject();
                    }
                });
            }
         */
    }

    @Override
    public void setResponse(String response, boolean fromCache) throws IOException {
        if (response == null) {
            result = null;
            return;
        }

        result = mapper.readValue(response, resultClass);
    }

    @Override
    public boolean isReadRequest() {
        return true;
    }
}
