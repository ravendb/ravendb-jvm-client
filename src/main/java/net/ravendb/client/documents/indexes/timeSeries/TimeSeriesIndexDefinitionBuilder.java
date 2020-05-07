package net.ravendb.client.documents.indexes.timeSeries;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.indexes.AbstractIndexDefinitionBuilder;

/**
 * This class provides a way to define a strongly typed index on the client.
 */
public class TimeSeriesIndexDefinitionBuilder extends AbstractIndexDefinitionBuilder<TimeSeriesIndexDefinition> {

    private String map;

    public TimeSeriesIndexDefinitionBuilder() {
        super(null);
    }

    public TimeSeriesIndexDefinitionBuilder(String indexName) {
        super(indexName);
    }

    public String getMap() {
        return map;
    }

    public void setMap(String map) {
        this.map = map;
    }

    @Override
    protected TimeSeriesIndexDefinition newIndexDefinition() {
        return new TimeSeriesIndexDefinition();
    }

    @Override
    public TimeSeriesIndexDefinition toIndexDefinition(DocumentConventions conventions) {
        return toIndexDefinition(conventions, true);
    }

    @Override
    public TimeSeriesIndexDefinition toIndexDefinition(DocumentConventions conventions, boolean validateMap) {
        if (map == null && validateMap) {
            throw new IllegalStateException("Map is required to generate an index, you cannot create an index without a valid Map property (in index " + _indexName + ").");
        }

        return super.toIndexDefinition(conventions, validateMap);
    }

    @Override
    protected void toIndexDefinition(TimeSeriesIndexDefinition indexDefinition, DocumentConventions conventions) {
        if (map == null) {
            return;
        }

        indexDefinition.getMaps().add(map);
    }

}
