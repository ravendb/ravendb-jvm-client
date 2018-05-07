package net.ravendb.client.documents.indexes;

import net.ravendb.client.Constants;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.indexes.spatial.SpatialOptions;
import net.ravendb.client.documents.indexes.spatial.SpatialOptionsFactory;
import net.ravendb.client.documents.operations.indexes.PutIndexesOperation;
import org.apache.commons.lang3.ObjectUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Base class for creating indexes
 */
public abstract class AbstractIndexCreationTask {

    protected String map;
    protected String reduce;

    protected DocumentConventions conventions;
    protected Map<String, String> additionalSources;
    protected IndexPriority priority;
    protected IndexLockMode lockMode;

    protected final Map<String, FieldStorage> storesStrings;
    protected final Map<String, FieldIndexing> indexesStrings;
    protected final Map<String, String> analyzersStrings;
    protected final Set<String> indexSuggestions;
    protected final Map<String, FieldTermVector> termVectorsStrings;
    protected final Map<String, SpatialOptions> spatialOptionsStrings;

    protected String outputReduceToCollection;

    public AbstractIndexCreationTask() {
        storesStrings = new HashMap<>();
        indexesStrings= new HashMap<>();
        analyzersStrings = new HashMap<>();
        indexSuggestions = new HashSet<>();
        termVectorsStrings = new HashMap<>();
        spatialOptionsStrings = new HashMap<>();
    }

    public Map<String, String> getAdditionalSources() {
        return additionalSources;
    }

    public void setAdditionalSources(Map<String, String> additionalSources) {
        this.additionalSources = additionalSources;
    }

    /**
     * Creates the index definition.
     * @return Index definition
     */
    public IndexDefinition createIndexDefinition() {
        if (conventions == null) {
            conventions = new DocumentConventions();
        }

        IndexDefinitionBuilder indexDefinitionBuilder = new IndexDefinitionBuilder(getIndexName());
        indexDefinitionBuilder.setIndexesStrings(indexesStrings);
        indexDefinitionBuilder.setAnalyzersStrings(analyzersStrings);
        indexDefinitionBuilder.setMap(map);
        indexDefinitionBuilder.setReduce(reduce);
        indexDefinitionBuilder.setStoresStrings(storesStrings);
        indexDefinitionBuilder.setSuggestionsOptions(indexSuggestions);
        indexDefinitionBuilder.setTermVectorsStrings(termVectorsStrings);
        indexDefinitionBuilder.setSpatialIndexesStrings(spatialOptionsStrings);
        indexDefinitionBuilder.setOutputReduceToCollection(outputReduceToCollection);
        indexDefinitionBuilder.setAdditionalSources(getAdditionalSources());

        return indexDefinitionBuilder.toIndexDefinition(conventions);
    }

    /**
     * Gets a value indicating whether this instance is map reduce index definition
     * @return if index is of type: Map/Reduce
     */
    public boolean isMapReduce() {
        return reduce != null;
    }

    /**
     * Generates index name from type name replacing all _ with /
     * @return index name
     */
    public String getIndexName() {
        return getClass().getSimpleName().replaceAll("_", "/");
    }

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
        store.executeIndex(this);
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
        putIndex(store, conventions, database);
    }

    private void putIndex(IDocumentStore store, DocumentConventions conventions, String database) {
        DocumentConventions oldConventions = getConventions();
        try {
            setConventions(ObjectUtils.firstNonNull(conventions, getConventions(), store.getConventions()));

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

    // AbstractGenericIndexCreationTask

    /**
     * Register a field to be indexed
     * @param field Field
     * @param indexing Desired field indexing type
     */
    protected void index(String field, FieldIndexing indexing) {
        indexesStrings.put(field, indexing);
    }

    /**
     * Register a field to be spatially indexed
     * @param field Field
     * @param indexing factory for spatial options
     */
    protected void spatial(String field, Function<SpatialOptionsFactory, SpatialOptions> indexing) {
        spatialOptionsStrings.put(field, indexing.apply(new SpatialOptionsFactory()));
    }

    //TBD protected void Store(Expression<Func<TReduceResult, object>> field, FieldStorage storage)

    protected void storeAllFields(FieldStorage storage) {
        storesStrings.put(Constants.Documents.Indexing.Fields.ALL_FIELDS, storage);
    }

    /**
     * Register a field to be stored
     * @param field Field name
     * @param storage Field storage value to use
     */
    protected void store(String field, FieldStorage storage) {
        storesStrings.put(field, storage);
    }

    /**
     * Register a field to be analyzed
     * @param field Field name
     * @param analyzer analyzer to use
     */
    protected void analyze(String field, String analyzer) {
        analyzersStrings.put(field, analyzer);
    }

    /**
     * Register a field to have term vectors
     * @param field Field name
     * @param termVector TermVector type
     */
    protected void termVector(String field, FieldTermVector termVector) {
        termVectorsStrings.put(field, termVector);
    }

    protected void suggestion(String field) {
        indexSuggestions.add(field);
    }
}
