package net.ravendb.client.documents.commands.batches;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.session.TransactionMode;
import net.ravendb.client.http.IRaftCommand;
import net.ravendb.client.util.RaftIdGenerator;

import java.util.List;

public class ClusterWideBatchCommand extends SingleNodeBatchCommand implements IRaftCommand {

    private Boolean _disableAtomicDocumentWrites;

    public Boolean isDisableAtomicDocumentWrites() {
        return _disableAtomicDocumentWrites;
    }

    @Override
    public String getRaftUniqueRequestId() {
        return RaftIdGenerator.newId();
    }

    public ClusterWideBatchCommand(DocumentConventions conventions, List<ICommandData> commands) {
        this(conventions, commands, null);
    }

    public ClusterWideBatchCommand(DocumentConventions conventions, List<ICommandData> commands, BatchOptions options) {
        this(conventions, commands, options, null);
    }

    public ClusterWideBatchCommand(DocumentConventions conventions, List<ICommandData> commands, BatchOptions options, Boolean disableAtomicDocumentsWrites) {
        super(conventions, commands, options, TransactionMode.CLUSTER_WIDE);

        _disableAtomicDocumentWrites = disableAtomicDocumentsWrites;
    }

    @Override
    protected void appendOptions(StringBuilder sb) {
        super.appendOptions(sb);

        if (_disableAtomicDocumentWrites == null) {
            return;
        }

        sb
                .append("&disableAtomicDocumentWrites=")
                .append(_disableAtomicDocumentWrites ? "true" : "false");
    }
}
