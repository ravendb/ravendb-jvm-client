package net.ravendb.client.test.server.etl;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.operations.connectionStrings.GetConnectionStringsOperation;
import net.ravendb.client.documents.operations.connectionStrings.GetConnectionStringsResult;
import net.ravendb.client.documents.operations.connectionStrings.PutConnectionStringOperation;
import net.ravendb.client.documents.operations.etl.elasticSearch.ElasticSearchConnectionString;
import net.ravendb.client.documents.operations.etl.queue.*;
import net.ravendb.client.documents.operations.etl.sql.SqlEtlConfiguration;
import net.ravendb.client.infrastructure.EnableOnServer;
import net.ravendb.client.serverwide.ConnectionStringType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class EtlConnectionStringCredentialsTest extends RemoteTestBase {

    @Test
    public void azureQueueStorageEntraIdSurvivesRoundTrip() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            EntraId entraId = new EntraId();
            entraId.setStorageAccountName("account1");
            entraId.setTenantId("tenant1");
            entraId.setClientId("client1");
            entraId.setClientSecret("secret1");

            AzureQueueStorageConnectionSettings settings = new AzureQueueStorageConnectionSettings();
            settings.setEntraId(entraId);

            AzureQueueStorageConnectionSettings result = putAndGet(store, "aqs-entra", QueueBrokerType.AZURE_QUEUE_STORAGE, settings, null)
                    .getAzureQueueStorageConnectionSettings();

            assertThat(result.getConnectionString()).isNull();
            assertThat(result.getPasswordless()).isNull();
            assertThat(result.getEntraId()).isNotNull();
            assertThat(result.getEntraId().getStorageAccountName()).isEqualTo("account1");
            assertThat(result.getEntraId().getTenantId()).isEqualTo("tenant1");
            assertThat(result.getEntraId().getClientId()).isEqualTo("client1");
            assertThat(result.getEntraId().getClientSecret()).isEqualTo("secret1");
        }
    }

    @Test
    public void azureQueueStoragePasswordlessSurvivesRoundTrip() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            Passwordless passwordless = new Passwordless();
            passwordless.setStorageAccountName("account2");

            AzureQueueStorageConnectionSettings settings = new AzureQueueStorageConnectionSettings();
            settings.setPasswordless(passwordless);

            AzureQueueStorageConnectionSettings result = putAndGet(store, "aqs-passwordless", QueueBrokerType.AZURE_QUEUE_STORAGE, settings, null)
                    .getAzureQueueStorageConnectionSettings();

            assertThat(result.getEntraId()).isNull();
            assertThat(result.getPasswordless()).isNotNull();
            assertThat(result.getPasswordless().getStorageAccountName()).isEqualTo("account2");
        }
    }

    @EnableOnServer(thresholdVersion = "7.1")
    @Test
    public void amazonSqsBasicCredentialsSurviveRoundTrip() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            AmazonSqsCredentials credentials = new AmazonSqsCredentials();
            credentials.setAccessKey("access1");
            credentials.setSecretKey("secret1");
            credentials.setRegionName("eu-west-1");

            AmazonSqsConnectionSettings settings = new AmazonSqsConnectionSettings();
            settings.setBasic(credentials);

            AmazonSqsConnectionSettings result = putAndGet(store, "sqs-basic", QueueBrokerType.AMAZON_SQS, null, settings)
                    .getAmazonSqsConnectionSettings();

            assertThat(result.isPasswordless()).isFalse();
            assertThat(result.getBasic()).isNotNull();
            assertThat(result.getBasic().getAccessKey()).isEqualTo("access1");
            assertThat(result.getBasic().getSecretKey()).isEqualTo("secret1");
            assertThat(result.getBasic().getRegionName()).isEqualTo("eu-west-1");
        }
    }

    @EnableOnServer(thresholdVersion = "7.1")
    @Test
    public void amazonSqsPasswordlessSurvivesRoundTrip() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            AmazonSqsConnectionSettings settings = new AmazonSqsConnectionSettings();
            settings.setPasswordless(true);

            AmazonSqsConnectionSettings result = putAndGet(store, "sqs-passwordless", QueueBrokerType.AMAZON_SQS, null, settings)
                    .getAmazonSqsConnectionSettings();

            assertThat(result.isPasswordless()).isTrue();
            assertThat(result.getBasic()).isNull();
        }
    }

    @Test
    public void elasticSearchEncodedApiKeySurvivesRoundTrip() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            ElasticSearchConnectionString connectionString = new ElasticSearchConnectionString();
            connectionString.setName("es-api-key");
            connectionString.setNodes(new String[] { "http://127.0.0.1:9200" });

            ElasticSearchConnectionString.ApiKeyAuthentication apiKey = new ElasticSearchConnectionString.ApiKeyAuthentication();
            apiKey.setApiKeyId("id1");
            apiKey.setApiKey("key1");
            apiKey.setEncodedApiKey("ZW5jb2RlZDE=");

            ElasticSearchConnectionString.Authentication authentication = new ElasticSearchConnectionString.Authentication();
            authentication.setApiKey(apiKey);
            connectionString.setAuthentication(authentication);

            store.maintenance().send(new PutConnectionStringOperation<>(connectionString));

            GetConnectionStringsResult result = store.maintenance().send(
                    new GetConnectionStringsOperation("es-api-key", ConnectionStringType.ELASTIC_SEARCH));

            ElasticSearchConnectionString.ApiKeyAuthentication loaded = result.getElasticSearchConnectionStrings()
                    .get("es-api-key").getAuthentication().getApiKey();

            assertThat(loaded.getApiKeyId()).isEqualTo("id1");
            assertThat(loaded.getApiKey()).isEqualTo("key1");
            assertThat(loaded.getEncodedApiKey()).isEqualTo("ZW5jb2RlZDE=");
        }
    }

    @Test
    public void sqlEtlConfigurationHasCSharpDefaults() {
        SqlEtlConfiguration configuration = new SqlEtlConfiguration();
        assertThat(configuration.isParameterizeDeletes()).isTrue();
        assertThat(configuration.isQuoteTables()).isTrue();
        assertThat(configuration.isForceQueryRecompile()).isFalse();
        assertThat(configuration.getSqlTables()).isNotNull().isEmpty();
    }

    private static QueueConnectionString putAndGet(IDocumentStore store, String name, QueueBrokerType brokerType,
                                                   AzureQueueStorageConnectionSettings azureQueueStorage,
                                                   AmazonSqsConnectionSettings amazonSqs) {
        QueueConnectionString connectionString = new QueueConnectionString();
        connectionString.setName(name);
        connectionString.setBrokerType(brokerType);
        connectionString.setAzureQueueStorageConnectionSettings(azureQueueStorage);
        connectionString.setAmazonSqsConnectionSettings(amazonSqs);

        store.maintenance().send(new PutConnectionStringOperation<>(connectionString));

        GetConnectionStringsResult result = store.maintenance().send(
                new GetConnectionStringsOperation(name, ConnectionStringType.QUEUE));

        QueueConnectionString loaded = result.getQueueConnectionStrings().get(name);
        assertThat(loaded).isNotNull();
        assertThat(loaded.getBrokerType()).isEqualTo(brokerType);
        return loaded;
    }
}
