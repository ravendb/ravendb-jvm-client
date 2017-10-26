package net.ravendb.client.documents.session;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.xml.internal.bind.v2.runtime.reflect.opt.Const;
import net.ravendb.client.Constants;

import javax.print.Doc;

/**
 * Information held about an entity by the session
 */
public class DocumentInfo {

    private String id;

    private String changeVector;

    private InMemoryDocumentSessionOperations.ConcurrencyCheckMode concurrencyCheckMode;

    private boolean ignoreChanges;

    private ObjectNode metadata;
    private ObjectNode document;

    //tODO: public IMetadataDictionary MetadataInstance { get; set; }

    private Object entity;
    private boolean newDocument;
    private String collection;

    /**
     * Gets the id
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the id
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Gets  the ChangeVector.
     */
    public String getChangeVector() {
        return changeVector;
    }

    /**
     * Sets the ChangeVector.
     */
    public void setChangeVector(String changeVector) {
        this.changeVector = changeVector;
    }

    /**
     * If set to true, the session will ignore this document
     * when saveChanges() is called, and won't perform and change tracking
     */
    public boolean isIgnoreChanges() {
        return ignoreChanges;
    }

    /**
     * If set to true, the session will ignore this document
     * when saveChanges() is called, and won't perform and change tracking
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

    /**
     * A concurrency check will be forced on this entity
     * even if UseOptimisticConcurrency is set to false
     */
    public InMemoryDocumentSessionOperations.ConcurrencyCheckMode getConcurrencyCheckMode() {
        return concurrencyCheckMode;
    }

    /**
     * A concurrency check will be forced on this entity
     * even if UseOptimisticConcurrency is set to false
     */
    public void setConcurrencyCheckMode(InMemoryDocumentSessionOperations.ConcurrencyCheckMode concurrencyCheckMode) {
        this.concurrencyCheckMode = concurrencyCheckMode;
    }

    public static DocumentInfo getNewDocumentInfo(ObjectNode document) {
        JsonNode metadata = document.get(Constants.Documents.Metadata.KEY);
        JsonNode id = metadata.get(Constants.Documents.Metadata.ID);
        JsonNode changeVector = metadata.get(Constants.Documents.Metadata.CHANGE_VECTOR);

        if (metadata == null || !metadata.isObject()) {
            throw new IllegalStateException("Document must have a metadata");
        }

        if (id == null || !id.isTextual()) {
            throw new IllegalStateException("Document must have an id");
        }

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
