package net.ravendb.client.documents.operations.cdcSink;

import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.connectionStrings.PutConnectionStringOperation;
import net.ravendb.client.documents.operations.connectionStrings.PutConnectionStringResult;
import net.ravendb.client.documents.operations.etl.sql.SqlConnectionString;
import net.ravendb.client.extensions.JsonExtensions;
import net.ravendb.client.http.IRaftCommand;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.infrastructure.EnableOnServer;
import net.ravendb.client.serverwide.DatabaseRecordWithEtag;
import net.ravendb.client.serverwide.operations.GetDatabaseRecordOperation;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Mirrors the .NET {@code SlowTests.Server.Documents.CdcSink.CdcSinkCrudTests} coverage of adding a CDC sink,
 * plus unit coverage of the command itself: the request it builds and the response it deserializes.
 */
public class AddCdcSinkOperationTest extends RemoteTestBase {

    private static SqlConnectionString createSqlConnectionString(String name) {
        SqlConnectionString connectionString = new SqlConnectionString();
        connectionString.setName(name);
        connectionString.setFactoryName("Microsoft.Data.SqlClient");
        connectionString.setConnectionString("Server=localhost;Database=TestDb;User Id=sa;Password=pass;");

        return connectionString;
    }

    private static CdcSinkConfiguration createCdcSinkConfiguration(String name, String connectionStringName) {
        CdcColumnMapping orderId = new CdcColumnMapping();
        orderId.setColumn("order_id");
        orderId.setName("OrderId");

        CdcColumnMapping customerId = new CdcColumnMapping();
        customerId.setColumn("customer_id");
        customerId.setName("CustomerId");

        CdcSinkTableConfig table = new CdcSinkTableConfig();
        table.setCollectionName("Orders");
        table.setSourceTableSchema("public");
        table.setSourceTableName("orders");
        table.setColumns(Arrays.asList(orderId, customerId));
        table.setPrimaryKeyColumns(Collections.singletonList("order_id"));

        CdcSinkConfiguration configuration = new CdcSinkConfiguration();
        configuration.setName(name);
        configuration.setConnectionStringName(connectionStringName);
        configuration.setTables(Collections.singletonList(table));

        return configuration;
    }

    private static RavenCommand<AddCdcSinkOperationResult> command(CdcSinkConfiguration configuration) {
        DocumentConventions conventions = new DocumentConventions();
        conventions.setUseHttpCompression(false); // so the request body can be read back as plain JSON

        return new AddCdcSinkOperation(configuration).getCommand(conventions);
    }

    @EnableOnServer(thresholdVersion = "7.2")
    @Test
    public void canAddCdcSink() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            SqlConnectionString connectionString = createSqlConnectionString("test-sql");
            PutConnectionStringResult putResult =
                    store.maintenance().send(new PutConnectionStringOperation<>(connectionString));
            assertThat(putResult.getRaftCommandIndex()).isGreaterThan(0);

            CdcSinkConfiguration configuration = createCdcSinkConfiguration("test-cdc", connectionString.getName());
            AddCdcSinkOperationResult addResult = store.maintenance().send(new AddCdcSinkOperation(configuration));

            assertThat(addResult).isNotNull();
            assertThat(addResult.getTaskId()).isGreaterThan(0);
            assertThat(addResult.getRaftCommandIndex()).isGreaterThan(0);

            DatabaseRecordWithEtag record =
                    store.maintenance().server().send(new GetDatabaseRecordOperation(store.getDatabase()));

            assertThat(record.getCdcSinks()).hasSize(1);
            assertThat(record.getCdcSinks().get(0).getName()).isEqualTo("test-cdc");
            assertThat(record.getCdcSinks().get(0).getConnectionStringName()).isEqualTo(connectionString.getName());
            assertThat(record.getCdcSinks().get(0).getTaskId()).isEqualTo(addResult.getTaskId());

            CdcSinkTableConfig table = record.getCdcSinks().get(0).getTables().get(0);
            assertThat(table.getCollectionName()).isEqualTo("Orders");
            assertThat(table.getSourceTableSchema()).isEqualTo("public");
            assertThat(table.getSourceTableName()).isEqualTo("orders");
            assertThat(table.getPrimaryKeyColumns()).containsExactly("order_id");
            assertThat(table.getColumns())
                    .extracting(CdcColumnMapping::getColumn)
                    .containsExactly("order_id", "customer_id");
        }
    }

    @EnableOnServer(thresholdVersion = "7.2")
    @Test
    public void canAddMultipleCdcSinks() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            SqlConnectionString connectionString = createSqlConnectionString("test-sql");
            store.maintenance().send(new PutConnectionStringOperation<>(connectionString));

            store.maintenance().send(new AddCdcSinkOperation(
                    createCdcSinkConfiguration("cdc-sink-1", connectionString.getName())));
            store.maintenance().send(new AddCdcSinkOperation(
                    createCdcSinkConfiguration("cdc-sink-2", connectionString.getName())));

            DatabaseRecordWithEtag record =
                    store.maintenance().server().send(new GetDatabaseRecordOperation(store.getDatabase()));

            assertThat(record.getCdcSinks()).hasSize(2);

            List<String> names = record.getCdcSinks()
                    .stream()
                    .map(CdcSinkConfiguration::getName)
                    .collect(Collectors.toList());

            assertThat(names).containsExactlyInAnyOrder("cdc-sink-1", "cdc-sink-2");
        }
    }

    @Test
    public void configurationIsRequired() {
        assertThatThrownBy(() -> new AddCdcSinkOperation(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void buildsPutRequestAgainstTheAdminCdcSinkEndpoint() throws Exception {
        RavenCommand<AddCdcSinkOperationResult> command =
                command(createCdcSinkConfiguration("test-cdc", "test-sql"));

        ServerNode node = new ServerNode();
        node.setUrl("http://localhost:8080");
        node.setDatabase("db1");

        HttpUriRequestBase request = command.createRequest(node);

        assertThat(request.getMethod()).isEqualTo("PUT");
        assertThat(request.getUri().toString()).isEqualTo("http://localhost:8080/databases/db1/admin/cdc-sink");
        assertThat(command.isReadRequest()).isFalse();
        assertThat(((IRaftCommand) command).getRaftUniqueRequestId()).isNotEmpty();
    }

    @Test
    public void sendsTheConfigurationUsingTheServerPropertyNames() throws Exception {
        RavenCommand<AddCdcSinkOperationResult> command =
                command(createCdcSinkConfiguration("test-cdc", "test-sql"));

        ServerNode node = new ServerNode();
        node.setUrl("http://localhost:8080");
        node.setDatabase("db1");

        HttpUriRequestBase request = command.createRequest(node);

        ByteArrayOutputStream body = new ByteArrayOutputStream();
        request.getEntity().writeTo(body);

        ObjectNode json = (ObjectNode) JsonExtensions.getDefaultMapper()
                .readTree(new String(body.toByteArray(), StandardCharsets.UTF_8));

        assertThat(json.get("Name").asText()).isEqualTo("test-cdc");
        assertThat(json.get("ConnectionStringName").asText()).isEqualTo("test-sql");
        assertThat(json.get("Disabled").asBoolean()).isFalse();
        assertThat(json.get("SkipInitialLoad").asBoolean()).isFalse();

        ObjectNode table = (ObjectNode) json.get("Tables").get(0);
        assertThat(table.get("CollectionName").asText()).isEqualTo("Orders");
        assertThat(table.get("SourceTableSchema").asText()).isEqualTo("public");
        assertThat(table.get("SourceTableName").asText()).isEqualTo("orders");
        assertThat(table.get("PrimaryKeyColumns").get(0).asText()).isEqualTo("order_id");

        ObjectNode column = (ObjectNode) table.get("Columns").get(0);
        assertThat(column.get("Column").asText()).isEqualTo("order_id");
        assertThat(column.get("Name").asText()).isEqualTo("OrderId");
    }

    @Test
    public void deserializesTheOperationResult() throws Exception {
        RavenCommand<AddCdcSinkOperationResult> command =
                command(createCdcSinkConfiguration("test-cdc", "test-sql"));

        command.setResponse("{\"RaftCommandIndex\":42,\"TaskId\":7}", false);

        assertThat(command.getResult().getRaftCommandIndex()).isEqualTo(42);
        assertThat(command.getResult().getTaskId()).isEqualTo(7);
    }

    @Test
    public void missingResponseIsInvalid() {
        RavenCommand<AddCdcSinkOperationResult> command =
                command(createCdcSinkConfiguration("test-cdc", "test-sql"));

        assertThatThrownBy(() -> command.setResponse(null, false))
                .isInstanceOf(IllegalStateException.class);
    }
}
