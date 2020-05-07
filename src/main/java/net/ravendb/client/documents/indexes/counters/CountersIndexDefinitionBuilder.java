package net.ravendb.client.documents.indexes.counters;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.indexes.AbstractIndexDefinitionBuilder;

/**
 * This class provides a way to define a strongly typed index on the client.
 */
public class CountersIndexDefinitionBuilder extends AbstractIndexDefinitionBuilder<CountersIndexDefinition> {
    private String map;

    public CountersIndexDefinitionBuilder() {
        super(null);
    }

    public CountersIndexDefinitionBuilder(String indexName) {
        super(indexName);
    }

    public String getMap() {
        return map;
    }

    public void setMap(String map) {
        this.map = map;
    }

    @Override
    protected CountersIndexDefinition newIndexDefinition() {
        return new CountersIndexDefinition();
    }

    @Override
    public CountersIndexDefinition toIndexDefinition(DocumentConventions conventions) {
        return toIndexDefinition(conventions, true);
    }

    @Override
    public CountersIndexDefinition toIndexDefinition(DocumentConventions conventions, boolean validateMap) {
        if (map == null && validateMap) {
            throw new IllegalStateException("Map is required to generate an index, you cannot create an index without a valid Map property (in index " + _indexName + ").");
        }

        return super.toIndexDefinition(conventions, validateMap);
    }

    @Override
    protected void toIndexDefinition(CountersIndexDefinition indexDefinition, DocumentConventions conventions) {
        if (map == null) {
            return;
        }

        indexDefinition.getMaps().add(map);
    }
}
