package net.ravendb.client.documents.operations.etl.elasticSearch;

import net.ravendb.client.documents.operations.etl.EtlConfiguration;
import net.ravendb.client.documents.operations.etl.EtlType;

import java.util.ArrayList;
import java.util.List;

public class ElasticSearchEtlConfiguration extends EtlConfiguration<ElasticSearchConnectionString> {

    private List<ElasticSearchIndex> elasticIndexes;
    private String destination;

    public ElasticSearchEtlConfiguration() {
        elasticIndexes = new ArrayList<>();
    }

    public List<ElasticSearchIndex> getElasticIndexes() {
        return elasticIndexes;
    }

    public void setElasticIndexes(List<ElasticSearchIndex> elasticIndexes) {
        this.elasticIndexes = elasticIndexes;
    }

    @Override
    public String getDestination() {
        if (destination == null) {
            destination = "@" + String.join(",", this.getConnection().getNodes());
        }
        return destination;
    }

    @Override
    public String getDefaultTaskName() {
        return "ElasticSearch ETL to " + this.getConnectionStringName();
    }

    @Override
    public boolean usingEncryptedCommunicationChannel() {
        for (String url : this.getConnection().getNodes()) {
            if (url.regionMatches(true, 0, "http:", 0, "http:".length())) {
                return false;
            }
        }
        return true;
    }

    public EtlType getEtlType() {
        return EtlType.ELASTIC_SEARCH;
    }
}
