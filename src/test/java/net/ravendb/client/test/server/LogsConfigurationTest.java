package net.ravendb.client.test.server;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.infrastructure.EnableOnServer;
import net.ravendb.client.serverwide.operations.logs.AdminLogsConfiguration;
import net.ravendb.client.serverwide.operations.logs.GetLogsConfigurationOperation;
import net.ravendb.client.serverwide.operations.logs.GetLogsConfigurationResult;
import net.ravendb.client.serverwide.operations.logs.LogFilter;
import net.ravendb.client.serverwide.operations.logs.LogFilterAction;
import net.ravendb.client.serverwide.operations.logs.LogLevel;
import net.ravendb.client.serverwide.operations.logs.SetLogsConfigurationOperation;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@EnableOnServer(thresholdVersion = "7.0")
public class LogsConfigurationTest extends RemoteTestBase {

    @Test
    public void canGetLogsConfiguration() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            GetLogsConfigurationResult configuration = store.maintenance().server().send(new GetLogsConfigurationOperation());

            assertThat(configuration.getLogs())
                    .isNotNull();
            assertThat(configuration.getLogs().getPath())
                    .isNotEmpty();
            assertThat(configuration.getLogs().getCurrentMinLevel())
                    .isNotNull();
            assertThat(configuration.getLogs().getMinLevel())
                    .isNotNull();
            assertThat(configuration.getLogs().getCurrentLogFilterDefaultAction())
                    .isNotNull();
            assertThat(configuration.getLogs().getCurrentFilters())
                    .isNotNull();

            assertThat(configuration.getAdminLogs())
                    .isNotNull();
            assertThat(configuration.getAdminLogs().getCurrentMinLevel())
                    .isNotNull();
        }
    }

    @Test
    public void canSetAdminLogsConfigurationWithFilters() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            GetLogsConfigurationResult configuration1 = store.maintenance().server().send(new GetLogsConfigurationOperation());
            AdminLogsConfiguration original = configuration1.getAdminLogs();

            LogLevel newMinLevel = original.getCurrentMinLevel() == LogLevel.INFO ? LogLevel.WARN : LogLevel.INFO;

            try {
                SetLogsConfigurationOperation.AdminLogsConfiguration adminLogs =
                        new SetLogsConfigurationOperation.AdminLogsConfiguration(newMinLevel);
                adminLogs.setFilters(Collections.singletonList(
                        new LogFilter(LogLevel.DEBUG, LogLevel.ERROR, "contains('${message}', 'test')", LogFilterAction.IGNORE)));
                adminLogs.setLogFilterDefaultAction(LogFilterAction.LOG);

                store.maintenance().server().send(new SetLogsConfigurationOperation(adminLogs));

                GetLogsConfigurationResult configuration2 = store.maintenance().server().send(new GetLogsConfigurationOperation());

                assertThat(configuration2.getAdminLogs().getCurrentMinLevel())
                        .isEqualTo(newMinLevel);
                assertThat(configuration2.getAdminLogs().getCurrentLogFilterDefaultAction())
                        .isEqualTo(LogFilterAction.LOG);
                assertThat(configuration2.getAdminLogs().getCurrentFilters())
                        .hasSize(1);

                LogFilter filter = configuration2.getAdminLogs().getCurrentFilters().get(0);
                assertThat(filter.getMinLevel())
                        .isEqualTo(LogLevel.DEBUG);
                assertThat(filter.getMaxLevel())
                        .isEqualTo(LogLevel.ERROR);
                assertThat(filter.getCondition())
                        .isEqualTo("contains('${message}', 'test')");
                assertThat(filter.getAction())
                        .isEqualTo(LogFilterAction.IGNORE);

                // the regular logs configuration is not affected
                assertThat(configuration2.getLogs().getCurrentMinLevel())
                        .isEqualTo(configuration1.getLogs().getCurrentMinLevel());
            } finally {
                SetLogsConfigurationOperation.AdminLogsConfiguration restore =
                        new SetLogsConfigurationOperation.AdminLogsConfiguration(original.getCurrentMinLevel());
                restore.setFilters(original.getCurrentFilters());
                restore.setLogFilterDefaultAction(original.getCurrentLogFilterDefaultAction());
                store.maintenance().server().send(new SetLogsConfigurationOperation(restore));
            }

            GetLogsConfigurationResult configuration3 = store.maintenance().server().send(new GetLogsConfigurationOperation());
            assertThat(configuration3.getAdminLogs().getCurrentMinLevel())
                    .isEqualTo(original.getCurrentMinLevel());
            assertThat(configuration3.getAdminLogs().getCurrentFilters())
                    .hasSameSizeAs(original.getCurrentFilters());
        }
    }

    @Test
    public void setLogsConfigurationRequiresConfiguration() {
        assertThatThrownBy(() -> new SetLogsConfigurationOperation((SetLogsConfigurationOperation.LogsConfiguration) null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SetLogsConfigurationOperation((SetLogsConfigurationOperation.MicrosoftLogsConfiguration) null, true))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SetLogsConfigurationOperation((SetLogsConfigurationOperation.AdminLogsConfiguration) null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new LogFilter(LogLevel.DEBUG, LogLevel.ERROR, null, LogFilterAction.LOG))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
