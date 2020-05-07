package net.ravendb.client.documents.indexes.counters;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.indexes.timeSeries.TimeSeriesIndexDefinition;
import net.ravendb.client.documents.indexes.timeSeries.TimeSeriesIndexDefinitionBuilder;

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
        indexDefinitionBuilder.setConfiguration(getConfiguration());

        CountersIndexDefinition indexDefinition = indexDefinitionBuilder.toIndexDefinition(conventions, false);
        indexDefinition.setMaps(new HashSet<>(maps));

        return indexDefinition;
    }
}
