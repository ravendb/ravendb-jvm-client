package net.ravendb.client.documents.operations.ongoingTasks;

import net.ravendb.client.documents.operations.etl.elasticSearch.ElasticSearchEtlConfiguration;

public class OngoingTaskElasticSearchEtlDetails extends OngoingTask {

    private ElasticSearchEtlConfiguration configuration;

    public OngoingTaskElasticSearchEtlDetails() {
        this.setTaskType(OngoingTaskType.ELASTIC_SEARCH_ETL);
    }

    public ElasticSearchEtlConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(ElasticSearchEtlConfiguration configuration) {
        this.configuration = configuration;
    }
}
