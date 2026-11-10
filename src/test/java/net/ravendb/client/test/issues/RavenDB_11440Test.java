package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.serverwide.operations.logs.GetLogsConfigurationOperation;
import net.ravendb.client.serverwide.operations.logs.GetLogsConfigurationResult;
import net.ravendb.client.serverwide.operations.logs.LogLevel;
import net.ravendb.client.serverwide.operations.logs.SetLogsConfigurationOperation;
import net.ravendb.client.infrastructure.EnableOnServer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@EnableOnServer(thresholdVersion = "7.0")
public class RavenDB_11440Test extends RemoteTestBase {

    @Test
    public void canGetLogsConfigurationAndChangeLogMode() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            GetLogsConfigurationResult configuration1 = store.maintenance().server().send(new GetLogsConfigurationOperation());

            LogLevel newMinLevel;
            switch (configuration1.getLogs().getCurrentMinLevel()) {
                case DEBUG:
                    newMinLevel = LogLevel.TRACE;
                    break;
                case TRACE:
                case INFO:
                case WARN:
                case ERROR:
                case FATAL:
                case OFF:
                    newMinLevel = LogLevel.DEBUG;
                    break;
                default:
                    throw new IllegalArgumentException();
            }

            try {
                store.maintenance().server().send(new SetLogsConfigurationOperation(
                        new SetLogsConfigurationOperation.LogsConfiguration(newMinLevel)));

                GetLogsConfigurationResult configuration2 = store.maintenance().server().send(new GetLogsConfigurationOperation());

                assertThat(configuration2.getLogs().getCurrentMinLevel())
                        .isEqualTo(newMinLevel);

                assertThat(configuration2.getLogs().getMinLevel())
                        .isEqualTo(configuration1.getLogs().getMinLevel());
                assertThat(configuration2.getLogs().getArchiveAboveSizeInMb())
                        .isEqualTo(configuration1.getLogs().getArchiveAboveSizeInMb());
                assertThat(configuration2.getLogs().isEnableArchiveFileCompression())
                        .isEqualTo(configuration1.getLogs().isEnableArchiveFileCompression());
                assertThat(configuration2.getLogs().getMaxArchiveDays())
                        .isEqualTo(configuration1.getLogs().getMaxArchiveDays());
                assertThat(configuration2.getLogs().getMaxArchiveFiles())
                        .isEqualTo(configuration1.getLogs().getMaxArchiveFiles());
                assertThat(configuration2.getLogs().getPath())
                        .isEqualTo(configuration1.getLogs().getPath());
            } finally {
                SetLogsConfigurationOperation.LogsConfiguration restore =
                        new SetLogsConfigurationOperation.LogsConfiguration(configuration1.getLogs().getCurrentMinLevel());
                restore.setFilters(configuration1.getLogs().getCurrentFilters());
                restore.setLogFilterDefaultAction(configuration1.getLogs().getCurrentLogFilterDefaultAction());
                store.maintenance().server().send(new SetLogsConfigurationOperation(restore));
            }
        }
    }
}
