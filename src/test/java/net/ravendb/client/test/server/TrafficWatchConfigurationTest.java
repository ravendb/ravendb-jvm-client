package net.ravendb.client.test.server;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.changes.TrafficWatchChangeType;
import net.ravendb.client.serverwide.operations.trafficWatch.GetTrafficWatchConfigurationOperation;
import net.ravendb.client.serverwide.operations.trafficWatch.PutTrafficWatchConfigurationOperation;
import net.ravendb.client.serverwide.operations.trafficWatch.TrafficWatchMode;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

public class TrafficWatchConfigurationTest extends RemoteTestBase {

    @Test
    public void checkDefaultsAndCanSetAndGetTrafficWatchConfiguration() throws Exception {
        try (DocumentStore store = getDocumentStore()) {
            PutTrafficWatchConfigurationOperation.Parameters defaultConfiguration = store.maintenance().server().send(new GetTrafficWatchConfigurationOperation());

            assertThat(defaultConfiguration.getTrafficWatchMode())
                    .isEqualTo(TrafficWatchMode.OFF);
            assertThat(defaultConfiguration.getDatabases())
                    .isNull();
            assertThat(defaultConfiguration.getStatusCodes())
                    .isNull();
            assertThat(defaultConfiguration.getMinimumResponseSizeInBytes())
                    .isEqualTo(0);
            assertThat(defaultConfiguration.getMinimumRequestSizeInBytes())
                    .isEqualTo(0);
            assertThat(defaultConfiguration.getMinimumDurationInMs())
                    .isEqualTo(0);
            assertThat(defaultConfiguration.getHttpMethods())
                    .isNull();
            assertThat(defaultConfiguration.getChangeTypes())
                    .isNull();

            PutTrafficWatchConfigurationOperation.Parameters configuration1 = new PutTrafficWatchConfigurationOperation.Parameters();
            configuration1.setTrafficWatchMode(TrafficWatchMode.OFF);
            configuration1.setDatabases(Arrays.asList("test1", "test2"));
            configuration1.setStatusCodes(Arrays.asList(200, 404, 500));
            configuration1.setMinimumResponseSizeInBytes(11);
            configuration1.setMinimumRequestSizeInBytes(22);
            configuration1.setMinimumDurationInMs(33);
            configuration1.setHttpMethods(Arrays.asList("GET", "POST"));
            configuration1.setChangeTypes(Arrays.asList(TrafficWatchChangeType.QUERIES, TrafficWatchChangeType.COUNTERS, TrafficWatchChangeType.BULK_DOCS));

            store.maintenance().server().send(new PutTrafficWatchConfigurationOperation(configuration1));

            PutTrafficWatchConfigurationOperation.Parameters configuration2 = store.maintenance().server().send(new GetTrafficWatchConfigurationOperation());

            assertThat(configuration2.getTrafficWatchMode())
                    .isEqualTo(configuration1.getTrafficWatchMode());
            assertThat(configuration2.getDatabases())
                    .isEqualTo(configuration1.getDatabases());
            assertThat(configuration2.getStatusCodes())
                    .isEqualTo(configuration1.getStatusCodes());
            assertThat(configuration2.getMinimumResponseSizeInBytes())
                    .isEqualTo(configuration1.getMinimumResponseSizeInBytes());
            assertThat(configuration2.getMinimumRequestSizeInBytes())
                    .isEqualTo(configuration1.getMinimumRequestSizeInBytes());
            assertThat(configuration2.getMinimumDurationInMs())
                    .isEqualTo(configuration1.getMinimumDurationInMs());
            assertThat(configuration2.getHttpMethods())
                    .isEqualTo(configuration1.getHttpMethods());
            assertThat(configuration2.getChangeTypes())
                    .isEqualTo(configuration1.getChangeTypes());
        }
    }
}
