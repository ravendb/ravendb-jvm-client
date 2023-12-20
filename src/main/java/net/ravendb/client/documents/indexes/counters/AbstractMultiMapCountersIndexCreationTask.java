package net.ravendb.client.documents.indexes.counters;

import net.ravendb.client.Constants;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.primitives.SharpEnum;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class AbstractMultiMapCountersIndexCreationTask extends AbstractGenericCountersIndexCreationTask {
    private final List<String> maps = new ArrayList<>();

    protected void addMap(String map) {
        if (map == null) {
            throw new IllegalArgumentException("Map cannot be null");
        }
        maps.add(map);
    }

    @Override
    public CountersIndexDefinition createIndexDefinition() {
        if (conventions == null) {
            conventions = new DocumentConventions();
        }

        CountersIndexDefinitionBuilder indexDefinitionBuilder = new CountersIndexDefinitionBuilder(getIndexName());
        indexDefinitionBuilder.setIndexesStrings(indexesStrings);
        indexDefinitionBuilder.setAnalyzersStrings(analyzersStrings);
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

        indexDefinitionBuilder.setCompoundFieldsStrings(compoundFieldsStrings);
        indexDefinitionBuilder.setArchivedDataProcessingBehavior(archivedDataProcessingBehavior);

        if (searchEngineType != null) {
            indexDefinitionBuilder.getConfiguration().put(Constants.Configuration.Indexes.INDEXING_STATIC_SEARCH_ENGINE_TYPE, SharpEnum.value(searchEngineType));
        }

        CountersIndexDefinition indexDefinition = indexDefinitionBuilder.toIndexDefinition(conventions, false);
        indexDefinition.setMaps(new HashSet<>(maps));

        return indexDefinition;
    }
}
