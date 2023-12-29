package net.ravendb.client.documents.bulkInsert;

import net.ravendb.client.exceptions.documents.bulkinsert.BulkInsertAbortedException;
import org.apache.commons.lang3.ObjectUtils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public abstract class BulkInsertOperationBase<T> {

    private boolean _streamEnsured;
    protected CompletableFuture<Void> _bulkInsertExecuteTask;

    protected long _operationId = -1;

    public abstract void store(T entity, String id);

    protected void executeBeforeStore() {
        if (!_streamEnsured) {
            waitForId();
            ensureStream();

            _streamEnsured = true;
        }

        if (_bulkInsertExecuteTask.isCompletedExceptionally()) {
            try {
                _bulkInsertExecuteTask.get();
            } catch (ExecutionException | InterruptedException e) {
                throwBulkInsertAborted(e, null);
            }
        }
    }

    protected void throwBulkInsertAborted(Exception e, Exception flushEx) {

        BulkInsertAbortedException errorFromServer = null;
        try {
            errorFromServer = getExceptionFromOperation();
        } catch (Exception ee) {
            // server is probably down, will propagate the original exception
        }

        if (errorFromServer != null) {
            throw errorFromServer;
        }

        throw new BulkInsertAbortedException("Failed to execute bulk insert",
                ObjectUtils.firstNonNull(e, flushEx));
    }

    protected abstract void waitForId();

    protected abstract void ensureStream();

    protected abstract BulkInsertAbortedException getExceptionFromOperation();

    public abstract void abort();

}