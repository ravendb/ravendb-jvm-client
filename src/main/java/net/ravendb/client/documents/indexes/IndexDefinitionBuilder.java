package net.ravendb.client.documents.indexes;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.dataArchival.ArchivedDataProcessingBehavior;

public class IndexDefinitionBuilder extends AbstractIndexDefinitionBuilder<IndexDefinition> {
    private String map;
    private ArchivedDataProcessingBehavior archivedDataProcessingBehavior;

    public IndexDefinitionBuilder() {
        super(null);
    }

    public IndexDefinitionBuilder(String indexName) {
        super(indexName);
    }

    public String getMap() {
        return map;
    }

    public void setMap(String map) {
        this.map = map;
    }

    public ArchivedDataProcessingBehavior getArchivedDataProcessingBehavior() {
        return archivedDataProcessingBehavior;
    }

    public void setArchivedDataProcessingBehavior(ArchivedDataProcessingBehavior archivedDataProcessingBehavior) {
        this.archivedDataProcessingBehavior = archivedDataProcessingBehavior;
    }

    @Override
    protected IndexDefinition newIndexDefinition() {
        return new IndexDefinition();
    }

    @Override
    public IndexDefinition toIndexDefinition(DocumentConventions conventions) {
        return toIndexDefinition(conventions, true);
    }

    @Override
    public IndexDefinition toIndexDefinition(DocumentConventions conventions, boolean validateMap) {
        if (map == null && validateMap) {
            throw new IllegalStateException("Map is required to generate an index, you cannot create an index without a valid Map property (in index " + _indexName + ").");
        }

        IndexDefinition indexDefinition = super.toIndexDefinition(conventions, validateMap);
        indexDefinition.setArchivedDataProcessingBehavior(archivedDataProcessingBehavior);
        return indexDefinition;
    }

    @Override
    protected void toIndexDefinition(IndexDefinition indexDefinition, DocumentConventions conventions) {
        if (map == null) {
            return;
        }

        indexDefinition.getMaps().add(map);
    }
}
