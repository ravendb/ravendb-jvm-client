package net.ravendb.client.documents.session;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import net.ravendb.client.Constants;
import net.ravendb.client.documents.operations.timeSeries.*;
import net.ravendb.client.documents.session.timeSeries.TimeSeriesEntry;
import net.ravendb.client.primitives.NetISO8601Utils;
import net.ravendb.client.primitives.Reference;
import org.apache.commons.lang3.ObjectUtils;

import java.util.*;
import java.util.stream.Collectors;

public class SessionDocumentTimeSeries extends SessionTimeSeriesBase
        implements ISessionDocumentTimeSeries {

    public SessionDocumentTimeSeries(InMemoryDocumentSessionOperations session, String documentId, String name) {
        super(session, documentId, name);
    }

    public SessionDocumentTimeSeries(InMemoryDocumentSessionOperations session, Object entity, String name) {
        super(session, entity, name);
    }

    @Override
    public List<TimeSeriesEntry> get() {
        return get(null, null, 0, Integer.MAX_VALUE);
    }

    @Override
    public List<TimeSeriesEntry> get(Date from, Date to) {
        return get(from, to, 0, Integer.MAX_VALUE);
    }

    @Override
    public List<TimeSeriesEntry> get(Date from, Date to, int start) {
        return get(from, to, start, Integer.MAX_VALUE);
    }

    @Override
    public List<TimeSeriesEntry> get(Date from, Date to, int start, int pageSize) {
        return getInternal(from, to, start, pageSize);
    }





}
