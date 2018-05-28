package net.ravendb.client.documents.session;

import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.IdTypeAndName;
import net.ravendb.client.documents.commands.batches.ICommandData;
import net.ravendb.client.http.RequestExecutor;

import java.util.Map;
import java.util.Set;

@SuppressWarnings("SpellCheckingInspection")
public abstract class AdvancedSessionExtentionBase {

    protected final InMemoryDocumentSessionOperations session;
    protected final Map<Object, DocumentInfo> documentsByEntity;
    protected final RequestExecutor requestExecutor;
    protected final SessionInfo sessionInfo;
    protected final IDocumentStore documentStore;
    protected final Map<IdTypeAndName, ICommandData> deferredCommandsMap;

    protected final Set<Object> deletedEntities;
    protected final DocumentsById documentsById;

    protected AdvancedSessionExtentionBase(InMemoryDocumentSessionOperations session) {
        this.session = session;
        this.documentsByEntity = session.documentsByEntity;
        this.requestExecutor = session.getRequestExecutor();
        this.sessionInfo = session.sessionInfo;
        this.documentStore = session.getDocumentStore();
        this.deferredCommandsMap = session.deferredCommandsMap;
        this.deletedEntities = session.deletedEntities;
        this.documentsById = session.documentsById;
    }

    public void defer(ICommandData command, ICommandData... commands) {
        session.defer(command, commands);
    }
}
