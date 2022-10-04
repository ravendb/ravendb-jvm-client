package net.ravendb.client.documents.operations.etl.elasticSearch;

import net.ravendb.client.documents.operations.etl.EtlConfiguration;
import net.ravendb.client.documents.operations.etl.EtlType;

import java.util.ArrayList;
import java.util.List;

public class ElasticSearchEtlConfiguration extends EtlConfiguration<ElasticSearchConnectionString> {

    private List<ElasticSearchIndex> elasticIndexes;

    public ElasticSearchEtlConfiguration() {
        elasticIndexes = new ArrayList<>();
    }

    public List<ElasticSearchIndex> getElasticIndexes() {
        return elasticIndexes;
    }

    public void setElasticIndexes(List<ElasticSearchIndex> elasticIndexes) {
        this.elasticIndexes = elasticIndexes;
    }

    public EtlType getEtlType() {
        return EtlType.ELASTIC_SEARCH;
    }
}
