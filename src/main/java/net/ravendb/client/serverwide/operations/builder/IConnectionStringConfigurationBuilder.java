package net.ravendb.client.serverwide.operations.builder;

import net.ravendb.client.documents.operations.AI.AiConnectionString;
import net.ravendb.client.documents.operations.etl.RavenConnectionString;
import net.ravendb.client.documents.operations.etl.elasticSearch.ElasticSearchConnectionString;
import net.ravendb.client.documents.operations.etl.olap.OlapConnectionString;
import net.ravendb.client.documents.operations.etl.queue.QueueConnectionString;
import net.ravendb.client.documents.operations.etl.snowflake.SnowflakeConnectionString;
import net.ravendb.client.documents.operations.etl.sql.SqlConnectionString;

public interface IConnectionStringConfigurationBuilder {
    IConnectionStringConfigurationBuilder addRavenConnectionString(RavenConnectionString connectionString);
    IConnectionStringConfigurationBuilder addSqlConnectionString(SqlConnectionString connectionString);
    IConnectionStringConfigurationBuilder addOlapConnectionString(OlapConnectionString connectionString);
    IConnectionStringConfigurationBuilder addElasticSearchConnectionString(ElasticSearchConnectionString connectionString);
    IConnectionStringConfigurationBuilder addQueueConnectionString(QueueConnectionString connectionString);
    IConnectionStringConfigurationBuilder addSnowflakeConnectionString(SnowflakeConnectionString connectionString);
    IConnectionStringConfigurationBuilder addAiConnectionString(AiConnectionString connectionString);
}
