package net.ravendb.client.documents.commands;

import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.json.JsonArrayResult;
import net.ravendb.client.primitives.NetISO8601Utils;
import net.ravendb.client.util.UrlUtils;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.classic.methods.HttpGet;

import java.io.IOException;
import java.util.Date;

public class GetRevisionsCommand extends RavenCommand<JsonArrayResult> {

    private String _id;
    private Integer _start;
    private Integer _pageSize;
    private boolean _metadataOnly;
    private Date _before;
    private String _changeVector;
    private String[] _changeVectors;

    public GetRevisionsCommand(String changeVector) {
        this(changeVector, false);
    }

    public GetRevisionsCommand(String changeVector, boolean metadataOnly) {
        super(JsonArrayResult.class);
        _changeVector = changeVector;
        _metadataOnly = metadataOnly;
    }

    public GetRevisionsCommand(String[] changeVectors) {
        this(changeVectors, false);
    }

    public GetRevisionsCommand(String[] changeVectors, boolean metadataOnly) {
        super(JsonArrayResult.class);
        _changeVectors = changeVectors;
        _metadataOnly = metadataOnly;
    }

    public GetRevisionsCommand(String id, Date before) {
        super(JsonArrayResult.class);
        if (id == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }
        _id = id;
        _before = before;
    }

    public GetRevisionsCommand(String id, Integer start, Integer pageSize) {
        this(id, start, pageSize, false);
    }

    public GetRevisionsCommand(String id, Integer start, Integer pageSize, boolean metadataOnly) {
        super(JsonArrayResult.class);
        if (id == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }

        _id = id;
        _start = start;
        _pageSize = pageSize;
        _metadataOnly = metadataOnly;
    }

    public String getId() {
        return _id;
    }

    public Date getBefore() {
        return _before;
    }

    public String getChangeVector() {
        return _changeVector;
    }

    public String[] getChangeVectors() {
        return _changeVectors;
    }

    @Override
    public HttpUriRequestBase createRequest(ServerNode node) {
        StringBuilder pathBuilder = new StringBuilder(node.getUrl())
                .append("/databases/")
                .append(node.getDatabase())
                .append("/revisions?");

        getRequestQueryString(pathBuilder);

        String url = pathBuilder.toString();
        return new HttpGet(url);
    }

    public void getRequestQueryString(StringBuilder pathBuilder) {
        if (_id != null) {
            pathBuilder.append("&id=").append(UrlUtils.escapeDataString(_id));
        } else if (_changeVector != null) {
            pathBuilder.append("&changeVector=").append(UrlUtils.escapeDataString(_changeVector));
        } else if (_changeVectors != null) {
            for (String changeVector : _changeVectors) {
                pathBuilder.append("&changeVector=").append(UrlUtils.escapeDataString(changeVector));
            }
        }

        if (_before != null) {
            pathBuilder.append("&before=").append(NetISO8601Utils.format(_before, true));
        }

        if (_start != null) {
            pathBuilder.append("&start=").append(_start);
        }

        if (_pageSize != null) {
            pathBuilder.append("&pageSize=").append(_pageSize);
        }

        if (_metadataOnly) {
            pathBuilder.append("&metadataOnly=true");
        }


    }

    @Override
    public void setResponse(String response, boolean fromCache) throws IOException {
        if (response == null) {
            result = null;
            return;
        }

        result = mapper.readValue(response, JsonArrayResult.class);
    }

    @Override
    public boolean isReadRequest() {
        return true;
    }
}
