package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.infrastructure.DisabledOn70Server;
import net.ravendb.client.serverwide.operations.logs.GetLogsConfigurationOperation;
import net.ravendb.client.serverwide.operations.logs.GetLogsConfigurationResult;
import net.ravendb.client.serverwide.operations.logs.LogMode;
import net.ravendb.client.serverwide.operations.logs.SetLogsConfigurationOperation;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@DisabledOn70Server
public class RavenDB_11440Test extends RemoteTestBase {

    @Test
    public void canGetLogsConfigurationAndChangeMode() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            GetLogsConfigurationResult configuration = store.maintenance().server().send(new GetLogsConfigurationOperation());


            try {

                LogMode modeToSet;

                switch (configuration.getCurrentMode()) {
                    case NONE:
                        modeToSet = LogMode.INFORMATION;
                        break;
                    case OPERATIONS:
                        modeToSet = LogMode.INFORMATION;
                        break;
                    case INFORMATION:
                        modeToSet = LogMode.NONE;
                        break;
                    default:
                        throw new IllegalStateException("Invalid mode: " + configuration.getCurrentMode());
                }

                Duration time = Duration.ofDays(1000);

                SetLogsConfigurationOperation.Parameters parameters = new SetLogsConfigurationOperation.Parameters();
                parameters.setMode(modeToSet);
                parameters.setRetentionTime(time);
                SetLogsConfigurationOperation setLogsOperation = new SetLogsConfigurationOperation(parameters);
                store.maintenance().server().send(setLogsOperation);

                GetLogsConfigurationResult configuration2 = store.maintenance().server().send(new GetLogsConfigurationOperation());

                assertThat(configuration2.getCurrentMode())
                        .isEqualTo(modeToSet);
                assertThat(configuration2.getRetentionTime().toString())
                        .isEqualTo(time.toString());
                assertThat(configuration2.getMode())
                        .isEqualTo(configuration.getMode());
                assertThat(configuration2.getPath())
                        .isEqualTo(configuration.getPath());
                assertThat(configuration2.isUseUtcTime())
                        .isEqualTo(configuration.isUseUtcTime());
            } finally {
                SetLogsConfigurationOperation.Parameters parameters = new SetLogsConfigurationOperation.Parameters();
                parameters.setMode(configuration.getCurrentMode());
                parameters.setRetentionTime(configuration.getRetentionTime());

                store.maintenance().server().send(new SetLogsConfigurationOperation(parameters));
            }
        }
    }
}
