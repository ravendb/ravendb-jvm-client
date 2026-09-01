package net.ravendb.client.test.server.etl.snowflake;

import net.ravendb.client.documents.operations.etl.EtlType;
import net.ravendb.client.documents.operations.etl.snowflake.SnowflakeConnectionString;
import net.ravendb.client.documents.operations.etl.snowflake.SnowflakeEtlConfiguration;
import net.ravendb.client.documents.operations.etl.snowflake.SnowflakeEtlTable;
import net.ravendb.client.documents.operations.ongoingTasks.OngoingTaskSnowflakeEtl;
import net.ravendb.client.documents.operations.ongoingTasks.OngoingTaskType;
import net.ravendb.client.extensions.JsonExtensions;
import net.ravendb.client.serverwide.ConnectionStringType;
import net.ravendb.client.serverwide.DatabaseRecord;
import net.ravendb.client.serverwide.operations.DatabaseRecordBuilder;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers the Snowflake ETL client surface: enum names on the wire, configuration round trips,
 * the ongoing-task shape, and the database record builder entry points. Driving a real Snowflake
 * ETL requires a Snowflake account, which the Java test suite does not provision.
 */
public class SnowflakeEtlTest {

    private static SnowflakeEtlConfiguration configuration() {
        SnowflakeEtlTable table = new SnowflakeEtlTable();
        table.setTableName("Orders");
        table.setDocumentIdColumn("Id");
        table.setInsertOnlyMode(true);

        SnowflakeEtlConfiguration configuration = new SnowflakeEtlConfiguration();
        configuration.setName("snowflake-etl");
        configuration.setConnectionStringName("snowflake-cs");
        configuration.setCommandTimeout(30);
        configuration.setSnowflakeTables(Collections.singletonList(table));

        return configuration;
    }

    @Test
    public void etlTypeIsSentUsingTheServerName() throws Exception {
        assertThat(new SnowflakeEtlConfiguration().getEtlType()).isEqualTo(EtlType.SNOWFLAKE);

        String json = JsonExtensions.getDefaultMapper().writeValueAsString(configuration());
        assertThat(json).contains("\"EtlType\":\"Snowflake\"");
    }

    @Test
    public void snowflakeTablesDefaultToAnEmptyList() {
        assertThat(new SnowflakeEtlConfiguration().getSnowflakeTables()).isEmpty();
    }

    @Test
    public void canRoundTripConfiguration() throws Exception {
        String json = JsonExtensions.getDefaultMapper().writeValueAsString(configuration());

        assertThat(json).contains("\"SnowflakeTables\"").contains("\"CommandTimeout\":30");

        SnowflakeEtlConfiguration parsed =
                JsonExtensions.getDefaultMapper().readValue(json, SnowflakeEtlConfiguration.class);

        assertThat(parsed.getName()).isEqualTo("snowflake-etl");
        assertThat(parsed.getConnectionStringName()).isEqualTo("snowflake-cs");
        assertThat(parsed.getCommandTimeout()).isEqualTo(30);
        assertThat(parsed.getSnowflakeTables()).hasSize(1);
        assertThat(parsed.getSnowflakeTables().get(0).getTableName()).isEqualTo("Orders");
        assertThat(parsed.getSnowflakeTables().get(0).getDocumentIdColumn()).isEqualTo("Id");
        assertThat(parsed.getSnowflakeTables().get(0).isInsertOnlyMode()).isTrue();
    }

    @Test
    public void canDeserializeOngoingTask() throws Exception {
        String json = "{\"TaskType\":\"SnowflakeEtl\",\"TaskId\":7,\"TaskName\":\"snowflake-etl\","
                + "\"ConnectionStringName\":\"snowflake-cs\",\"ConnectionString\":\"account=acc;\","
                + "\"Configuration\":{\"Name\":\"snowflake-etl\",\"CommandTimeout\":30,"
                + "\"SnowflakeTables\":[{\"TableName\":\"Orders\",\"DocumentIdColumn\":\"Id\",\"InsertOnlyMode\":false}]}}";

        OngoingTaskSnowflakeEtl task =
                JsonExtensions.getDefaultMapper().readValue(json, OngoingTaskSnowflakeEtl.class);

        assertThat(task.getTaskType()).isEqualTo(OngoingTaskType.SNOWFLAKE_ETL);
        assertThat(task.getTaskId()).isEqualTo(7);
        assertThat(task.getConnectionStringName()).isEqualTo("snowflake-cs");
        assertThat(task.getConnectionString()).isEqualTo("account=acc;");
        assertThat(task.getConfiguration().getSnowflakeTables()).hasSize(1);
        assertThat(task.getConfiguration().getEtlType()).isEqualTo(EtlType.SNOWFLAKE);
    }

    @Test
    public void ongoingTaskTypeIsSentUsingTheServerName() throws Exception {
        String json = JsonExtensions.getDefaultMapper().writeValueAsString(new OngoingTaskSnowflakeEtl());
        assertThat(json).contains("\"TaskType\":\"SnowflakeEtl\"");
    }

    @Test
    public void connectionStringTypeIsSentUsingTheServerName() throws Exception {
        SnowflakeConnectionString connectionString = new SnowflakeConnectionString();
        connectionString.setName("snowflake-cs");
        connectionString.setConnectionString("account=acc;user=usr;");

        assertThat(connectionString.getType()).isEqualTo(ConnectionStringType.SNOWFLAKE);

        String json = JsonExtensions.getDefaultMapper().writeValueAsString(connectionString);
        assertThat(json).contains("\"Type\":\"Snowflake\"");

        SnowflakeConnectionString parsed =
                JsonExtensions.getDefaultMapper().readValue(json, SnowflakeConnectionString.class);
        assertThat(parsed.getConnectionString()).isEqualTo("account=acc;user=usr;");
    }

    @Test
    public void databaseRecordBuilderAcceptsSnowflake() {
        SnowflakeConnectionString connectionString = new SnowflakeConnectionString();
        connectionString.setName("snowflake-cs");
        connectionString.setConnectionString("account=acc;");

        DatabaseRecord record = DatabaseRecordBuilder.create()
                .regular("db1")
                .withConnectionStrings(builder -> builder.addSnowflakeConnectionString(connectionString))
                .withEtls(builder -> builder.addSnowflakeEtl(configuration()))
                .toDatabaseRecord();

        assertThat(record.getSnowflakeConnectionStrings()).containsKey("snowflake-cs");
        assertThat(record.getSnowflakeEtls()).hasSize(1);
        assertThat(record.getSnowflakeEtls().get(0).getName()).isEqualTo("snowflake-etl");
    }
}
