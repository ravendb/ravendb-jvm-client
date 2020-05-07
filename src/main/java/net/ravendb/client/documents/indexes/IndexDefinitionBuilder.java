package net.ravendb.client.documents.indexes;

import net.ravendb.client.documents.conventions.DocumentConventions;

public class IndexDefinitionBuilder extends AbstractIndexDefinitionBuilder<IndexDefinition> {
    private String map;

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

        return super.toIndexDefinition(conventions, validateMap);
    }

    @Override
    protected void toIndexDefinition(IndexDefinition indexDefinition, DocumentConventions conventions) {
        if (map == null) {
            return;
        }

        indexDefinition.getMaps().add(map);
    }
}
