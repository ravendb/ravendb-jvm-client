package net.ravendb.client.documents.indexes;

import net.ravendb.client.documents.conventions.DocumentConventions;

/**
 * Base class for creating indexes
 */
@SuppressWarnings("SameParameterValue")
public abstract class AbstractIndexCreationTask extends AbstractGenericIndexCreationTask {

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
}
