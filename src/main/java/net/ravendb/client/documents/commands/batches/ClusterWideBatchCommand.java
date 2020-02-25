package net.ravendb.client.documents.commands.batches;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.session.TransactionMode;
import net.ravendb.client.http.IRaftCommand;
import net.ravendb.client.util.RaftIdGenerator;

import java.util.List;

public class ClusterWideBatchCommand extends SingleNodeBatchCommand implements IRaftCommand {

    @Override
    public String getRaftUniqueRequestId() {
        return RaftIdGenerator.newId();
    }

    public ClusterWideBatchCommand(DocumentConventions conventions, List<ICommandData> commands) {
        this(conventions, commands, null);
    }

    public ClusterWideBatchCommand(DocumentConventions conventions, List<ICommandData> commands, BatchOptions options) {
        super(conventions, commands, options, TransactionMode.CLUSTER_WIDE);
    }
}
