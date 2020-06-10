package net.ravendb.client.documents.indexes;

import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.indexes.PutIndexesOperation;
import org.apache.commons.lang3.ObjectUtils;

/**
 * Base class for creating indexes
 *
 * The naming convention is that underscores in the inherited class names are replaced by slashed
 * For example: Posts_ByName will be saved to Posts/ByName
 * @param <TIndexDefinition> Index definition
 */
public abstract class AbstractIndexCreationTaskBase<TIndexDefinition extends IndexDefinition>
        extends AbstractCommonApiForIndexes implements IAbstractIndexCreationTask {

    /**
     * Creates the index definition.
     * @return Index definition
     */
    public abstract TIndexDefinition createIndexDefinition();

    protected DocumentConventions conventions;

    protected IndexPriority priority;
    protected IndexLockMode lockMode;

    /**
     * Gets the conventions that should be used when index definition is created.
     * @return document conventions
     */
    public DocumentConventions getConventions() {
        return conventions;
    }

    /**
     * Sets the conventions that should be used when index definition is created.
     * @param conventions Conventions to set
     */
    public void setConventions(DocumentConventions conventions) {
        this.conventions = conventions;
    }

    public IndexPriority getPriority() {
        return priority;
    }

    public void setPriority(IndexPriority priority) {
        this.priority = priority;
    }

    public IndexLockMode getLockMode() {
        return lockMode;
    }

    public void setLockMode(IndexLockMode lockMode) {
        this.lockMode = lockMode;
    }

    /**
     * Executes the index creation against the specified document store.
     * @param store target document store
     */
    public void execute(IDocumentStore store) {
        execute(store, null);
    }

    /**
     * Executes the index creation against the specified document database using the specified conventions
     * @param store target document store
     * @param conventions Document conventions to use
     */
    public void execute(IDocumentStore store, DocumentConventions conventions) {
        execute(store, conventions, null);
    }

    /**
     * Executes the index creation against the specified document database using the specified conventions
     * @param store target document store
     * @param conventions Document conventions to use
     * @param database Target database
     */
    public void execute(IDocumentStore store, DocumentConventions conventions, String database) {
        DocumentConventions oldConventions = getConventions();
        try {
            String databaseForConventions = ObjectUtils.firstNonNull(database, store.getDatabase());
            setConventions(ObjectUtils.firstNonNull(conventions, getConventions(), store.getRequestExecutor(databaseForConventions).getConventions()));

            IndexDefinition indexDefinition = createIndexDefinition();
            indexDefinition.setName(getIndexName());

            if (lockMode != null) {
                indexDefinition.setLockMode(lockMode);
            }

            if (priority != null) {
                indexDefinition.setPriority(priority);
            }

            store.maintenance().forDatabase(ObjectUtils.firstNonNull(database, store.getDatabase())).send(new PutIndexesOperation(indexDefinition));
        } finally {
            setConventions(oldConventions);
        }
    }
}
