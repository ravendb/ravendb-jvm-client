package net.ravendb.client.documents.session;

import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.IdTypeAndName;
import net.ravendb.client.documents.commands.batches.ICommandData;
import net.ravendb.client.http.RequestExecutor;
import org.apache.commons.lang3.StringUtils;

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


    public <T> void forceRevisionCreationFor(T entity) {
        forceRevisionCreationFor(entity, ForceRevisionStrategy.BEFORE);
    }

    public <T> void forceRevisionCreationFor(T entity, ForceRevisionStrategy strategy) {
        if (entity == null) {
            throw new IllegalArgumentException("Entity cannot be null");
        }

        DocumentInfo documentInfo = session.documentsByEntity.get(entity);
        if (documentInfo == null) {
            throw new IllegalStateException("Cannot create a revision for the requested entity because it is Not tracked by the session");
        }

        addIdToList(documentInfo.getId(), strategy);
    }

    public void forceRevisionCreationFor(String id) {
        forceRevisionCreationFor(id, ForceRevisionStrategy.BEFORE);
    }

    public void forceRevisionCreationFor(String id, ForceRevisionStrategy strategy) {
        addIdToList(id, strategy);
    }

    private void addIdToList(String id, ForceRevisionStrategy requestedStrategy) {
        if (StringUtils.isEmpty(id)) {
            throw new IllegalArgumentException("Id cannot be null or empty");
        }

        ForceRevisionStrategy existingStrategy = session.idsForCreatingForcedRevisions.get(id);
        boolean idAlreadyAdded = existingStrategy != null;
        if (idAlreadyAdded && existingStrategy != requestedStrategy) {
            throw new IllegalStateException("A request for creating a revision was already made for document "
                    + id + " in the current session but with a different force strategy." + "New strategy requested: "
                    + requestedStrategy + ". Previous strategy: " + existingStrategy + " .");
        }

        if (!idAlreadyAdded) {
            session.idsForCreatingForcedRevisions.put(id, requestedStrategy);
        }
    }
}
