package net.ravendb.client.documents.indexes;

import net.ravendb.client.Constants;
import net.ravendb.client.documents.indexes.spatial.SpatialOptions;
import net.ravendb.client.documents.indexes.spatial.SpatialOptionsFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Base class for creating indexes
 */
@SuppressWarnings("SameParameterValue")
public abstract class AbstractGenericIndexCreationTask extends AbstractIndexCreationTaskBase {

    protected String map;
    protected String reduce;

    protected final Map<String, FieldStorage> storesStrings;
    protected final Map<String, FieldIndexing> indexesStrings;
    protected final Map<String, String> analyzersStrings;
    protected final Set<String> indexSuggestions;
    protected final Map<String, FieldTermVector> termVectorsStrings;
    protected final Map<String, SpatialOptions> spatialOptionsStrings;

    protected String outputReduceToCollection;

    public AbstractGenericIndexCreationTask() {
        storesStrings = new HashMap<>();
        indexesStrings= new HashMap<>();
        analyzersStrings = new HashMap<>();
        indexSuggestions = new HashSet<>();
        termVectorsStrings = new HashMap<>();
        spatialOptionsStrings = new HashMap<>();
    }

    /**
     * Gets a value indicating whether this instance is map reduce index definition
     * @return if index is of type: Map/Reduce
     */
    public boolean isMapReduce() {
        return reduce != null;
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
