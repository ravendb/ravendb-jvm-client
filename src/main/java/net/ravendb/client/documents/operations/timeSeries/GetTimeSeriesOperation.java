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
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class GetTimeSeriesOperation implements IOperation<TimeSeriesDetails> {
    private final String _docId;
    private List<TimeSeriesRange> _ranges;
    private final int _start;
    private final int _pageSize;

    public GetTimeSeriesOperation(String docId, String timeseries, Date from, Date to) {
        this(docId, timeseries, from, to, 0, Integer.MAX_VALUE);
    }

    public GetTimeSeriesOperation(String docId, String timeseries, Date from, Date to, int start) {
        this(docId, timeseries, from, to, start, Integer.MAX_VALUE);
    }

    public GetTimeSeriesOperation(String docId, String timeseries, Date from, Date to, int start, int pageSize) {
        this(docId, start, pageSize);

        TimeSeriesRange range = new TimeSeriesRange();
        range.setName(timeseries);
        range.setFrom(from);
        range.setTo(to);

        _ranges = Collections.singletonList(range);
    }

    public GetTimeSeriesOperation(String docId, List<TimeSeriesRange> ranges) {
        this(docId, ranges, 0, Integer.MAX_VALUE);
    }

    public GetTimeSeriesOperation(String docId, List<TimeSeriesRange> ranges, int start) {
        this(docId, ranges, start, Integer.MAX_VALUE);
    }

    public GetTimeSeriesOperation(String docId, List<TimeSeriesRange> ranges, int start, int pageSize) {
        this(docId, start, pageSize);

        if (ranges == null) {
            throw new IllegalArgumentException("Ranges cannot be null");
        }

        _ranges = ranges;
    }

    private GetTimeSeriesOperation(String docId, int start, int pageSize) {
        if (StringUtils.isEmpty(docId)) {
            throw new IllegalArgumentException("DocId cannot be null or empty");
        }

        _docId = docId;
        _start = start;
        _pageSize = pageSize;
    }

    @Override
    public RavenCommand<TimeSeriesDetails> getCommand(IDocumentStore store, DocumentConventions conventions, HttpCache cache) {
        return new GetTimeSeriesCommand(_docId, _ranges, _start, _pageSize);
    }

    private static class GetTimeSeriesCommand extends RavenCommand<TimeSeriesDetails> {
        private final String _docId;
        private final List<TimeSeriesRange> _ranges;
        private final int _start;
        private final int _pageSize;

        public GetTimeSeriesCommand(String docId, List<TimeSeriesRange> ranges, int start, int pageSize) {
            super(TimeSeriesDetails.class);

            if (docId == null) {
                throw new IllegalArgumentException("DocumentId cannot be null");
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
                    .append("/timeseries")
                    .append("?id=")
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

            for (TimeSeriesRange range : _ranges) {
                pathBuilder
                        .append("&name=")
                        .append(ObjectUtils.firstNonNull(range.getName(), "")) //TODO: c# can we throw on client side?
                        .append("&from=")
                        .append(range.getFrom() == null ? NetISO8601Utils.MIN_DATE_AS_STRING : NetISO8601Utils.format(range.getFrom()))
                        .append("&to=")
                        .append(range.getTo() == null ? NetISO8601Utils.MAX_DATE_AS_STRING : NetISO8601Utils.format(range.getTo()));
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
