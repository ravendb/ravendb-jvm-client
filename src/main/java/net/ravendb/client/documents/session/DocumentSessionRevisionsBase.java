package net.ravendb.client.documents.session;

import org.apache.commons.lang3.StringUtils;

public class DocumentSessionRevisionsBase extends AdvancedSessionExtensionBase {
    public DocumentSessionRevisionsBase(InMemoryDocumentSessionOperations session) {
        super(session);
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
