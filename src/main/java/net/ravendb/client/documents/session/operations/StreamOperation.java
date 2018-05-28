package net.ravendb.client.documents.session.operations;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.documents.CloseableIterator;
import net.ravendb.client.documents.commands.QueryStreamCommand;
import net.ravendb.client.documents.commands.StreamCommand;
import net.ravendb.client.documents.commands.StreamResultResponse;
import net.ravendb.client.documents.queries.IndexQuery;
import net.ravendb.client.documents.session.InMemoryDocumentSessionOperations;
import net.ravendb.client.documents.session.StreamQueryStatistics;
import net.ravendb.client.extensions.JsonExtensions;
import net.ravendb.client.primitives.NetISO8601Utils;
import net.ravendb.client.util.UrlUtils;

import java.io.IOException;

public class StreamOperation {
    private final InMemoryDocumentSessionOperations _session;
    private StreamQueryStatistics _statistics;
    private boolean _isQueryStream;

    public StreamOperation(InMemoryDocumentSessionOperations session) {
        _session = session;
    }

    public StreamOperation(InMemoryDocumentSessionOperations session, StreamQueryStatistics statistics) {
        _session = session;
        _statistics = statistics;
    }

    public QueryStreamCommand createRequest(IndexQuery query) {
        _isQueryStream = true;

        if (query.isWaitForNonStaleResults()) {
            throw new UnsupportedOperationException("Since stream() does not wait for indexing (by design), streaming query with setWaitForNonStaleResults is not supported");
        }

        _session.incrementRequestCount();

        return new QueryStreamCommand(_session.getConventions(), query);
    }

    public StreamCommand createRequest(String startsWith, String matches, int start, int pageSize, String exclude, String startAfter) {
        StringBuilder sb = new StringBuilder("streams/docs?");

        if (startsWith != null) {
            sb.append("startsWith=").append(UrlUtils.escapeDataString(startsWith)).append("&");
        }

        if (matches != null) {
            sb.append("matches=").append(UrlUtils.escapeDataString(matches)).append("&");
        }

        if (exclude != null) {
            sb.append("exclude=").append(UrlUtils.escapeDataString(exclude)).append("&");
        }

        if (startAfter != null) {
            sb.append("startAfter=").append(UrlUtils.escapeDataString(startAfter)).append("&");
        }

        if (start != 0) {
            sb.append("start=").append(start).append("&");
        }

        if (pageSize != Integer.MAX_VALUE) {
            sb.append("pageSize=").append(pageSize).append("&");
        }

        return new StreamCommand(sb.toString());
    }

    public CloseableIterator<ObjectNode> setResult(StreamResultResponse response)  {
        if (response == null) {
            throw new IllegalStateException("The index does not exists, failed to stream results");
        }

        try {
            JsonParser parser = JsonExtensions.getDefaultMapper().getFactory().createParser(response.getStream());

            if (parser.nextToken() != JsonToken.START_OBJECT) {
                throw new IllegalStateException("Expected start object");
            }

            if (_isQueryStream) {
                handleStreamQueryStats(parser, _statistics);
            }

            if (!"Results".equals(parser.nextFieldName())) {
                throw new IllegalStateException("Expected Results field");
            }

            if (parser.nextToken() != JsonToken.START_ARRAY) {
                throw new IllegalStateException("Expected results array start");
            }

            return new YieldStreamResults(response, parser);
        } catch (IOException e) {
            throw new RuntimeException("Unable to stream result: " + e.getMessage(), e);
        }
    }

    private static void handleStreamQueryStats(JsonParser parser, StreamQueryStatistics streamQueryStatistics) throws IOException {
        if (!"ResultEtag".equals(parser.nextFieldName())) {
            throw new IllegalStateException("Expected ResultETag field");
        }

        long resultEtag = parser.nextLongValue(0);

        if (!"IsStale".equals(parser.nextFieldName())) {
            throw new IllegalStateException("Expected IsStale field");
        }

        boolean isStale = parser.nextBooleanValue();

        if (!"IndexName".equals(parser.nextFieldName())) {
            throw new IllegalStateException("Expected IndexName field");
        }

        String indexName = parser.nextTextValue();

        if (!"TotalResults".equals(parser.nextFieldName())) {
            throw new IllegalStateException("Expected TotalResults field");
        }

        int totalResults = (int) parser.nextLongValue(0);

        if (!"IndexTimestamp".equals(parser.nextFieldName())) {
            throw new IllegalStateException("Expected IndexTimestamp field");
        }

        String indexTimestamp = parser.nextTextValue();

        if (streamQueryStatistics == null) {
            return;
        }

        streamQueryStatistics.setIndexName(indexName);
        streamQueryStatistics.setStale(isStale);
        streamQueryStatistics.setTotalResults(totalResults);
        streamQueryStatistics.setResultEtag(resultEtag);
        streamQueryStatistics.setIndexTimestamp(NetISO8601Utils.parse(indexTimestamp));
    }

    private class YieldStreamResults implements CloseableIterator<ObjectNode> {

        private final StreamResultResponse response;
        private final JsonParser parser;

        public YieldStreamResults(StreamResultResponse response, JsonParser parser) {
            this.response = response;
            this.parser = parser;
        }

        @SuppressWarnings("UnnecessaryLocalVariable")
        @Override
        public ObjectNode next() {
            try {
                ObjectNode node = JsonExtensions.getDefaultMapper().readTree(parser);
                return node;
            } catch (IOException e) {
                throw new IllegalStateException("Unable to read stream result: " + e.getMessage(), e);
            }
        }

        @Override
        public boolean hasNext() {
            try {
                JsonToken jsonToken = parser.nextToken();
                if (jsonToken == JsonToken.END_ARRAY) {

                    if (parser.nextToken() != JsonToken.END_OBJECT) {
                        throw new IllegalStateException("Expected '}' after results array");
                    }

                    return false;
                }

                return true;
            } catch (IOException e) {
                throw new IllegalStateException("Unable to read stream result: " + e.getMessage(), e);
            }
        }

        @Override
        public void close() {
            try {
                response.getResponse().close();
            } catch (IOException e) {
                throw new RuntimeException("Unable to close stream response");
            }
        }


    }
}
