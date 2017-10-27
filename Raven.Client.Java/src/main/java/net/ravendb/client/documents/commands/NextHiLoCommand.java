package net.ravendb.client.documents.commands;

import net.ravendb.client.documents.identity.HiLoResult;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.primitives.NetISO8601Utils;
import net.ravendb.client.primitives.Reference;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;

import java.io.IOException;
import java.util.Date;

public class NextHiLoCommand extends RavenCommand<HiLoResult> {

    private final String _tag;
    private final long _lastBatchSize;
    private final Date _lastRangeAt;
    private final String _identityPartsSeparator;
    private final long _lastRangeMax;

    public NextHiLoCommand(String tag, long lastBatchSize, Date lastRangeAt, String identityPartsSeparator, long lastRangeMax) {
        super(HiLoResult.class);

        if (tag == null) {
            throw new IllegalArgumentException("tag cannot be null");
        }

        if (identityPartsSeparator == null) {
            throw new IllegalArgumentException("identityPartsSeparator cannot be null");
        }

        _tag = tag;
        _lastBatchSize = lastBatchSize;
        _lastRangeAt = lastRangeAt;
        _identityPartsSeparator = identityPartsSeparator;
        _lastRangeMax = lastRangeMax;
    }

    @Override
    public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
        String date = _lastRangeAt != null ? NetISO8601Utils.format(_lastRangeAt, true) : "";
        String path = "/hilo/next?tag=" + _tag + "&lastBatchSize=" + _lastBatchSize + "&lastRangeAt=" + date + "&identityPartsSeparator=" + _identityPartsSeparator + "&lastMax=" + _lastRangeMax;

        url.value = node.getUrl() + "/databases/" + node.getDatabase() + path;

        return new HttpGet();
    }

    @Override
    public void setResponse(String response, boolean fromCache) throws IOException {
        result = mapper.readValue(response, resultClass);
    }

    @Override
    public boolean isReadRequest() {
        return true;
    }
}
