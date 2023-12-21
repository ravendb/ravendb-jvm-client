package net.ravendb.client.documents.operations.ongoingTasks;

import net.ravendb.client.documents.operations.etl.elasticSearch.ElasticSearchEtlConfiguration;

public class OngoingTaskElasticSearchEtl extends OngoingTask {

    private String connectionStringName;
    private String[] nodesUrls;
    private ElasticSearchEtlConfiguration configuration;

    public OngoingTaskElasticSearchEtl() {
        this.setTaskType(OngoingTaskType.ELASTIC_SEARCH_ETL);
    }

    public String getConnectionStringName() {
        return connectionStringName;
    }

    public void setConnectionStringName(String connectionStringName) {
        this.connectionStringName = connectionStringName;
    }

    public String[] getNodesUrls() {
        return nodesUrls;
    }

    public void setNodesUrls(String[] nodesUrls) {
        this.nodesUrls = nodesUrls;
    }

    public ElasticSearchEtlConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(ElasticSearchEtlConfiguration configuration) {
        this.configuration = configuration;
    }
}
