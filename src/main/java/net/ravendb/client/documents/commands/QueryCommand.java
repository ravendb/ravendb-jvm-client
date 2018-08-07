package net.ravendb.client.documents.commands;

import com.fasterxml.jackson.core.JsonGenerator;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.queries.IndexQuery;
import net.ravendb.client.documents.queries.QueryResult;
import net.ravendb.client.extensions.JsonExtensions;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.json.ContentProviderHttpEntity;
import net.ravendb.client.primitives.Reference;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;

import java.io.IOException;

@SuppressWarnings("ALL")
public class QueryCommand extends RavenCommand<QueryResult> {
    private final DocumentConventions _conventions;
    private final IndexQuery _indexQuery;
    private final boolean _metadataOnly;
    private final boolean _indexEntriesOnly;

    public QueryCommand(DocumentConventions conventions, IndexQuery indexQuery, boolean metadataOnly, boolean indexEntriesOnly) {
        super(QueryResult.class);

        _conventions = conventions;

        if (indexQuery == null) {
            throw new IllegalArgumentException("indexQuery cannot be null");
        }

        _indexQuery = indexQuery;
        _metadataOnly = metadataOnly;
        _indexEntriesOnly = indexEntriesOnly;
    }

    @Override
    public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
        canCache = !_indexQuery.isDisableCaching();

        // we won't allow aggressive caching of queries with WaitForNonStaleResults
        canCacheAggressively = canCache && !_indexQuery.isWaitForNonStaleResults();

        StringBuilder path = new StringBuilder(node.getUrl())
                .append("/databases/")
                .append(node.getDatabase())
                .append("/queries?queryHash=")
                // we need to add a query hash because we are using POST queries
                // so we need to unique parameter per query so the query cache will
                // work properly
                .append(_indexQuery.getQueryHash());

        if (_metadataOnly) {
            path.append("&metadataOnly=true");
        }

        if (_indexEntriesOnly) {
            path.append("&debug=entries");
        }

        HttpPost request = new HttpPost();
        request.setEntity(new ContentProviderHttpEntity(outputStream -> {
            try (JsonGenerator generator = mapper.getFactory().createGenerator(outputStream)) {
                JsonExtensions.writeIndexQuery(generator, _conventions, _indexQuery);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, ContentType.APPLICATION_JSON));

        url.value = path.toString();
        return request;
    }

    @Override
    public void setResponse(String response, boolean fromCache) throws IOException {
        if (response == null) {
            result = null;
            return;
        }

        result = mapper.readValue(response, QueryResult.class);
        if (fromCache) {
            result.setDurationInMs(-1);

            if (result.getTimings() != null) {
                result.getTimings().setDurationInMs(-1);
                result.setTimings(null);
            }
        }
    }

    @SuppressWarnings("SameReturnValue")
    @Override
    public boolean isReadRequest() {
        return true;
    }
}
