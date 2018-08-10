package net.ravendb.client.documents.operations.counters;

import com.fasterxml.jackson.core.JsonGenerator;
import com.google.common.collect.Sets;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IOperation;
import net.ravendb.client.http.HttpCache;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.json.ContentProviderHttpEntity;
import net.ravendb.client.primitives.Reference;
import net.ravendb.client.util.UrlUtils;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

public class GetCountersOperation implements IOperation<CountersDetail> {

    private final String _docId;
    private final String[] _counters;
    private final boolean _returnFullResults;

    public GetCountersOperation(String docId, String[] counters) {
        this(docId, counters, false);
    }

    public GetCountersOperation(String docId, String[] counters, boolean returnFullResults) {
        _docId = docId;
        _counters = counters;
        _returnFullResults = returnFullResults;
    }

    public GetCountersOperation(String docId, String counter) {
        this(docId, counter, false);
    }

    public GetCountersOperation(String docId, String counter, boolean returnFullResults) {
        _docId = docId;
        _counters = new String[] { counter };
        _returnFullResults = returnFullResults;
    }

    public GetCountersOperation(String docId) {
        this(docId, false);
    }

    public GetCountersOperation(String docId, boolean returnFullResults) {
        _docId = docId;
        _counters = new String[0];
        _returnFullResults = returnFullResults;
    }

    @Override
    public RavenCommand<CountersDetail> getCommand(IDocumentStore store, DocumentConventions conventions, HttpCache cache) {
        return new GetCounterValuesCommand(_docId, _counters, _returnFullResults, conventions);
    }

    private static class GetCounterValuesCommand extends RavenCommand<CountersDetail> {
        private final String _docId;
        private final String[] _counters;
        private final boolean _returnFullResults;
        private final DocumentConventions _conventions;

        public GetCounterValuesCommand(String docId, String[] counters, boolean returnFullResults, DocumentConventions conventions) {
            super(CountersDetail.class);

            if (docId == null) {
                throw new IllegalArgumentException("DocId cannot be null");
            }

            _docId = docId;
            _counters = counters;
            _returnFullResults = returnFullResults;
            _conventions = conventions;
        }

        @Override
        public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
            StringBuilder pathBuilder = new StringBuilder(node.getUrl());
            pathBuilder.append("/databases/")
                    .append(node.getDatabase())
                    .append("/counters?docId=")
                    .append(UrlUtils.escapeDataString(_docId));

            if (_returnFullResults) {
                pathBuilder.append("&full=true");
            }

            HttpRequestBase request = new HttpGet();

            if (_counters.length > 0) {
                if (_counters.length > 1) {
                    request = prepareRequestWithMultipleCounters(pathBuilder, request);
                } else {
                    pathBuilder.append("&counter=")
                            .append(UrlUtils.escapeDataString(_counters[0]));
                }
            }

            url.value = pathBuilder.toString();

            return request;
        }

        private HttpRequestBase prepareRequestWithMultipleCounters(StringBuilder pathBuilder, HttpRequestBase request) {
            HashSet<String> uniqueNames = Sets.newHashSet(_counters);

            if (uniqueNames.stream().map(x -> x.length()).reduce((a, b) -> a + b).get() < 1024) {
                for (String uniqueName : uniqueNames) {
                    if (uniqueName != null) {
                        pathBuilder.append("&counter=")
                                .append(UrlUtils.escapeDataString(uniqueName));
                    }
                }
            } else {
                HttpPost postRequest = new HttpPost();
                request = postRequest;

                DocumentCountersOperation docOps = new DocumentCountersOperation();
                docOps.setDocumentId(_docId);
                docOps.setOperations(new ArrayList<>());

                for (String counter : _counters) {
                    CounterOperation counterOperation = new CounterOperation();
                    counterOperation.setType(CounterOperationType.GET);
                    counterOperation.setCounterName(counter);

                    docOps.getOperations().add(counterOperation);
                }

                CounterBatch batch = new CounterBatch();
                batch.setDocuments(Collections.singletonList(docOps));

                postRequest.setEntity(new ContentProviderHttpEntity(outputStream -> {
                    try (JsonGenerator generator = mapper.getFactory().createGenerator(outputStream)) {
                        batch.serialize(generator, _conventions);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }, ContentType.APPLICATION_JSON));
            }

            return request;
        }

        @Override
        public void setResponse(String response, boolean fromCache) throws IOException {
            if (response == null){
                return;
            }

            result = mapper.readValue(response, CountersDetail.class);
        }

        @Override
        public boolean isReadRequest() {
            return true;
        }
    }
}
