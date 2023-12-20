package net.ravendb.client.documents.indexes;

import net.ravendb.client.Constants;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.primitives.SharpEnum;

/**
 * Base class for creating indexes
 */
@SuppressWarnings("SameParameterValue")
public abstract class AbstractIndexCreationTask extends AbstractGenericIndexCreationTask {

    protected String map;

    public String getMap() {
        return map;
    }

    public void setMap(String map) {
        this.map = map;
    }

    @Override
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
        indexDefinitionBuilder.setPatternForOutputReduceToCollectionReferences(patternForOutputReduceToCollectionReferences);
        indexDefinitionBuilder.setPatternReferencesCollectionName(patternReferencesCollectionName);
        indexDefinitionBuilder.setAdditionalSources(getAdditionalSources());
        indexDefinitionBuilder.setAdditionalAssemblies(getAdditionalAssemblies());
        indexDefinitionBuilder.setConfiguration(getConfiguration());
        indexDefinitionBuilder.setLockMode(lockMode);
        indexDefinitionBuilder.setPriority(priority);
        indexDefinitionBuilder.setState(state);
        indexDefinitionBuilder.setDeploymentMode(deploymentMode);
        indexDefinitionBuilder.setCompoundFieldsStrings(compoundFieldsStrings);
        indexDefinitionBuilder.setArchivedDataProcessingBehavior(archivedDataProcessingBehavior);

        if (searchEngineType != null) {
            indexDefinitionBuilder.getConfiguration().put(Constants.Configuration.Indexes.INDEXING_STATIC_SEARCH_ENGINE_TYPE, SharpEnum.value(searchEngineType));
        }

        return indexDefinitionBuilder.toIndexDefinition(conventions);
    }
}
