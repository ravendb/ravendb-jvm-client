package net.ravendb.client.documents.indexes;

import net.ravendb.client.Constants;
import net.ravendb.client.documents.DocumentStoreBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.dataArchival.ArchivedDataProcessingBehavior;
import net.ravendb.client.documents.operations.indexes.PutIndexesOperation;
import net.ravendb.client.primitives.SharpEnum;
import org.apache.commons.lang3.ObjectUtils;

import java.util.List;

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

    protected IndexDeploymentMode deploymentMode;
    protected SearchEngineType searchEngineType;
    protected ArchivedDataProcessingBehavior archivedDataProcessingBehavior;
    protected IndexState state;

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

    public IndexDeploymentMode getDeploymentMode() {
        return deploymentMode;
    }

    public void setDeploymentMode(IndexDeploymentMode deploymentMode) {
        this.deploymentMode = deploymentMode;
    }

    public SearchEngineType getSearchEngineType() {
        return searchEngineType;
    }

    public void setSearchEngineType(SearchEngineType searchEngineType) {
        this.searchEngineType = searchEngineType;
    }

    public ArchivedDataProcessingBehavior getArchivedDataProcessingBehavior() {
        return archivedDataProcessingBehavior;
    }

    public void setArchivedDataProcessingBehavior(ArchivedDataProcessingBehavior archivedDataProcessingBehavior) {
        this.archivedDataProcessingBehavior = archivedDataProcessingBehavior;
    }

    public IndexState getState() {
        return state;
    }

    public void setState(IndexState state) {
        this.state = state;
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
        database = DocumentStoreBase.getEffectiveDatabase(store, database);
        try {

            setConventions(ObjectUtils.firstNonNull(conventions, getConventions(),
                    store.getRequestExecutor(database).getConventions()));

            IndexDefinition indexDefinition = createIndexDefinition();
            indexDefinition.setName(getIndexName());

            if (lockMode != null) {
                indexDefinition.setLockMode(lockMode);
            }

            if (priority != null) {
                indexDefinition.setPriority(priority);
            }

            if (state != null) {
                indexDefinition.setState(state);
            }

            if (archivedDataProcessingBehavior != null) {
                indexDefinition.setArchivedDataProcessingBehavior(archivedDataProcessingBehavior);
            }

            if (deploymentMode != null) {
                indexDefinition.setDeploymentMode(deploymentMode);
            }


            store.maintenance()
                    .forDatabase(database)
                    .send(new PutIndexesOperation(indexDefinition));
        } finally {
            setConventions(oldConventions);
        }
    }
}
