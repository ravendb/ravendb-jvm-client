package net.ravendb.client.documents.session;

import net.ravendb.client.primitives.EventArgs;

public class BeforeStoreEventArgs extends EventArgs {

    private IMetadataDictionary _documentMetadata;

    private final InMemoryDocumentSessionOperations session;
    private final String documentId;
    private final Object entity;

    public BeforeStoreEventArgs(InMemoryDocumentSessionOperations session, String documentId, Object entity) {
        this.session = session;
        this.documentId = documentId;
        this.entity = entity;
    }

    public InMemoryDocumentSessionOperations getSession() {
        return session;
    }

    public String getDocumentId() {
        return documentId;
    }

    public Object getEntity() {
        return entity;
    }

    public boolean isMetadataAccessed() {
        return _documentMetadata != null;
    }

    public IMetadataDictionary getDocumentMetadata() {
        if (_documentMetadata == null) {
            _documentMetadata = session.getMetadataFor(entity);
        }

        return _documentMetadata;
    }
}
