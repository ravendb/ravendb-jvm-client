package net.ravendb.client.documents.commands;

import com.fasterxml.jackson.core.JsonGenerator;
import net.ravendb.client.Parameters;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.queries.IndexQuery;
import net.ravendb.client.documents.queries.QueryResult;
import net.ravendb.client.documents.session.InMemoryDocumentSessionOperations;
import net.ravendb.client.extensions.JsonExtensions;
import net.ravendb.client.json.ContentProviderHttpEntity;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;

import java.io.IOException;

@SuppressWarnings("ALL")
public class QueryCommand extends AbstractQueryCommand<QueryResult, Parameters> {
    private final DocumentConventions _conventions;
    private final IndexQuery _indexQuery;
    private final InMemoryDocumentSessionOperations _session;

    public QueryCommand(InMemoryDocumentSessionOperations session, IndexQuery indexQuery, boolean metadataOnly, boolean indexEntriesOnly) {
        super(QueryResult.class, indexQuery, !indexQuery.isDisableCaching(), metadataOnly, indexEntriesOnly, false);

        _session = session;

        if (indexQuery == null) {
            throw new IllegalArgumentException("indexQuery cannot be null");
        }

        _indexQuery = indexQuery;
        _conventions = session.getConventions();
    }

    protected String getQueryHash() {
        return _indexQuery.getQueryHash(_session.getConventions().getEntityMapper());
    }


    protected HttpEntity getContent() {
        return new ContentProviderHttpEntity(outputStream -> {
            try (JsonGenerator generator = createSafeJsonGenerator(outputStream)) {
                JsonExtensions.writeIndexQuery(generator, _session.getConventions(), _indexQuery);
            }
        }, ContentType.APPLICATION_JSON, _conventions);
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
}
