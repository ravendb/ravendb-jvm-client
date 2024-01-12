package net.ravendb.client.documents.indexes;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.indexes.spatial.SpatialOptions;
import net.ravendb.client.exceptions.documents.compilation.IndexCompilationException;
import org.apache.commons.lang3.ObjectUtils;

import java.util.*;
import java.util.function.BiConsumer;

public abstract class AbstractIndexDefinitionBuilder<TIndexDefinition extends IndexDefinition> {

    protected final String _indexName;

    private String reduce;

    private Map<String, FieldStorage> storesStrings;
    private Map<String, FieldIndexing> indexesStrings;
    private Map<String, String> analyzersStrings;
    private Set<String> suggestionsOptions;
    private Map<String, FieldTermVector> termVectorsStrings;
    private Map<String, SpatialOptions> spatialIndexesStrings;
    private IndexLockMode lockMode;
    private IndexPriority priority;
    private IndexState state;
    private List<String[]> compoundFieldsStrings;
    private IndexDeploymentMode deploymentMode;
    private String outputReduceToCollection;
    private String patternForOutputReduceToCollectionReferences;
    private String patternReferencesCollectionName;
    private Map<String, String> additionalSources;
    private Set<AdditionalAssembly> additionalAssemblies;
    private IndexConfiguration configuration;

    protected AbstractIndexDefinitionBuilder(String indexName) {
        _indexName = ObjectUtils.firstNonNull(indexName, getClass().getSimpleName());
        if (_indexName.length() > 256) {
            throw new IllegalArgumentException("The index name is limited to 256 characters, but was: " + _indexName);
        }
        storesStrings = new HashMap<>();
        indexesStrings = new HashMap<>();
        suggestionsOptions = new HashSet<>();
        analyzersStrings = new HashMap<>();
        termVectorsStrings = new HashMap<>();
        spatialIndexesStrings = new HashMap<>();
        configuration = new IndexConfiguration();
    }

    public TIndexDefinition toIndexDefinition(DocumentConventions conventions) {
        return toIndexDefinition(conventions, true);
    }

    public TIndexDefinition toIndexDefinition(DocumentConventions conventions, boolean validateMap) {
        try {
            TIndexDefinition indexDefinition = newIndexDefinition();
            indexDefinition.setName(_indexName);
            indexDefinition.setReduce(reduce);
            indexDefinition.setLockMode(lockMode);
            indexDefinition.setPriority(priority);
            indexDefinition.setDeploymentMode(deploymentMode);
            indexDefinition.setState(state);
            indexDefinition.setOutputReduceToCollection(outputReduceToCollection);
            indexDefinition.setPatternForOutputReduceToCollectionReferences(patternForOutputReduceToCollectionReferences);
            indexDefinition.setPatternReferencesCollectionName(patternReferencesCollectionName);
            indexDefinition.setCompoundFields(compoundFieldsStrings);

            Map<String, Boolean> suggestions = new HashMap<>();
            for (String suggestionsOption : suggestionsOptions) {
                suggestions.put(suggestionsOption, true);
            }

            applyValues(indexDefinition, indexesStrings, (options, value) -> options.setIndexing(value));
            applyValues(indexDefinition, storesStrings, (options, value) -> options.setStorage(value));
            applyValues(indexDefinition, analyzersStrings, (options, value) -> options.setAnalyzer(value));
            applyValues(indexDefinition, termVectorsStrings, (options, value) -> options.setTermVector(value));
            applyValues(indexDefinition, spatialIndexesStrings, (options, value) -> options.setSpatial(value));
            applyValues(indexDefinition, suggestions, (options, value) -> options.setSuggestions(value));

            indexDefinition.setAdditionalSources(additionalSources);
            indexDefinition.setAdditionalAssemblies(additionalAssemblies);
            indexDefinition.setConfiguration(configuration);

            toIndexDefinition(indexDefinition, conventions);

            return indexDefinition;
        } catch (Exception e) {
            throw new IndexCompilationException("Failed to create index " + _indexName, e);
        }
    }

    protected abstract TIndexDefinition newIndexDefinition();

    protected abstract void toIndexDefinition(TIndexDefinition indexDefinition, DocumentConventions conventions);

    private <T> void applyValues(IndexDefinition indexDefinition, Map<String, T> values, BiConsumer<IndexFieldOptions, T> action) {
        for (Map.Entry<String, T> kvp : values.entrySet()) {
            IndexFieldOptions field = indexDefinition.getFields().computeIfAbsent(kvp.getKey(), x -> new IndexFieldOptions());
            action.accept(field, kvp.getValue());
        }
    }

    public String getReduce() {
        return reduce;
    }

    public void setReduce(String reduce) {
        this.reduce = reduce;
    }

    public Map<String, FieldStorage> getStoresStrings() {
        return storesStrings;
    }

    public void setStoresStrings(Map<String, FieldStorage> storesStrings) {
        this.storesStrings = storesStrings;
    }

    public Map<String, FieldIndexing> getIndexesStrings() {
        return indexesStrings;
    }

    public void setIndexesStrings(Map<String, FieldIndexing> indexesStrings) {
        this.indexesStrings = indexesStrings;
    }

    public Map<String, String> getAnalyzersStrings() {
        return analyzersStrings;
    }

    public void setAnalyzersStrings(Map<String, String> analyzersStrings) {
        this.analyzersStrings = analyzersStrings;
    }

    public Set<String> getSuggestionsOptions() {
        return suggestionsOptions;
    }

    public void setSuggestionsOptions(Set<String> suggestionsOptions) {
        this.suggestionsOptions = suggestionsOptions;
    }

    public Map<String, FieldTermVector> getTermVectorsStrings() {
        return termVectorsStrings;
    }

    public void setTermVectorsStrings(Map<String, FieldTermVector> termVectorsStrings) {
        this.termVectorsStrings = termVectorsStrings;
    }

    public Map<String, SpatialOptions> getSpatialIndexesStrings() {
        return spatialIndexesStrings;
    }

    public void setSpatialIndexesStrings(Map<String, SpatialOptions> spatialIndexesStrings) {
        this.spatialIndexesStrings = spatialIndexesStrings;
    }

    public IndexLockMode getLockMode() {
        return lockMode;
    }

    public void setLockMode(IndexLockMode lockMode) {
        this.lockMode = lockMode;
    }

    public IndexPriority getPriority() {
        return priority;
    }

    public void setPriority(IndexPriority priority) {
        this.priority = priority;
    }

    public IndexState getState() {
        return state;
    }

    public void setState(IndexState state) {
        this.state = state;
    }

    public List<String[]> getCompoundFieldsStrings() {
        return compoundFieldsStrings;
    }

    public void setCompoundFieldsStrings(List<String[]> compoundFieldsStrings) {
        this.compoundFieldsStrings = compoundFieldsStrings;
    }

    public IndexDeploymentMode getDeploymentMode() {
        return deploymentMode;
    }

    public void setDeploymentMode(IndexDeploymentMode deploymentMode) {
        this.deploymentMode = deploymentMode;
    }

    public String getOutputReduceToCollection() {
        return outputReduceToCollection;
    }

    public void setOutputReduceToCollection(String outputReduceToCollection) {
        this.outputReduceToCollection = outputReduceToCollection;
    }

    public String getPatternForOutputReduceToCollectionReferences() {
        return patternForOutputReduceToCollectionReferences;
    }

    public void setPatternForOutputReduceToCollectionReferences(String patternForOutputReduceToCollectionReferences) {
        this.patternForOutputReduceToCollectionReferences = patternForOutputReduceToCollectionReferences;
    }

    public String getPatternReferencesCollectionName() {
        return patternReferencesCollectionName;
    }

    public void setPatternReferencesCollectionName(String patternReferencesCollectionName) {
        this.patternReferencesCollectionName = patternReferencesCollectionName;
    }

    public Map<String, String> getAdditionalSources() {
        return additionalSources;
    }

    public void setAdditionalSources(Map<String, String> additionalSources) {
        this.additionalSources = additionalSources;
    }

    public Set<AdditionalAssembly> getAdditionalAssemblies() {
        return additionalAssemblies;
    }

    public void setAdditionalAssemblies(Set<AdditionalAssembly> additionalAssemblies) {
        this.additionalAssemblies = additionalAssemblies;
    }

    public IndexConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(IndexConfiguration configuration) {
        this.configuration = configuration;
    }
}
