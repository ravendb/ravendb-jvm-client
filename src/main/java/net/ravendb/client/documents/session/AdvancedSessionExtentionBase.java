package net.ravendb.client.documents.session;

import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.IdTypeAndName;
import net.ravendb.client.documents.commands.batches.ICommandData;
import net.ravendb.client.http.RequestExecutor;

import java.util.Map;
import java.util.Set;

public abstract class AdvancedSessionExtentionBase {

    protected InMemoryDocumentSessionOperations session;
    protected Map<Object, DocumentInfo> documentsByEntity;
    protected RequestExecutor requestExecutor;
    protected SessionInfo sessionInfo;
    protected IDocumentStore documentStore;
    protected Map<IdTypeAndName, ICommandData> deferredCommandsMap;

    protected Set<Object> deletedEntities;
    protected DocumentsById documentsById;

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
