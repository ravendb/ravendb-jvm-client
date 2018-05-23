package net.ravendb.client.documents.operations;

import com.fasterxml.jackson.core.JsonGenerator;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.queries.IndexQuery;
import net.ravendb.client.documents.queries.QueryOperationOptions;
import net.ravendb.client.extensions.JsonExtensions;
import net.ravendb.client.http.HttpCache;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.json.ContentProviderHttpEntity;
import net.ravendb.client.primitives.Reference;
import net.ravendb.client.util.TimeUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;

import java.io.IOException;

public class PatchByQueryOperation implements IOperation<OperationIdResult> {

    protected static IndexQuery DUMMY_QUERY = new IndexQuery();

    private final IndexQuery _queryToUpdate;
    private final QueryOperationOptions _options;

    public PatchByQueryOperation(String queryToUpdate) {
        this(new IndexQuery(queryToUpdate));
    }

    public PatchByQueryOperation(IndexQuery queryToUpdate) {
        this(queryToUpdate, null);
    }
    public PatchByQueryOperation(IndexQuery queryToUpdate, QueryOperationOptions options) {
        if (queryToUpdate == null) {
            throw new IllegalArgumentException("QueryToUpdate cannot be null");
        }

        _queryToUpdate = queryToUpdate;
        _options = options;
    }

    @Override
    public RavenCommand<OperationIdResult> getCommand(IDocumentStore store, DocumentConventions conventions, HttpCache cache) {
        return new PatchByQueryCommand(conventions, _queryToUpdate, _options);
    }

    private static class PatchByQueryCommand extends RavenCommand<OperationIdResult> {
        private final DocumentConventions _conventions;
        private final IndexQuery _queryToUpdate;
        private final QueryOperationOptions _options;

        public PatchByQueryCommand(DocumentConventions conventions, IndexQuery queryToUpdate, QueryOperationOptions options) {
            super(OperationIdResult.class);
            _conventions = conventions;
            _queryToUpdate = queryToUpdate;
            _options = ObjectUtils.firstNonNull(options, new QueryOperationOptions());
        }

        @Override
        public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
            String path = node.getUrl() + "/databases/" + node.getDatabase() + "/queries?allowStale="
                    + _options.isAllowStale();
            if (_options.getMaxOpsPerSecond() != null) {
                path += "&maxOpsPerSec=" + _options.getMaxOpsPerSecond();
            }

            path += "&details=" + _options.isRetrieveDetails();

            if (_options.getStaleTimeout() != null) {
                path += "&staleTimeout=" + TimeUtils.durationToTimeSpan(_options.getStaleTimeout());
            }

            HttpPatch request = new HttpPatch();
            request.setEntity(new ContentProviderHttpEntity(outputStream -> {
                try (JsonGenerator generator = mapper.getFactory().createGenerator(outputStream)) {
                    generator.writeStartObject();

                    generator.writeFieldName("Query");
                    JsonExtensions.writeIndexQuery(generator, _conventions, _queryToUpdate);

                    generator.writeEndObject();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }, ContentType.APPLICATION_JSON));

            url.value = path;


            return request;
        }

        @Override
        public void setResponse(String response, boolean fromCache) throws IOException {
            if (response == null) {
                throwInvalidResponse();
            }

            result = mapper.readValue(response, OperationIdResult.class);
        }

        @Override
        public boolean isReadRequest() {
            return false;
        }
    }
}
