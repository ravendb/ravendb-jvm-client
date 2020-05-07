package net.ravendb.client.documents.session;

import net.ravendb.client.documents.IdTypeAndName;
import net.ravendb.client.documents.commands.batches.CommandType;
import net.ravendb.client.documents.commands.batches.ICommandData;
import net.ravendb.client.documents.commands.batches.TimeSeriesBatchCommandData;
import net.ravendb.client.documents.operations.timeSeries.TimeSeriesOperation;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

/**
 * Abstract implementation for in memory session operations
 */
public class SessionTimeSeriesBase {

    protected String docId;
    protected String name;
    protected InMemoryDocumentSessionOperations session;

    protected SessionTimeSeriesBase(InMemoryDocumentSessionOperations session, String documentId, String name) {
        if (documentId == null) {
            throw new IllegalArgumentException("DocumentId cannot be null");
        }

        if (name == null) {
            throw new IllegalArgumentException("Name cannot be null");
        }

        this.docId = documentId;
        this.name = name;
        this.session = session;
    }

    protected SessionTimeSeriesBase(InMemoryDocumentSessionOperations session, Object entity, String name) {
        DocumentInfo documentInfo = session.documentsByEntity.get(entity);
        if (documentInfo == null) {
            throwEntityNotInSession(entity);
            return;
        }

        if (StringUtils.isBlank(name)) {
            throw new IllegalArgumentException("Name cannot be null or whitespace");
        }

        this.docId = documentInfo.getId();
        this.name = name;
        this.session = session;
    }

    public void append(Date timestamp, double value) {
        append(timestamp, value, null);
    }

    public void append(Date timestamp, double value, String tag) {
        append(timestamp, new double[] { value }, tag);
    }

    public void append(Date timestamp, double[] values) {
        append(timestamp, values, null);
    }

    public void append(Date timestamp, double[] values, String tag) {
        DocumentInfo documentInfo = session.documentsById.getValue(docId);
        if (documentInfo != null && session.deletedEntities.contains(documentInfo.getEntity())) {
            throwDocumentAlreadyDeletedInSession(docId, name);
        }

        TimeSeriesOperation.AppendOperation op = new TimeSeriesOperation.AppendOperation(timestamp, values, tag);

        ICommandData command = session.deferredCommandsMap.get(IdTypeAndName.create(docId, CommandType.TIME_SERIES, name));
        if (command != null) {
            TimeSeriesBatchCommandData tsCmd = (TimeSeriesBatchCommandData) command;

            if (tsCmd.getTimeSeries().getAppends() == null) {
                tsCmd.getTimeSeries().setAppends(new ArrayList<>());
            }

            tsCmd.getTimeSeries().getAppends().add(op);
        } else {
            List<TimeSeriesOperation.AppendOperation> appends = new ArrayList<>();
            appends.add(op);
            session.defer(new TimeSeriesBatchCommandData(docId, name, appends, null));
        }
    }

    public void remove(Date at) {
        remove(at, at);
    }

    public void remove(Date from, Date to) {
        DocumentInfo documentInfo = session.documentsById.getValue(docId);
        if (documentInfo != null && session.deletedEntities.contains(documentInfo.getEntity())) {
            throwDocumentAlreadyDeletedInSession(docId, name);
        }

        TimeSeriesOperation.RemoveOperation op = new TimeSeriesOperation.RemoveOperation(from, to);

        ICommandData command = session.deferredCommandsMap.get(IdTypeAndName.create(docId, CommandType.TIME_SERIES, name));
        if (command != null) {
            TimeSeriesBatchCommandData tsCmd = (TimeSeriesBatchCommandData) command;

            if (tsCmd.getTimeSeries().getRemovals() == null) {
                tsCmd.getTimeSeries().setRemovals(new ArrayList<>());
            }

            tsCmd.getTimeSeries().getRemovals().add(op);
        } else {
            List<TimeSeriesOperation.RemoveOperation> removals = new ArrayList<>();
            removals.add(op);
            session.defer(new TimeSeriesBatchCommandData(docId, name, null, removals));
        }
    }

    private static void throwDocumentAlreadyDeletedInSession(String documentId, String timeSeries) {
        throw new IllegalStateException("Can't modify timeseries " + timeSeries + " of document " + documentId + ", the document was already delete in this session.");
    }

    protected void throwEntityNotInSession(Object entity) {
        throw new IllegalArgumentException("Entity is not associated with the session, cannot add timeseries to it. " +
                "Use documentId instead or track the entity in the session.");
    }
}
