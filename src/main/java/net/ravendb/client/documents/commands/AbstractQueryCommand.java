package net.ravendb.client.documents.commands;

import net.ravendb.client.documents.queries.IndexQuery;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.primitives.Reference;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;

public abstract class AbstractQueryCommand<TResult, TParameters> extends RavenCommand<TResult> {

    private final boolean _metadataOnly;
    private final boolean _indexEntriesOnly;
    private final boolean _ignoreLimit;

    public AbstractQueryCommand(Class<TResult> queryResultClass, IndexQuery indexQuery, boolean canCache, boolean metadataOnly, boolean indexEntriesOnly, boolean ignoreLimit) {
        super(queryResultClass);
        _metadataOnly = metadataOnly;
        _indexEntriesOnly = indexEntriesOnly;
        _ignoreLimit = ignoreLimit;

        this.canCache = canCache;

        // we won't allow aggressive caching of queries with WaitForNonStaleResults
        this.canCacheAggressively = canCache && !indexQuery.isWaitForNonStaleResults();
    }

    @SuppressWarnings("SameReturnValue")
    @Override
    public boolean isReadRequest() {
        return true;
    }

    protected abstract String getQueryHash();

    @Override
    public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
        StringBuilder path = new StringBuilder(node.getUrl())
                .append("/databases/")
                .append(node.getDatabase())
                .append("/queries?queryHash=")
                // we need to add a query hash because we are using POST queries
                // so we need to unique parameter per query so the query cache will
                // work properly
                .append(getQueryHash());

        if (_metadataOnly) {
            path.append("&metadataOnly=true");
        }

        if (_indexEntriesOnly) {
            path.append("&debug=entries");
        }

        if (_ignoreLimit) {
            path.append("&ignoreLimit=true");
        }

        HttpPost request = new HttpPost();
        request.setEntity(getContent());

        url.value = path.toString();
        return request;
    }

    protected abstract HttpEntity getContent();

}
