package net.ravendb.client.documents.session;

import net.ravendb.client.primitives.EventArgs;

public class AfterStoreEventArgs extends EventArgs {

    private IMetadataDictionary _documentMetadata;

    private InMemoryDocumentSessionOperations session;
    private String documentId;
    private Object entity;

    public AfterStoreEventArgs(InMemoryDocumentSessionOperations session, String documentId, Object entity) {
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

    public IMetadataDictionary getDocumentMetadata() {
        if (_documentMetadata == null) {
            _documentMetadata = session.getMetadataFor(entity);
        }

        return _documentMetadata;
    }
}
