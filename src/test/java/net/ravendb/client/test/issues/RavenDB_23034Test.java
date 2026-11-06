package net.ravendb.client.test.issues;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.ravendb.client.documents.operations.backups.RestoreBackupConfiguration;
import net.ravendb.client.documents.smuggler.DatabaseRecordItemType;
import net.ravendb.client.documents.smuggler.DatabaseSmugglerExportOptions;
import net.ravendb.client.documents.smuggler.DatabaseSmugglerImportOptions;
import net.ravendb.client.extensions.JsonExtensions;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;

public class RavenDB_23034Test {

    // the C# rate-limit tests (SmugglerApiTests, MaxReadOpsPerSecOptionTests) measure timings of large exports/backups;
    // here we verify the options reach the wire with the C# property names

    @Test
    public void smugglerOptionsSerializeMaxReadOpsPerSecondAndDatabaseRecordTypes() {
        ObjectMapper mapper = JsonExtensions.getDefaultMapper();

        DatabaseSmugglerExportOptions exportOptions = new DatabaseSmugglerExportOptions();
        exportOptions.setMaxReadOpsPerSecond(100);
        exportOptions.setOperateOnDatabaseRecordTypes(EnumSet.of(DatabaseRecordItemType.CLIENT, DatabaseRecordItemType.EXPIRATION));

        JsonNode json = mapper.valueToTree(exportOptions);
        assertThat(json.get("MaxReadOpsPerSecond").asInt())
                .isEqualTo(100);
        assertThat(json.get("OperateOnDatabaseRecordTypes").asText().split(","))
                .containsExactlyInAnyOrder("Client", "Expiration");
        assertThat(json.has("OperateOnDatabaseRecordType"))
                .isFalse();

        DatabaseSmugglerImportOptions importOptions = new DatabaseSmugglerImportOptions();
        json = mapper.valueToTree(importOptions);
        assertThat(json.get("OperateOnDatabaseRecordTypes").asText().split(","))
                .contains("Client", "PostgreSQLIntegration", "CdcSinks");
        assertThat(json.has("MaxReadOpsPerSecond"))
                .isTrue();
        assertThat(json.get("MaxReadOpsPerSecond").isNull())
                .isTrue();
    }

    @Test
    public void restoreConfigurationSerializesMaxReadOpsPerSecond() {
        ObjectMapper mapper = JsonExtensions.getDefaultMapper();

        RestoreBackupConfiguration restore = new RestoreBackupConfiguration();
        restore.setMaxReadOpsPerSecond(50);

        JsonNode json = mapper.valueToTree(restore);
        assertThat(json.get("MaxReadOpsPerSecond").asInt())
                .isEqualTo(50);
    }
}
