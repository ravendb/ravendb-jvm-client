package net.ravendb.client.documents.session;

import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.IdTypeAndName;
import net.ravendb.client.documents.commands.batches.ICommandData;
import net.ravendb.client.http.RequestExecutor;

import java.util.Map;

public abstract class AdvancedSessionExtensionBase {

    protected final InMemoryDocumentSessionOperations session;
    protected final RequestExecutor requestExecutor;
    protected final SessionInfo sessionInfo;
    protected final IDocumentStore documentStore;
    protected final Map<IdTypeAndName, ICommandData> deferredCommandsMap;
    protected final DocumentsById documentsById;

    protected AdvancedSessionExtensionBase(InMemoryDocumentSessionOperations session) {
        this.session = session;
        this.requestExecutor = session.getRequestExecutor();
        this.sessionInfo = session.sessionInfo;
        this.documentStore = session.getDocumentStore();
        this.deferredCommandsMap = session.deferredCommandsMap;
        this.documentsById = session.documentsById;
    }

    public void defer(ICommandData command, ICommandData... commands) {
        session.defer(command, commands);
    }

}
