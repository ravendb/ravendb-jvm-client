package net.ravendb.client.documents.session;

import net.ravendb.client.documents.IdTypeAndName;
import net.ravendb.client.documents.commands.batches.CommandType;
import net.ravendb.client.documents.commands.batches.CountersBatchCommandData;
import net.ravendb.client.documents.commands.batches.ICommandData;
import net.ravendb.client.documents.operations.counters.CounterOperation;
import net.ravendb.client.documents.operations.counters.CounterOperationType;
import net.ravendb.client.primitives.Tuple;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

/**
 * Abstract implementation for in memory session operations
 */
public abstract class SessionCountersBase {

    protected String docId;
    protected InMemoryDocumentSessionOperations session;

    protected SessionCountersBase(InMemoryDocumentSessionOperations session, String documentId) {
        if (StringUtils.isEmpty(documentId)) {
            throw new IllegalArgumentException("DocumentId cannot be empty");
        }

        this.docId = documentId;
        this.session = session;
    }

    protected SessionCountersBase(InMemoryDocumentSessionOperations session, Object entity) {
        DocumentInfo document = session.documentsByEntity.get(entity);

        if (document == null) {
            throwEntityNotInSession(entity);
            return;
        }

        docId = document.getId();
        this.session = session;
    }

    public void increment(String counter) {
        increment(counter, 1);
    }

    public void increment(String counter, long delta) {
        if (StringUtils.isBlank(counter)) {
            throw new IllegalArgumentException("Counter cannot be empty");
        }

        CounterOperation counterOp = new CounterOperation();
        counterOp.setType(CounterOperationType.INCREMENT);
        counterOp.setCounterName(counter);
        counterOp.setDelta(delta);

        DocumentInfo documentInfo = session.documentsById.getValue(docId);
        if (documentInfo != null && session.deletedEntities.contains(documentInfo.getEntity())) {
            throwDocumentAlreadyDeletedInSession(docId, counter);
        }

        ICommandData command = session.deferredCommandsMap.get(IdTypeAndName.create(docId, CommandType.COUNTERS, null));
        if (command != null) {
            CountersBatchCommandData countersBatchCommandData = (CountersBatchCommandData) command;
            if (countersBatchCommandData.hasDelete(counter)) {
                throwIncrementCounterAfterDeleteAttempt(docId, counter);
            }

            countersBatchCommandData.getCounters().getOperations().add(counterOp);
        } else {
            session.defer(new CountersBatchCommandData(docId, counterOp));
        }
    }

    public void delete(String counter) {
        if (StringUtils.isBlank(counter)) {
            throw new IllegalArgumentException("Counter is required");
        }

        if (session.deferredCommandsMap.containsKey(IdTypeAndName.create(docId, CommandType.DELETE, null))) {
            return; // no-op
        }

        DocumentInfo documentInfo = session.documentsById.getValue(docId);

        if (documentInfo != null && session.deletedEntities.contains(documentInfo.getEntity())) {
            return;  //no-op
        }

        CounterOperation counterOp = new CounterOperation();
        counterOp.setType(CounterOperationType.DELETE);
        counterOp.setCounterName(counter);

        ICommandData command = session.deferredCommandsMap.get(IdTypeAndName.create(docId, CommandType.COUNTERS, null));
        if (command != null) {
            CountersBatchCommandData countersBatchCommandData = (CountersBatchCommandData) command;
            if (countersBatchCommandData.hasIncrement(counter)) {
                throwDeleteCounterAfterIncrementAttempt(docId, counter);
            }

            countersBatchCommandData.getCounters().getOperations().add(counterOp);
        } else {
            session.defer(new CountersBatchCommandData(docId, counterOp));
        }

        Tuple<Boolean, Map<String, Long>> cache = session.getCountersByDocId().get(docId);
        if (cache != null) {
            cache.second.remove(counter);
        }
    }

    protected void throwEntityNotInSession(Object entity) {
        throw new IllegalArgumentException("Entity is not associated with the session, cannot add counter to it. " +
            "Use documentId instead of track the entity in the session");
    }

    private static void throwIncrementCounterAfterDeleteAttempt(String documentId, String counter) {
        throw new IllegalStateException("Can't increment counter " + counter + " of document " + documentId + ", there is a deferred command registered to delete a counter with the same name.");
    }

    private static void throwDeleteCounterAfterIncrementAttempt(String documentId, String counter) {
        throw new IllegalStateException("Can't delete counter " + counter + " of document " + documentId + ", there is a deferred command registered to increment a counter with the same name.");
    }

    private static void throwDocumentAlreadyDeletedInSession(String documentId, String counter) {
        throw new IllegalStateException("Can't increment counter " + counter + " of document " + documentId + ", the document was already deleted in this session.");
    }
}
