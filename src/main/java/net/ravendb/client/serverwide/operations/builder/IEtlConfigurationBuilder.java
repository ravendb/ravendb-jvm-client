package net.ravendb.client.serverwide.operations.builder;

import net.ravendb.client.documents.operations.etl.RavenEtlConfiguration;
import net.ravendb.client.documents.operations.etl.elasticSearch.ElasticSearchEtlConfiguration;
import net.ravendb.client.documents.operations.etl.olap.OlapEtlConfiguration;
import net.ravendb.client.documents.operations.etl.queue.QueueEtlConfiguration;
import net.ravendb.client.documents.operations.etl.sql.SqlEtlConfiguration;

public interface IEtlConfigurationBuilder {
    IEtlConfigurationBuilder addRavenEtl(RavenEtlConfiguration configuration);
    IEtlConfigurationBuilder addSqlEtl(SqlEtlConfiguration configuration);
    IEtlConfigurationBuilder addElasticSearchEtl(ElasticSearchEtlConfiguration configuration);
    IEtlConfigurationBuilder addOlapEtl(OlapEtlConfiguration configuration);
    IEtlConfigurationBuilder addQueueEtl(QueueEtlConfiguration configuration);
}
