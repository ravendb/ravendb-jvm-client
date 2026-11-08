package net.ravendb.client.documents.operations;

import com.fasterxml.jackson.core.JsonGenerator;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.queries.IndexPatchOptions;
import net.ravendb.client.documents.queries.IndexQuery;
import net.ravendb.client.documents.queries.QueryOperationOptions;
import net.ravendb.client.extensions.JsonExtensions;
import net.ravendb.client.http.HttpCache;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.json.ContentProviderHttpEntity;
import net.ravendb.client.util.TimeUtils;
import net.ravendb.client.util.UrlUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.hc.client5.http.classic.methods.HttpPatch;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.core5.http.ContentType;

import java.io.IOException;

public class PatchByQueryOperation implements IOperation<OperationIdResult> {

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

    static void appendIndexPatchOptions(StringBuilder path, QueryOperationOptions options) {
        IndexPatchOptions indexPatchOptions = options.getIndexPatchOptions();
        if (indexPatchOptions == null) {
            return;
        }

        path.append("&waitForIndexesTimeout=").append(TimeUtils.durationToTimeSpan(indexPatchOptions.getWaitForIndexesTimeout()));
        path.append("&throwOnTimeoutInWaitForIndexes=").append(indexPatchOptions.isThrowOnTimeoutInWaitForIndexes());

        if (indexPatchOptions.getWaitForSpecificIndexes() == null) {
            return;
        }

        for (String specificIndex : indexPatchOptions.getWaitForSpecificIndexes()) {
            path.append("&waitForSpecificIndexes=").append(UrlUtils.escapeDataString(specificIndex));
        }
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
        public HttpUriRequestBase createRequest(ServerNode node) {
            String path = node.getUrl() + "/databases/" + node.getDatabase() + "/queries?allowStale="
                    + _options.isAllowStale();
            if (_options.getMaxOpsPerSecond() != null) {
                path += "&maxOpsPerSec=" + _options.getMaxOpsPerSecond();
            }

            path += "&details=" + _options.isRetrieveDetails();

            if (_options.getStaleTimeout() != null) {
                path += "&staleTimeout=" + TimeUtils.durationToTimeSpan(_options.getStaleTimeout());
            }

            if (_options.isIgnoreMaxStepsForScript()) {
                path += "&ignoreMaxStepsForScript=" + _options.isIgnoreMaxStepsForScript();
            }

            StringBuilder pathBuilder = new StringBuilder(path);
            appendIndexPatchOptions(pathBuilder, _options);

            HttpPatch request = new HttpPatch(pathBuilder.toString());
            request.setEntity(new ContentProviderHttpEntity(outputStream -> {
                try (JsonGenerator generator = createSafeJsonGenerator(outputStream)) {
                    generator.writeStartObject();

                    generator.writeFieldName("Query");
                    JsonExtensions.writeIndexQuery(generator, _conventions, _queryToUpdate);

                    generator.writeEndObject();
                }
            }, ContentType.APPLICATION_JSON, _conventions));

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
