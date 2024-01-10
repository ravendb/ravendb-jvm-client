package net.ravendb.client.documents.operations.counters;

import com.fasterxml.jackson.core.JsonGenerator;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IOperation;
import net.ravendb.client.http.HttpCache;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.json.ContentProviderHttpEntity;
import net.ravendb.client.primitives.Reference;
import net.ravendb.client.util.UrlUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;

import java.io.IOException;
import java.util.*;

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
        public HttpUriRequestBase createRequest(ServerNode node) {
            StringBuilder pathBuilder = new StringBuilder(node.getUrl());
            pathBuilder.append("/databases/")
                    .append(node.getDatabase())
                    .append("/counters?docId=")
                    .append(UrlUtils.escapeDataString(_docId));

            if (_counters != null && _counters.length > 0) {
                if (_counters.length > 1) {
                    HttpEntity entity = prepareRequestWithMultipleCounters(pathBuilder);
                    if (entity != null) {
                        HttpPost httpPost = new HttpPost(pathBuilder.toString());
                        httpPost.setEntity(entity);
                        return httpPost;
                    }
                } else {
                    pathBuilder.append("&counter=")
                            .append(UrlUtils.escapeDataString(_counters[0]));
                }
            }

            if (_returnFullResults) { // if we dropped to Post, _returnFullResults is part of the request content
                pathBuilder.append("&full=true");
            }

            return new HttpGet(pathBuilder.toString());
        }

        private HttpEntity prepareRequestWithMultipleCounters(StringBuilder pathBuilder) {
            Reference<Integer> sumLengthRef = new Reference<>();
            List<String> uniqueNames = getOrderedUniqueNames(sumLengthRef);

            // if it is too big, we drop to POST (note that means that we can't use the HTTP cache any longer)
            // we are fine with that, such requests are going to be rare
            if (sumLengthRef.value < 1024) {
                for (String uniqueName : uniqueNames) {
                    pathBuilder.append("&counter=")
                            .append(UrlUtils.escapeDataString(ObjectUtils.firstNonNull(uniqueName, "")));
                }
                return null;
            } else {
                DocumentCountersOperation docOps = new DocumentCountersOperation();
                docOps.setDocumentId(_docId);
                docOps.setOperations(new ArrayList<>());

                for (String counter : uniqueNames) {
                    CounterOperation counterOperation = new CounterOperation();
                    counterOperation.setType(CounterOperationType.GET);
                    counterOperation.setCounterName(counter);

                    docOps.getOperations().add(counterOperation);
                }

                CounterBatch batch = new CounterBatch();
                batch.setDocuments(Collections.singletonList(docOps));
                batch.setReplyWithAllNodesValues(_returnFullResults);

                return new ContentProviderHttpEntity(outputStream -> {
                    try (JsonGenerator generator = createSafeJsonGenerator(outputStream)) {
                        batch.serialize(generator);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }, ContentType.APPLICATION_JSON, _conventions);
            }
        }

        @Override
        public void setResponse(String response, boolean fromCache) throws IOException {
            if (response == null) {
                return;
            }

            result = mapper.readValue(response, CountersDetail.class);
        }

        private List<String> getOrderedUniqueNames(Reference<Integer> sumRef) {
            Set<String> uniqueNames = new HashSet<>();
            List<String> orderedUniqueNames = new ArrayList<>();

            sumRef.value = 0;

            for (String counter : _counters) {
                if (uniqueNames.add(counter)) {
                    orderedUniqueNames.add(counter);
                    sumRef.value += counter != null ? counter.length() : 0;
                }
            }

            return orderedUniqueNames;
        }

        @Override
        public boolean isReadRequest() {
            return true;
        }
    }
}
