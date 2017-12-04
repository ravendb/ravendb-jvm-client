package net.ravendb.client.documents.indexes;

import net.ravendb.client.Constants;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.indexes.spatial.SpatialOptions;
import net.ravendb.client.documents.indexes.spatial.SpatialOptionsFactory;
import net.ravendb.client.documents.operations.indexes.PutIndexesOperation;

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

    private DocumentConventions conventions;
    protected Map<String, String> additionalSources;
    protected IndexPriority priority;
    protected IndexLockMode lockMode;

    protected Map<String, FieldStorage> storesStrings;
    protected Map<String, FieldIndexing> indexesStrings;
    protected Map<String, String> analyzersStrings;
    protected Set<String> indexSuggestions;
    protected Map<String, FieldTermVector> termVectorsStrings;
    protected Map<String, SpatialOptions> spatialOptionsStrings;

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
     */
    public boolean isMapReduce() {
        return reduce != null;
    }

    /**
     * Generates index name from type name replacing all _ with /
     */
    public String getIndexName() {
        return getClass().getSimpleName().replaceAll("_", "/");
    }

    /**
     * Gets the conventions that should be used when index definition is created.
     */
    public DocumentConventions getConventions() {
        return conventions;
    }

    /**
     * Sets the conventions that should be used when index definition is created.
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
     */
    public void execute(IDocumentStore store) {
        store.executeIndex(this);
    }

    /**
     * Executes the index creation against the specified document database using the specified conventions
     */
    public void execute(IDocumentStore store, DocumentConventions conventions) {
        putIndex(store, conventions);
    }

    private void putIndex(IDocumentStore store, DocumentConventions conventions) {
        setConventions(conventions);

        IndexDefinition indexDefinition = createIndexDefinition();
        indexDefinition.setName(getIndexName());

        if (lockMode != null) {
            indexDefinition.setLockMode(lockMode);
        }

        if (priority != null) {
            indexDefinition.setPriority(priority);
        }

        store.maintenance().send(new PutIndexesOperation(indexDefinition));
    }

    // AbstractGenericIndexCreationTask

    /**
     * Register a field to be indexed
     */
    protected void index(String field, FieldIndexing indexing) {
        indexesStrings.put(field, indexing);
    }

    /**
     * Register a field to be spatially indexed
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
     */
    protected void store(String field, FieldStorage storage) {
        storesStrings.put(field, storage);
    }

    /**
     * Register a field to be analyzed
     */
    protected void analyze(String field, String analyzer) {
        analyzersStrings.put(field, analyzer);
    }

    /**
     * Register a field to have term vectors
     */
    protected void termVector(String field, FieldTermVector termVector) {
        termVectorsStrings.put(field, termVector);
    }

    protected void suggestion(String field) {
        indexSuggestions.add(field);
    }
}
