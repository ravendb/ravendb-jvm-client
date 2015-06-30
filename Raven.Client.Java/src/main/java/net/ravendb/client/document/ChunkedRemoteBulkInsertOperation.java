package net.ravendb.client.document;

import net.ravendb.abstractions.closure.Action1;
import net.ravendb.abstractions.closure.Delegates;
import net.ravendb.abstractions.data.BulkInsertOptions;
import net.ravendb.abstractions.data.Constants;
import net.ravendb.abstractions.json.linq.RavenJObject;
import net.ravendb.abstractions.util.DocumentHelpers;
import net.ravendb.client.changes.IDatabaseChanges;
import net.ravendb.client.connection.ServerClient;

import java.util.UUID;

public class ChunkedRemoteBulkInsertOperation implements ILowLevelBulkInsertOperation {

    private final BulkInsertOptions options;
    private final ServerClient client;
    private final IDatabaseChanges changes;
    private int processedItemsInCurrentOperation;
    private RemoteBulkInsertOperation current;
    private long currentChunkSize;
    private boolean disposed;
    private Action1<String> report;
    //TODO: previous task?

    public ChunkedRemoteBulkInsertOperation(BulkInsertOptions options, ServerClient serverClient, IDatabaseChanges changes) {
        this.options = options;
        this.client = serverClient;
        this.changes = changes;
        currentChunkSize = 0;
        current = getBulkInsertOperation();
    }

    @Override
    public UUID getOperationId() {
        return current == null ? Constants.EMPTY_UUID : current.getOperationId();
    }

    @Override
    public void write(String id, RavenJObject metadata, RavenJObject data) throws InterruptedException {
        write(id, metadata, data, null);
    }

    @Override
    public void write(String id, RavenJObject metadata, RavenJObject data, Integer dataSize) throws InterruptedException {
        current = getBulkInsertOperation();

        current.write(id, metadata, data, dataSize);

        if (options.getChunkedBulkInsertOptions().getMaxChunkVolumeInBytes() > 0) {
            currentChunkSize += DocumentHelpers.getRoughSize(data);
        }

        processedItemsInCurrentOperation++;
    }

    private RemoteBulkInsertOperation getBulkInsertOperation() {
        if (current == null) {
            return current = createBulkInsertOperation();
        }

        if (processedItemsInCurrentOperation < options.getChunkedBulkInsertOptions().getMaxDocumentsPerChunk()) {
            if (options.getChunkedBulkInsertOptions().getMaxChunkVolumeInBytes() <= 0 || currentChunkSize < options.getChunkedBulkInsertOptions().getMaxChunkVolumeInBytes()) {
                return current;
            }
        }

        //TODO: do we need prev task logic?

        currentChunkSize = 0;
        processedItemsInCurrentOperation = 0;
        current = createBulkInsertOperation();
        return current;
    }

    private RemoteBulkInsertOperation createBulkInsertOperation() {
        RemoteBulkInsertOperation operation = new RemoteBulkInsertOperation(options, client, changes, getOperationId());
        if (getReport() != null) {
            operation.setReport(Delegates.combine(operation.getReport(), getReport()));
        }
        return operation;
    }

    @Override
    public Action1<String> getReport() {
        return report;
    }

    @Override
    public void setReport(Action1<String> report) {
        this.report = report;
    }

    @Override
    public void abort() {
        current.abort();
    }

    @Override
    public void close() {
        if (disposed) {
            return;
        }
        if (current != null) {
            current.close();
        }
    }

    @Override
    public boolean isAborted() {
        return current != null && current.isAborted();
    }

}
