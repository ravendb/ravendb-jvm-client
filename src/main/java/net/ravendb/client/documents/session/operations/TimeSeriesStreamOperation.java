package net.ravendb.client.documents.session.operations;

import net.ravendb.client.documents.commands.StreamCommand;
import net.ravendb.client.documents.session.InMemoryDocumentSessionOperations;
import net.ravendb.client.documents.session.StreamQueryStatistics;
import net.ravendb.client.util.UrlUtils;
import java.time.Instant;
import java.time.Duration;

public final class TimeSeriesStreamOperation extends StreamOperation {

    private final String docId;
    private final String name;
    private final Instant from;
    private final Instant to;
    private final Duration offset;

    public TimeSeriesStreamOperation(
            InMemoryDocumentSessionOperations session,
            String docId,
            String name,
            Instant from,
            Instant to,
            Duration offset) {

        super(session);

        if (docId == null || docId.isEmpty()) {
            throw new IllegalArgumentException("docId");
        }
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("name");
        }

        this.docId = docId;
        this.name = name;
        this.from = from;
        this.to = to;
        this.offset = offset;
    }

    public TimeSeriesStreamOperation(
            InMemoryDocumentSessionOperations session,
            StreamQueryStatistics statistics,
            String docId,
            String name,
            Instant from,
            Instant to,
            Duration offset) {

        super(session, statistics);

        if (docId == null || docId.isEmpty()) {
            throw new IllegalArgumentException("docId");
        }
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("name");
        }

        this.docId = docId;
        this.name = name;
        this.from = from;
        this.to = to;
        this.offset = offset;
    }

    public StreamCommand createRequest() {
        StringBuilder sb = new StringBuilder("streams/timeseries?");

        sb.append("docId=").append(UrlUtils.escapeDataString(docId)).append('&');
        sb.append("name=").append(UrlUtils.escapeDataString(name)).append('&');

        if (from != null) {
            sb.append("from=").append(from).append('&');
        }

        if (to != null) {
            sb.append("to=").append(to).append('&');
        }

        if (offset != null) {
            sb.append("offset=").append(offset).append('&');
        }

        return new StreamCommand(sb.toString());
    }
}
