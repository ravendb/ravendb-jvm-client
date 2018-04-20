package net.ravendb.client.documents.session;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.Constants;

/**
 * Information held about an entity by the session
 */
public class DocumentInfo {

    private String id;

    private String changeVector;

    private ConcurrencyCheckMode concurrencyCheckMode;

    private boolean ignoreChanges;

    private ObjectNode metadata;
    private ObjectNode document;

    private IMetadataDictionary metadataInstance;

    private Object entity;
    private boolean newDocument;
    private String collection;

    /**
     * Gets the id
     * @return Document id
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the id
     * @param id Sets the value
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Gets the ChangeVector.
     * @return change vector
     */
    public String getChangeVector() {
        return changeVector;
    }

    /**
     * Sets the ChangeVector.
     * @param changeVector Sets the value
     */
    public void setChangeVector(String changeVector) {
        this.changeVector = changeVector;
    }

    /**
     * If set to true, the session will ignore this document
     * when saveChanges() is called, and won't perform and change tracking
     * @return true is changes should be ignored
     */
    public boolean isIgnoreChanges() {
        return ignoreChanges;
    }

    /**
     * If set to true, the session will ignore this document
     * when saveChanges() is called, and won't perform and change tracking
     * @param ignoreChanges sets the value
     */
    public void setIgnoreChanges(boolean ignoreChanges) {
        this.ignoreChanges = ignoreChanges;
    }

    public ObjectNode getMetadata() {
        return metadata;
    }

    public void setMetadata(ObjectNode metadata) {
        this.metadata = metadata;
    }

    public ObjectNode getDocument() {
        return document;
    }

    public void setDocument(ObjectNode document) {
        this.document = document;
    }

    public Object getEntity() {
        return entity;
    }

    public void setEntity(Object entity) {
        this.entity = entity;
    }

    public boolean isNewDocument() {
        return newDocument;
    }

    public void setNewDocument(boolean newDocument) {
        this.newDocument = newDocument;
    }

    public String getCollection() {
        return collection;
    }

    public void setCollection(String collection) {
        this.collection = collection;
    }

    public IMetadataDictionary getMetadataInstance() {
        return metadataInstance;
    }

    public void setMetadataInstance(IMetadataDictionary metadataInstance) {
        this.metadataInstance = metadataInstance;
    }

    /**
     * A concurrency check will be forced on this entity
     * even if UseOptimisticConcurrency is set to false
     * @return concurrency check mode
     */
    public ConcurrencyCheckMode getConcurrencyCheckMode() {
        return concurrencyCheckMode;
    }

    /**
     * A concurrency check will be forced on this entity
     * even if UseOptimisticConcurrency is set to false
     * @param concurrencyCheckMode sets the value
     */
    public void setConcurrencyCheckMode(ConcurrencyCheckMode concurrencyCheckMode) {
        this.concurrencyCheckMode = concurrencyCheckMode;
    }

    public static DocumentInfo getNewDocumentInfo(ObjectNode document) {
        JsonNode metadata = document.get(Constants.Documents.Metadata.KEY);

        if (metadata == null || !metadata.isObject()) {
            throw new IllegalStateException("Document must have a metadata");
        }

        JsonNode id = metadata.get(Constants.Documents.Metadata.ID);
        if (id == null || !id.isTextual()) {
            throw new IllegalStateException("Document must have an id");
        }

        JsonNode changeVector = metadata.get(Constants.Documents.Metadata.CHANGE_VECTOR);
        if (changeVector == null || !changeVector.isTextual()) {
            throw new IllegalStateException("Document " + id.asText() + " must have a Change Vector");
        }

        DocumentInfo newDocumentInfo = new DocumentInfo();
        newDocumentInfo.setId(id.asText());
        newDocumentInfo.setDocument(document);
        newDocumentInfo.setMetadata((ObjectNode) metadata);
        newDocumentInfo.setEntity(null);
        newDocumentInfo.setChangeVector(changeVector.asText());
        return newDocumentInfo;
    }
}
