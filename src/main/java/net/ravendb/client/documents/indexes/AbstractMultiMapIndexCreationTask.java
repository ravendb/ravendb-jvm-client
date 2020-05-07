package net.ravendb.client.documents.indexes;

import net.ravendb.client.documents.conventions.DocumentConventions;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class AbstractMultiMapIndexCreationTask extends AbstractGenericIndexCreationTask {

    private final List<String> maps = new ArrayList<>();

    protected void addMap(String map) {
        if (map == null) {
            throw new IllegalArgumentException("Map cannot be null");
        }
        maps.add(map);
    }

    @Override
    public IndexDefinition createIndexDefinition() {
        if (conventions == null) {
            conventions = new DocumentConventions();
        }

        IndexDefinitionBuilder indexDefinitionBuilder = new IndexDefinitionBuilder(getIndexName());
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

        IndexDefinition indexDefinition = indexDefinitionBuilder.toIndexDefinition(conventions, false);
        indexDefinition.setMaps(new HashSet<>(maps));

        return indexDefinition;
    }
}
