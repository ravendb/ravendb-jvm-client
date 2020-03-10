package net.ravendb.client.documents.commands.batches;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.session.TransactionMode;
import net.ravendb.client.http.IRaftCommand;
import net.ravendb.client.primitives.CleanCloseable;
import net.ravendb.client.util.RaftIdGenerator;

import java.util.List;

/**
 * @deprecated BatchCommand is not supported anymore. Will be removed in next major version of the product.
 */
public class BatchCommand extends SingleNodeBatchCommand implements CleanCloseable, IRaftCommand {
    public BatchCommand(DocumentConventions conventions, List<ICommandData> commands) {
        super(conventions, commands);
    }

    public BatchCommand(DocumentConventions conventions, List<ICommandData> commands, BatchOptions options, TransactionMode mode) {
        super(conventions, commands, options, mode);
    }

    @Override
    public String getRaftUniqueRequestId() {
        return RaftIdGenerator.newId();
    }
}
