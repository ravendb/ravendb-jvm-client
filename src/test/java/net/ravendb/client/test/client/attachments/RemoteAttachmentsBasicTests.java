package net.ravendb.client.test.client.attachments;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.attachments.*;
import net.ravendb.client.documents.operations.attachments.remote.ConfigureRemoteAttachmentsOperation;
import net.ravendb.client.documents.operations.attachments.remote.GetRemoteAttachmentsConfigurationOperation;
import net.ravendb.client.infrastructure.DisabledOnPullRequest;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RemoteAttachmentsBasicTests extends RemoteTestBase {

    @DisabledOnPullRequest
    @Test
    public void canPutAndGetRemoteAttachmentsConfigurationWithCaseInsensitiveIdentifier() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {

            RemoteAttachmentsConfiguration configToSend = new RemoteAttachmentsConfiguration();
            Map<String, RemoteAttachmentsDestinationConfiguration> destinations = new HashMap<>();

            RemoteAttachmentsDestinationConfiguration destConfig = new RemoteAttachmentsDestinationConfiguration();
            RemoteAttachmentsS3Settings s3 = new RemoteAttachmentsS3Settings();
            s3.setBucketName("testS3Bucket-Users");
            destConfig.setS3Settings(s3);
            destConfig.setDisabled(false);

            destinations.put("S3-uSeRs", destConfig);
            configToSend.setDestinations(destinations);
            configToSend.setMaxItemsToProcess(1L);

            store.maintenance().send(new ConfigureRemoteAttachmentsOperation(configToSend));

            RemoteAttachmentsConfiguration config =
                    store.maintenance().send(new GetRemoteAttachmentsConfigurationOperation());

            assertThat(config.getDestinations().size())
                    .isEqualTo(1);

            Map.Entry<String, RemoteAttachmentsDestinationConfiguration> destination =
                    config.getDestinations().entrySet().iterator().next();

            assertThat(destination).isNotNull();
            assertThat(destination.getKey())
                    .isEqualTo("S3-uSeRs");

            assertThat(destination.getValue().getS3Settings().getBucketName())
                    .isEqualTo("testS3Bucket-Users");

            assertThat(destination.getValue().isDisabled())
                    .isFalse();

            assertThat(config.getCheckFrequencyInSec())
                    .isNull();
        }
    }

    @DisabledOnPullRequest
    @Test
    public void canPutAndGetRemoteAttachmentsConfigurationWithDefaultRemoteFrequencyInSec() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {

            RemoteAttachmentsConfiguration configToSend = new RemoteAttachmentsConfiguration();

            Map<String, RemoteAttachmentsDestinationConfiguration> destinations = new HashMap<>();

            RemoteAttachmentsDestinationConfiguration destConfig = new RemoteAttachmentsDestinationConfiguration();
            RemoteAttachmentsS3Settings s3 = new RemoteAttachmentsS3Settings();
            s3.setBucketName("testS3Bucket-Users");
            destConfig.setS3Settings(s3);
            destConfig.setDisabled(false);

            destinations.put("S3-Users", destConfig);

            configToSend.setDestinations(destinations);
            configToSend.setMaxItemsToProcess(1L);

            store.maintenance().send(new ConfigureRemoteAttachmentsOperation(configToSend));

            RemoteAttachmentsConfiguration config =
                    store.maintenance().send(new GetRemoteAttachmentsConfigurationOperation());

            assertThat(config.getDestinations().size())
                    .isEqualTo(1);

            Map.Entry<String, RemoteAttachmentsDestinationConfiguration> destination =
                    config.getDestinations().entrySet().iterator().next();

            assertThat(destination).isNotNull();
            assertThat(destination.getKey())
                    .isEqualTo("S3-Users");

            assertThat(destination.getValue().getS3Settings().getBucketName())
                    .isEqualTo("testS3Bucket-Users");

            assertThat(destination.getValue().isDisabled())
                    .isFalse();

            assertThat(config.getCheckFrequencyInSec())
                    .isNull();
        }
    }

    @DisabledOnPullRequest
    @Test
    public void canPutAndGetRemoteAttachmentsConfiguration() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {

            RemoteAttachmentsConfiguration c1 = new RemoteAttachmentsConfiguration();

            Map<String, RemoteAttachmentsDestinationConfiguration> destinations1 = new HashMap<>();

            RemoteAttachmentsDestinationConfiguration dest1 = new RemoteAttachmentsDestinationConfiguration();
            RemoteAttachmentsS3Settings s3_1 = new RemoteAttachmentsS3Settings();
            s3_1.setBucketName("testS3Bucket-Users");
            dest1.setS3Settings(s3_1);
            dest1.setDisabled(false);

            destinations1.put("S3-Users", dest1);

            c1.setDestinations(destinations1);
            c1.setCheckFrequencyInSec(1000L);

            store.maintenance().send(new ConfigureRemoteAttachmentsOperation(c1));

            RemoteAttachmentsConfiguration config =
                    store.maintenance().send(new GetRemoteAttachmentsConfigurationOperation());

            assertThat(config.getDestinations().size())
                    .isEqualTo(1);

            Map.Entry<String, RemoteAttachmentsDestinationConfiguration> destination =
                    config.getDestinations().entrySet().iterator().next();

            assertThat(destination).isNotNull();
            assertThat(destination.getKey())
                    .isEqualTo("S3-Users");

            assertThat(destination.getValue().getS3Settings().getBucketName())
                    .isEqualTo("testS3Bucket-Users");

            assertThat(destination.getValue().isDisabled())
                    .isFalse();

            assertThat(config.getCheckFrequencyInSec())
                    .isEqualTo(1000L);

            RemoteAttachmentsConfiguration c2 = new RemoteAttachmentsConfiguration();

            Map<String, RemoteAttachmentsDestinationConfiguration> destinations2 = new HashMap<>();

            RemoteAttachmentsDestinationConfiguration dest2 = new RemoteAttachmentsDestinationConfiguration();
            RemoteAttachmentsS3Settings s3_2 = new RemoteAttachmentsS3Settings();
            s3_2.setBucketName("testS3Bucket-Orders");
            dest2.setS3Settings(s3_2);
            dest2.setDisabled(true);

            destinations2.put("S3-Orders", dest2);

            c2.setDestinations(destinations2);
            c2.setCheckFrequencyInSec(10000L);
            c2.setDisabled(true);

            store.maintenance().send(new ConfigureRemoteAttachmentsOperation(c2));

            RemoteAttachmentsConfiguration config2 =
                    store.maintenance().send(new GetRemoteAttachmentsConfigurationOperation());

            assertThat(config2.getDestinations().size())
                    .isEqualTo(1);

            assertThat(config2.isDisabled())
                    .isTrue();

            Map.Entry<String, RemoteAttachmentsDestinationConfiguration> destination2 =
                    config2.getDestinations().entrySet().iterator().next();

            assertThat(destination2).isNotNull();
            assertThat(destination2.getKey())
                    .isEqualTo("S3-Orders");

            assertThat(destination2.getValue().getS3Settings().getBucketName())
                    .isEqualTo("testS3Bucket-Orders");

            assertThat(destination2.getValue().isDisabled())
                    .isTrue();

            assertThat(config2.getCheckFrequencyInSec())
                    .isEqualTo(10000L);
        }
    }

    @DisabledOnPullRequest
    @Test
    public void canPutAndUpdateRemoteAttachmentsConfiguration() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {

            RemoteAttachmentsConfiguration c1 = new RemoteAttachmentsConfiguration();

            Map<String, RemoteAttachmentsDestinationConfiguration> destinations1 = new LinkedHashMap<>();

            RemoteAttachmentsDestinationConfiguration dest1 = new RemoteAttachmentsDestinationConfiguration();
            RemoteAttachmentsS3Settings s3_1 = new RemoteAttachmentsS3Settings();
            s3_1.setBucketName("testS3Bucket-Users");
            dest1.setS3Settings(s3_1);
            dest1.setDisabled(false);

            destinations1.put("S3-Users", dest1);

            c1.setDestinations(destinations1);
            c1.setCheckFrequencyInSec(1000L);

            store.maintenance().send(new ConfigureRemoteAttachmentsOperation(c1));

            RemoteAttachmentsConfiguration config =
                    store.maintenance().send(new GetRemoteAttachmentsConfigurationOperation());

            Map.Entry<String, RemoteAttachmentsDestinationConfiguration> destination =
                    config.getDestinations().entrySet().iterator().next();

            assertThat(destination).isNotNull();
            assertThat(destination.getKey()).isEqualTo("S3-Users");
            assertThat(destination.getValue().getS3Settings().getBucketName())
                    .isEqualTo("testS3Bucket-Users");
            assertThat(destination.getValue().isDisabled()).isFalse();
            assertThat(config.getCheckFrequencyInSec()).isEqualTo(1000L);

            RemoteAttachmentsDestinationConfiguration ordersDest = new RemoteAttachmentsDestinationConfiguration();
            RemoteAttachmentsS3Settings s3 = new RemoteAttachmentsS3Settings();
            s3.setBucketName("testS3Bucket-Orders");
            ordersDest.setS3Settings(s3);
            ordersDest.setDisabled(true);

            config.getDestinations().put("S3-Orders", ordersDest);


            config.setCheckFrequencyInSec(10000L);

            store.maintenance().send(new ConfigureRemoteAttachmentsOperation(config));

            RemoteAttachmentsConfiguration config2 =
                    store.maintenance().send(new GetRemoteAttachmentsConfigurationOperation());

            Map.Entry<String, RemoteAttachmentsDestinationConfiguration> destination2 =
                    config2.getDestinations().entrySet().stream().reduce((a, b) -> b).orElse(null);

            assertThat(destination2).isNotNull();
            assertThat(destination2.getKey()).isEqualTo("S3-Orders");
            assertThat(destination2.getValue().getS3Settings().getBucketName())
                    .isEqualTo("testS3Bucket-Orders");
            assertThat(destination2.getValue().isDisabled()).isTrue();
            assertThat(config2.getCheckFrequencyInSec()).isEqualTo(10000L);

            RemoteAttachmentsConfiguration config3 =
                    store.maintenance().send(new GetRemoteAttachmentsConfigurationOperation());

            RemoteAttachmentsDestinationConfiguration destUsers =
                    config3.getDestinations().get("S3-Users");

            assertThat(destUsers).isNotNull();
            assertThat(destUsers.getS3Settings().getBucketName())
                    .isEqualTo("testS3Bucket-Users");
            assertThat(destUsers.isDisabled()).isFalse();
            assertThat(config3.getCheckFrequencyInSec()).isEqualTo(10000L);

            RemoteAttachmentsDestinationConfiguration destOrders =
                    config3.getDestinations().get("S3-Orders");

            assertThat(destOrders).isNotNull();
            assertThat(destOrders.getS3Settings().getBucketName())
                    .isEqualTo("testS3Bucket-Orders");
            assertThat(destOrders.isDisabled()).isTrue();
        }
    }

    @Test
    public void canAssertRemoteAttachmentsConfiguration() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {

            boolean[] disabledValues = new boolean[] { true, false };

            for (boolean disabled : disabledValues) {

                assertThatThrownBy(() -> {
                    RemoteAttachmentsConfiguration cfg = new RemoteAttachmentsConfiguration();

                    Map<String, RemoteAttachmentsDestinationConfiguration> dest = new LinkedHashMap<>();

                    RemoteAttachmentsDestinationConfiguration d = new RemoteAttachmentsDestinationConfiguration();

                    RemoteAttachmentsS3Settings s3Settings = new RemoteAttachmentsS3Settings();
                    s3Settings.setBucketName("testS3Bucket");
                    d.setS3Settings(s3Settings);

                    RemoteAttachmentsAzureSettings azureSettings = new RemoteAttachmentsAzureSettings();
                    azureSettings.setAccountName("testAzureAccount");
                    azureSettings.setStorageContainer("testAzureContainer");
                    d.setAzureSettings(azureSettings);

                    d.setDisabled(disabled);

                    dest.put(null, d);

                    cfg.setDestinations(dest);
                    cfg.setCheckFrequencyInSec(1000L);

                    store.maintenance().send(new ConfigureRemoteAttachmentsOperation(cfg));
                })
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("Value cannot be null. (Parameter 'key')");

                assertThatThrownBy(() -> {
                    RemoteAttachmentsConfiguration cfg = new RemoteAttachmentsConfiguration();

                    Map<String, RemoteAttachmentsDestinationConfiguration> dest = new LinkedHashMap<>();

                    RemoteAttachmentsDestinationConfiguration d = new RemoteAttachmentsDestinationConfiguration();

                    RemoteAttachmentsS3Settings s3 = new RemoteAttachmentsS3Settings();
                    s3.setBucketName("testS3Bucket");
                    d.setS3Settings(s3);

                    RemoteAttachmentsAzureSettings azure = new RemoteAttachmentsAzureSettings();
                    azure.setAccountName("testAzureAccount");
                    azure.setStorageContainer("testAzureContainer");
                    d.setAzureSettings(azure);

                    d.setDisabled(disabled);

                    dest.put(null, d);

                    cfg.setDestinations(dest);
                    cfg.setCheckFrequencyInSec(1000L);

                    store.maintenance().send(new ConfigureRemoteAttachmentsOperation(cfg));
                })
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("Value cannot be null. (Parameter 'key')");


                assertThatThrownBy(() -> {
                    RemoteAttachmentsConfiguration cfg = new RemoteAttachmentsConfiguration();

                    Map<String, RemoteAttachmentsDestinationConfiguration> dest = new LinkedHashMap<>();

                    RemoteAttachmentsDestinationConfiguration d = new RemoteAttachmentsDestinationConfiguration();

                    RemoteAttachmentsS3Settings s3 = new RemoteAttachmentsS3Settings();
                    s3.setBucketName("testS3Bucket");
                    d.setS3Settings(s3);

                    RemoteAttachmentsAzureSettings azure = new RemoteAttachmentsAzureSettings();
                    azure.setAccountName("testAzureAccount");
                    azure.setStorageContainer("testAzureContainer");
                    d.setAzureSettings(azure);

                    d.setDisabled(disabled);

                    dest.put("test", d);

                    cfg.setDestinations(dest);
                    cfg.setCheckFrequencyInSec(1000L);

                    store.maintenance().send(new ConfigureRemoteAttachmentsOperation(cfg));
                })
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining("Only one uploader for RemoteAttachmentsConfiguration can be configured");

                assertThatThrownBy(() -> {
                    RemoteAttachmentsConfiguration cfg = new RemoteAttachmentsConfiguration();

                    Map<String, RemoteAttachmentsDestinationConfiguration> dest = new LinkedHashMap<>();

                    RemoteAttachmentsDestinationConfiguration d = new RemoteAttachmentsDestinationConfiguration();

                    RemoteAttachmentsS3Settings s3 = new RemoteAttachmentsS3Settings();
                    s3.setBucketName("testS3Bucket");
                    d.setS3Settings(s3);

                    d.setDisabled(disabled);

                    dest.put("test", d);

                    cfg.setDestinations(dest);
                    cfg.setCheckFrequencyInSec(0L);

                    store.maintenance().send(new ConfigureRemoteAttachmentsOperation(cfg));
                })
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining("Remote attachments check frequency must be greater than 0.");

                assertThatThrownBy(() -> {
                    RemoteAttachmentsConfiguration cfg = new RemoteAttachmentsConfiguration();

                    Map<String, RemoteAttachmentsDestinationConfiguration> dest = new LinkedHashMap<>();

                    RemoteAttachmentsDestinationConfiguration d = new RemoteAttachmentsDestinationConfiguration();

                    RemoteAttachmentsS3Settings s3 = new RemoteAttachmentsS3Settings();
                    s3.setBucketName("testS3Bucket");
                    d.setS3Settings(s3);

                    d.setDisabled(disabled);

                    dest.put("test", d);

                    cfg.setDestinations(dest);
                    cfg.setCheckFrequencyInSec(1L);
                    cfg.setMaxItemsToProcess(0L);

                    store.maintenance().send(new ConfigureRemoteAttachmentsOperation(cfg));
                })
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining("Max items to process must be greater than 0.");

                assertThatThrownBy(() -> {
                    RemoteAttachmentsConfiguration cfg = new RemoteAttachmentsConfiguration();

                    Map<String, RemoteAttachmentsDestinationConfiguration> dest = new LinkedHashMap<>();

                    RemoteAttachmentsDestinationConfiguration d = new RemoteAttachmentsDestinationConfiguration();
                    d.setDisabled(disabled);

                    dest.put("test", d);

                    cfg.setDestinations(dest);
                    cfg.setCheckFrequencyInSec(1L);
                    cfg.setMaxItemsToProcess(1L);

                    store.maintenance().send(new ConfigureRemoteAttachmentsOperation(cfg));
                })
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining("Exactly one uploader for RemoteAttachmentsConfiguration must be configured.");

                assertThatThrownBy(() -> {
                    RemoteAttachmentsConfiguration cfg = new RemoteAttachmentsConfiguration();

                    Map<String, RemoteAttachmentsDestinationConfiguration> dest = new LinkedHashMap<>();

                    RemoteAttachmentsDestinationConfiguration d = new RemoteAttachmentsDestinationConfiguration();

                    RemoteAttachmentsS3Settings s3 = new RemoteAttachmentsS3Settings();
                    s3.setBucketName("testS3Bucket");
                    d.setS3Settings(s3);

                    RemoteAttachmentsAzureSettings azure = new RemoteAttachmentsAzureSettings();
                    azure.setAccountName("testAzureAccount");
                    azure.setStorageContainer("testAzureContainer");
                    d.setAzureSettings(azure);

                    d.setDisabled(disabled);

                    dest.put("test", d);

                    cfg.setDestinations(dest);
                    cfg.setCheckFrequencyInSec(1L);
                    cfg.setMaxItemsToProcess(1L);

                    store.maintenance().send(new ConfigureRemoteAttachmentsOperation(cfg));
                })
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining("Only one uploader for RemoteAttachmentsConfiguration can be configured.");

                assertThatThrownBy(() -> {
                    RemoteAttachmentsConfiguration cfg = new RemoteAttachmentsConfiguration();

                    Map<String, RemoteAttachmentsDestinationConfiguration> dest = new LinkedHashMap<>();
                    dest.put("S3-Users", null);

                    cfg.setDestinations(dest);
                    cfg.setCheckFrequencyInSec(1000L);

                    store.maintenance().send(new ConfigureRemoteAttachmentsOperation(cfg));
                })
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining("Destination configuration for key S3-Users is null");

                assertThatThrownBy(() -> {
                    RemoteAttachmentsConfiguration cfg = new RemoteAttachmentsConfiguration();

                    Map<String, RemoteAttachmentsDestinationConfiguration> dest = new LinkedHashMap<>();

                    RemoteAttachmentsDestinationConfiguration cfg1 = new RemoteAttachmentsDestinationConfiguration();
                    cfg1.setDisabled(false);
                    RemoteAttachmentsS3Settings s3 = new RemoteAttachmentsS3Settings();
                    s3.setBucketName("testS3Bucket");
                    cfg1.setS3Settings(s3);
                    dest.put("test", cfg1);

                    RemoteAttachmentsDestinationConfiguration cfg2 = new RemoteAttachmentsDestinationConfiguration();
                    cfg2.setDisabled(false);
                    RemoteAttachmentsAzureSettings azure = new RemoteAttachmentsAzureSettings();
                    azure.setAccountName("testAzureAccount");
                    azure.setStorageContainer("testAzureContainer");
                    cfg2.setAzureSettings(azure);
                    dest.put("TEST", cfg2);

                    cfg.setDestinations(dest);
                    cfg.setCheckFrequencyInSec(1L);

                    store.maintenance()
                            .forDatabase(store.getDatabase())
                            .send(new ConfigureRemoteAttachmentsOperation(cfg));
                })
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining("Destination key 'TEST' is duplicate. Duplicate keys are not allowed in remote attachments configuration");

            }
        }
    }
}
