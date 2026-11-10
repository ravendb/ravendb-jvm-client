package net.ravendb.client.documents.indexes.counters;

import net.ravendb.client.Constants;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.primitives.SharpEnum;

public class AbstractCountersIndexCreationTask extends AbstractGenericCountersIndexCreationTask {
    protected String map;

    public String getMap() {
        return map;
    }

    public void setMap(String map) {
        this.map = map;
    }

    @Override
    public CountersIndexDefinition createIndexDefinition() {
        if (conventions == null) {
            conventions = new DocumentConventions();
        }

        CountersIndexDefinitionBuilder indexDefinitionBuilder = new CountersIndexDefinitionBuilder(getIndexName());
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
        indexDefinitionBuilder.setLockMode(getLockMode());
        indexDefinitionBuilder.setPriority(getPriority());
        indexDefinitionBuilder.setState(getState());
        indexDefinitionBuilder.setDeploymentMode(getDeploymentMode());

        if (searchEngineType != null) {
            indexDefinitionBuilder.getConfiguration().put(Constants.Configuration.Indexes.INDEXING_STATIC_SEARCH_ENGINE_TYPE, SharpEnum.value(searchEngineType));
        }

        return indexDefinitionBuilder.toIndexDefinition(conventions);
    }
}
