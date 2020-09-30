package net.ravendb.client.documents.indexes.timeSeries;

import net.ravendb.client.documents.conventions.DocumentConventions;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Allow to create indexes with multiple maps
 */
public class AbstractMultiMapTimeSeriesIndexCreationTask extends AbstractGenericTimeSeriesIndexCreationTask {
    private final List<String> maps = new ArrayList<>();

    protected void addMap(String map) {
        if (map == null) {
            throw new IllegalArgumentException("Map cannot be null");
        }
        maps.add(map);
    }

    @Override
    public TimeSeriesIndexDefinition createIndexDefinition() {
        if (conventions == null) {
            conventions = new DocumentConventions();
        }

        TimeSeriesIndexDefinitionBuilder indexDefinitionBuilder = new TimeSeriesIndexDefinitionBuilder(getIndexName());
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
        indexDefinitionBuilder.setConfiguration(getConfiguration());
        indexDefinitionBuilder.setLockMode(getLockMode());
        indexDefinitionBuilder.setPriority(getPriority());

        TimeSeriesIndexDefinition indexDefinition = indexDefinitionBuilder.toIndexDefinition(conventions, false);
        indexDefinition.setMaps(new HashSet<>(maps));

        return indexDefinition;
    }
}
