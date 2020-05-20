package net.ravendb.client.documents.operations.timeSeries;

import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IOperation;
import net.ravendb.client.http.HttpCache;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.primitives.NetISO8601Utils;
import net.ravendb.client.primitives.Reference;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;

import java.io.IOException;
import java.util.List;

public class GetMultipleTimeSeriesOperation implements IOperation<TimeSeriesDetails> {

    private final String _docId;
    private List<TimeSeriesRange> _ranges;
    private final int _start;
    private final int _pageSize;

    public GetMultipleTimeSeriesOperation(String docId, List<TimeSeriesRange> ranges) {
        this(docId, ranges, 0, Integer.MAX_VALUE);
    }

    public GetMultipleTimeSeriesOperation(String docId, List<TimeSeriesRange> ranges, int start, int pageSize) {
        this(docId, start, pageSize);

        if (ranges == null) {
            throw new IllegalArgumentException("Ranges cannot be null");
        }

        _ranges = ranges;
    }

    private GetMultipleTimeSeriesOperation(String docId, int start, int pageSize) {
        if (StringUtils.isEmpty(docId)) {
            throw new IllegalArgumentException("DocId cannot be null or empty");
        }

        _docId = docId;
        _start = start;
        _pageSize = pageSize;
    }

    @Override
    public RavenCommand<TimeSeriesDetails> getCommand(IDocumentStore store, DocumentConventions conventions, HttpCache cache) {
        return new GetMultipleTimeSeriesCommand(_docId, _ranges, _start, _pageSize);
    }

    public static class GetMultipleTimeSeriesCommand extends RavenCommand<TimeSeriesDetails> {
        private final String _docId;
        private final List<TimeSeriesRange> _ranges;
        private final int _start;
        private final int _pageSize;

        public GetMultipleTimeSeriesCommand(String docId, List<TimeSeriesRange> ranges, int start, int pageSize) {
            super(TimeSeriesDetails.class);

            if (docId == null) {
                throw new IllegalArgumentException("DocId cannot be null");
            }

            _docId = docId;
            _ranges = ranges;
            _start = start;
            _pageSize = pageSize;
        }

        @Override
        public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
            StringBuilder pathBuilder = new StringBuilder(node.getUrl());

            pathBuilder
                    .append("/databases/")
                    .append(node.getDatabase())
                    .append("/timeseries/ranges")
                    .append("?docId=")
                    .append(urlEncode(_docId));

            if (_start > 0) {
                pathBuilder
                        .append("&start=")
                        .append(_start);
            }

            if (_pageSize < Integer.MAX_VALUE) {
                pathBuilder
                        .append("&pageSize=")
                        .append(_pageSize);
            }

            if (_ranges.isEmpty()) {
                throw new IllegalArgumentException("Ranges cannot be null or empty");
            }

            for (TimeSeriesRange range : _ranges) {
                if (StringUtils.isEmpty(range.getName())) {
                    throw new IllegalArgumentException("Missing name argument in TimeSeriesRange. Name cannot be null or empty");
                }

                pathBuilder
                        .append("&name=")
                        .append(ObjectUtils.firstNonNull(range.getName(), ""))
                        .append("&from=")
                        .append(range.getFrom() == null ? "" : NetISO8601Utils.format(range.getFrom(), true))
                        .append("&to=")
                        .append(range.getTo() == null ? "" : NetISO8601Utils.format(range.getTo(), true));
            }

            url.value = pathBuilder.toString();

            return new HttpGet();
        }

        @Override
        public void setResponse(String response, boolean fromCache) throws IOException {
            if (response == null) {
                return;
            }

            result = mapper.readValue(response, resultClass);
        }

        @Override
        public boolean isReadRequest() {
            return true;
        }
    }
}
