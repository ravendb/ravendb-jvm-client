package net.ravendb.client.test.server;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.infrastructure.DisabledOn70Server;
import net.ravendb.client.serverwide.operations.logs.GetLogsConfigurationOperation;
import net.ravendb.client.serverwide.operations.logs.GetLogsConfigurationResult;
import net.ravendb.client.serverwide.operations.logs.LogMode;
import net.ravendb.client.serverwide.operations.logs.SetLogsConfigurationOperation;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisabledOn70Server
public class LogsConfigurationTest extends RemoteTestBase {

    @Test
    public void canGetAndSetLogging() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try {
                GetLogsConfigurationOperation getOperation = new GetLogsConfigurationOperation();

                GetLogsConfigurationResult logsConfig =
                        store.maintenance().server().send(getOperation);

                assertThat(logsConfig.getCurrentMode())
                        .isEqualTo(LogMode.OPERATIONS);

                assertThat(logsConfig.getMode())
                        .isEqualTo(LogMode.OPERATIONS);

                // now try to set mode to operations and info
                SetLogsConfigurationOperation.Parameters parameters = new SetLogsConfigurationOperation.Parameters();
                parameters.setMode(LogMode.INFORMATION);
                SetLogsConfigurationOperation setOperation = new SetLogsConfigurationOperation(parameters);

                store.maintenance().server().send(setOperation);

                getOperation = new GetLogsConfigurationOperation();

                logsConfig =
                        store.maintenance().server().send(getOperation);

                assertThat(logsConfig.getCurrentMode())
                        .isEqualTo(LogMode.INFORMATION);

                assertThat(logsConfig.getMode())
                        .isEqualTo(LogMode.OPERATIONS);
            } finally {
                // try to clean up

                SetLogsConfigurationOperation.Parameters parameters = new SetLogsConfigurationOperation.Parameters();
                parameters.setMode(LogMode.OPERATIONS);
                SetLogsConfigurationOperation setOperation = new SetLogsConfigurationOperation(parameters);
                store.maintenance().server().send(setOperation);
            }
        }
    }
}
