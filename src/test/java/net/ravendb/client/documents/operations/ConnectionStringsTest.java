package net.ravendb.client.documents.operations;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.operations.backups.FtpSettings;
import net.ravendb.client.documents.operations.connectionStrings.*;
import net.ravendb.client.documents.operations.etl.elasticSearch.ElasticSearchConnectionString;
import net.ravendb.client.documents.operations.etl.olap.OlapConnectionString;
import net.ravendb.client.documents.operations.etl.queue.KafkaConnectionSettings;
import net.ravendb.client.documents.operations.etl.queue.QueueBrokerType;
import net.ravendb.client.documents.operations.etl.queue.QueueConnectionString;
import net.ravendb.client.documents.operations.etl.queue.RabbitMqConnectionSettings;
import net.ravendb.client.documents.operations.etl.sql.SqlConnectionString;
import net.ravendb.client.serverwide.ConnectionStringType;
import net.ravendb.client.documents.operations.etl.RavenConnectionString;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ConnectionStringsTest extends RemoteTestBase {

    @Test
    public void canCreateGetAndDeleteConnectionStrings() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {

            RavenConnectionString ravenConnectionString1 = new RavenConnectionString();
            ravenConnectionString1.setDatabase("db1");
            ravenConnectionString1.setTopologyDiscoveryUrls(new String[] { "http://localhost:8080" });
            ravenConnectionString1.setName("r1");

            SqlConnectionString sqlConnectionString1 = new SqlConnectionString();
            sqlConnectionString1.setFactoryName("test");
            sqlConnectionString1.setConnectionString("test");
            sqlConnectionString1.setName("s1");

            ElasticSearchConnectionString elasticSearchConnectionString = new ElasticSearchConnectionString();
            elasticSearchConnectionString.setName("e1");
            elasticSearchConnectionString.setNodes(new String[] { "http://127.0.0.1:8080" });

            QueueConnectionString kafkaConnectionString = new QueueConnectionString();
            kafkaConnectionString.setName("k1");
            kafkaConnectionString.setBrokerType(QueueBrokerType.KAFKA);
            kafkaConnectionString.setKafkaConnectionSettings(new KafkaConnectionSettings());
            kafkaConnectionString.getKafkaConnectionSettings().setBootstrapServers("localhost:9092");

            QueueConnectionString rabbitConnectionString = new QueueConnectionString();
            rabbitConnectionString.setName("r1");
            rabbitConnectionString.setBrokerType(QueueBrokerType.RABBIT_MQ);
            rabbitConnectionString.setRabbitMqConnectionSettings(new RabbitMqConnectionSettings());
            rabbitConnectionString.getRabbitMqConnectionSettings().setConnectionString("localhost:888");

            OlapConnectionString olapConnectionString = new OlapConnectionString();
            olapConnectionString.setName("o1");
            olapConnectionString.setFtpSettings(new FtpSettings());
            olapConnectionString.getFtpSettings().setUrl("localhost:9090");

            PutConnectionStringResult putResult = store.maintenance().send(new PutConnectionStringOperation<>(ravenConnectionString1));
            assertThat(putResult.getRaftCommandIndex())
                    .isPositive();

            putResult = store.maintenance().send(new PutConnectionStringOperation<>(sqlConnectionString1));
            assertThat(putResult.getRaftCommandIndex())
                    .isPositive();

            putResult = store.maintenance().send(new PutConnectionStringOperation<>(elasticSearchConnectionString));
            assertThat(putResult.getRaftCommandIndex())
                    .isPositive();

            putResult = store.maintenance().send(new PutConnectionStringOperation<>(kafkaConnectionString));
            assertThat(putResult.getRaftCommandIndex())
                    .isPositive();

            putResult = store.maintenance().send(new PutConnectionStringOperation<>(rabbitConnectionString));
            assertThat(putResult.getRaftCommandIndex())
                    .isPositive();

            putResult = store.maintenance().send(new PutConnectionStringOperation<>(olapConnectionString));
            assertThat(putResult.getRaftCommandIndex())
                    .isPositive();

            GetConnectionStringsResult connectionStrings = store.maintenance().send(new GetConnectionStringsOperation());
            assertThat(connectionStrings.getRavenConnectionStrings())
                    .containsKey("r1")
                    .hasSize(1);

            assertThat(connectionStrings.getSqlConnectionStrings())
                    .containsKey("s1")
                    .hasSize(1);

            assertThat(connectionStrings.getElasticSearchConnectionStrings())
                    .containsKey("e1")
                    .hasSize(1);

            assertThat(connectionStrings.getOlapConnectionStrings())
                    .containsKey("o1")
                    .hasSize(1);

            assertThat(connectionStrings.getQueueConnectionStrings())
                    .containsKey("k1")
                    .containsKey("r1")
                    .hasSize(2);

            GetConnectionStringsResult ravenOnly = store.maintenance().send(new GetConnectionStringsOperation("r1", ConnectionStringType.RAVEN));
            assertThat(ravenOnly.getRavenConnectionStrings())
                    .containsKey("r1")
                    .hasSize(1);
            assertThat(ravenOnly.getSqlConnectionStrings())
                    .isEmpty();

            GetConnectionStringsResult sqlOnly = store.maintenance().send(new GetConnectionStringsOperation("s1", ConnectionStringType.SQL));
            assertThat(sqlOnly.getRavenConnectionStrings())
                    .isEmpty();
            assertThat(sqlOnly.getSqlConnectionStrings())
                    .containsKey("s1")
                    .hasSize(1);

            GetConnectionStringsResult elasticOnly = store.maintenance().send(new GetConnectionStringsOperation("e1", ConnectionStringType.ELASTIC_SEARCH));
            assertThat(elasticOnly.getRavenConnectionStrings())
                    .isEmpty();
            assertThat(elasticOnly.getElasticSearchConnectionStrings())
                    .containsKey("e1")
                    .hasSize(1);

            GetConnectionStringsResult olapOnly = store.maintenance().send(new GetConnectionStringsOperation("o1", ConnectionStringType.OLAP));
            assertThat(olapOnly.getRavenConnectionStrings())
                    .isEmpty();
            assertThat(olapOnly.getOlapConnectionStrings())
                    .containsKey("o1")
                    .hasSize(1);

            GetConnectionStringsResult rabbitOnly = store.maintenance().send(new GetConnectionStringsOperation("r1", ConnectionStringType.QUEUE));
            assertThat(rabbitOnly.getRavenConnectionStrings())
                    .isEmpty();
            assertThat(rabbitOnly.getQueueConnectionStrings())
                    .containsKey("r1")
                    .hasSize(1);

            GetConnectionStringsResult kafkaOnly = store.maintenance().send(new GetConnectionStringsOperation("k1", ConnectionStringType.QUEUE));
            assertThat(kafkaOnly.getRavenConnectionStrings())
                    .isEmpty();
            assertThat(kafkaOnly.getQueueConnectionStrings())
                    .containsKey("k1")
                    .hasSize(1);


            RemoveConnectionStringResult removeResult = store.maintenance().send(new RemoveConnectionStringOperation<>(sqlOnly.getSqlConnectionStrings().values().stream().findFirst().get()));
            assertThat(removeResult.getRaftCommandIndex())
                    .isPositive();

            GetConnectionStringsResult afterDelete = store.maintenance().send(new GetConnectionStringsOperation("s1", ConnectionStringType.SQL));
            assertThat(afterDelete.getRavenConnectionStrings())
                    .isEmpty();
            assertThat(afterDelete.getSqlConnectionStrings())
                    .isEmpty();
        }
    }
}
