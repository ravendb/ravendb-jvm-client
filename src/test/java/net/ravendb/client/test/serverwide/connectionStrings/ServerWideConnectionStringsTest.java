package net.ravendb.client.test.serverwide.connectionStrings;

import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.operations.connectionStrings.GetConnectionStringsOperation;
import net.ravendb.client.documents.operations.connectionStrings.GetConnectionStringsResult;
import net.ravendb.client.documents.operations.etl.RavenConnectionString;
import net.ravendb.client.documents.operations.etl.sql.SqlConnectionString;
import net.ravendb.client.extensions.JsonExtensions;
import net.ravendb.client.infrastructure.EnableOnServer;
import net.ravendb.client.serverwide.ConnectionStringType;
import net.ravendb.client.serverwide.operations.connectionStrings.GetServerWideConnectionStringsOperation;
import net.ravendb.client.serverwide.operations.connectionStrings.GetServerWideConnectionStringsResult;
import net.ravendb.client.serverwide.operations.connectionStrings.PutServerWideConnectionStringOperation;
import net.ravendb.client.serverwide.operations.connectionStrings.PutServerWideConnectionStringResult;
import net.ravendb.client.serverwide.operations.connectionStrings.RemoveServerWideConnectionStringOperation;
import net.ravendb.client.serverwide.operations.connectionStrings.RemoveServerWideConnectionStringResult;
import net.ravendb.client.serverwide.operations.connectionStrings.ServerWideConnectionString;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@EnableOnServer(thresholdVersion = "7.2")
public class ServerWideConnectionStringsTest extends RemoteTestBase {

    private static ServerWideConnectionString ravenServerWide(String name, String database, String url, String[] excludedDatabases) {
        RavenConnectionString connectionString = new RavenConnectionString();
        connectionString.setName(name);
        connectionString.setDatabase(database);
        connectionString.setTopologyDiscoveryUrls(new String[]{url});

        ServerWideConnectionString serverWide = new ServerWideConnectionString();
        serverWide.setConnectionString(connectionString);
        serverWide.setExcludedDatabases(excludedDatabases);
        return serverWide;
    }

    @Test
    public void serverWideConnectionStringIsFlattenedOnTheWire() {
        ServerWideConnectionString serverWide = ravenServerWide("swcs/1", "db1", "http://localhost:8080", new String[]{"excluded1"});

        ObjectNode json = JsonExtensions.getDefaultMapper().valueToTree(serverWide);

        // The underlying connection string must be inlined, not nested under a "ConnectionString" property.
        assertThat(json.has("ConnectionString")).isFalse();
        assertThat(json.get("Name").asText()).isEqualTo("swcs/1");
        assertThat(json.get("Database").asText()).isEqualTo("db1");
        assertThat(json.get("Type").asText()).isEqualTo("Raven");
        assertThat(json.get("TopologyDiscoveryUrls").get(0).asText()).isEqualTo("http://localhost:8080");
        assertThat(json.get("ExcludedDatabases").get(0).asText()).isEqualTo("excluded1");
    }

    @Test
    public void canRoundTripServerWideConnectionString() throws Exception {
        ServerWideConnectionString serverWide = ravenServerWide("swcs/2", "db2", "http://localhost:8080", new String[]{"a", "b"});

        String json = JsonExtensions.getDefaultMapper().writeValueAsString(serverWide);
        ServerWideConnectionString parsed = JsonExtensions.getDefaultMapper().readValue(json, ServerWideConnectionString.class);

        assertThat(parsed).isNotNull();
        assertThat(parsed.getName()).isEqualTo("swcs/2");
        assertThat(parsed.getType()).isEqualTo(ConnectionStringType.RAVEN);
        assertThat(parsed.getExcludedDatabases()).containsExactly("a", "b");
        assertThat(parsed.getConnectionString()).isInstanceOf(RavenConnectionString.class);
        assertThat(((RavenConnectionString) parsed.getConnectionString()).getDatabase()).isEqualTo("db2");
        assertThat(parsed.isExcluded("A")).isTrue();
        assertThat(parsed.isExcluded("c")).isFalse();
    }

    @Test
    public void canRoundTripSqlServerWideConnectionString() throws Exception {
        SqlConnectionString sql = new SqlConnectionString();
        sql.setName("swcs/sql");
        sql.setConnectionString("Server=localhost;Database=test;");
        sql.setFactoryName("Npgsql");

        ServerWideConnectionString serverWide = new ServerWideConnectionString();
        serverWide.setConnectionString(sql);

        String json = JsonExtensions.getDefaultMapper().writeValueAsString(serverWide);
        ServerWideConnectionString parsed = JsonExtensions.getDefaultMapper().readValue(json, ServerWideConnectionString.class);

        assertThat(parsed.getType()).isEqualTo(ConnectionStringType.SQL);
        assertThat(parsed.getConnectionString()).isInstanceOf(SqlConnectionString.class);
        assertThat(((SqlConnectionString) parsed.getConnectionString()).getFactoryName()).isEqualTo("Npgsql");
        assertThat(parsed.getExcludedDatabases()).isNull();
    }

    @Test
    public void operationsValidateTheirArguments() {
        assertThatThrownBy(() -> new GetServerWideConnectionStringsOperation("", ConnectionStringType.RAVEN))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new PutServerWideConnectionStringOperation(null))
                .isInstanceOf(IllegalArgumentException.class);

        // ConnectionString itself must be populated
        assertThatThrownBy(() -> new PutServerWideConnectionStringOperation(new ServerWideConnectionString()))
                .isInstanceOf(IllegalArgumentException.class);

        // Only the name is required to remove, but it must be present
        assertThatThrownBy(() -> new RemoveServerWideConnectionStringOperation<>(new RavenConnectionString()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * Server-wide connection strings are a licensed feature. A license that predates the 7.2.5
     * {@code ServerWideConnectionStrings} attribute rejects the PUT, which says nothing about the client,
     * so skip rather than fail in that case.
     */
    private static PutServerWideConnectionStringResult putOrSkipWhenUnlicensed(IDocumentStore store, ServerWideConnectionString serverWide) {
        try {
            return store.maintenance().server().send(new PutServerWideConnectionStringOperation(serverWide));
        } catch (RuntimeException e) {
            String message = e.getMessage();
            if (message != null && message.contains("ServerWideConnectionStrings")) {
                Assumptions.abort("The test server's license does not support server-wide connection strings");
            }
            throw e;
        }
    }

    @Test
    public void canCreateGetAndDeleteServerWideConnectionString() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            ServerWideConnectionString serverWide = ravenServerWide("swcs/crud", "someDatabase", store.getUrls()[0], null);

            PutServerWideConnectionStringResult putResult = putOrSkipWhenUnlicensed(store, serverWide);
            assertThat(putResult.getRaftCommandIndex()).isGreaterThan(0);

            GetServerWideConnectionStringsResult getResult = store.maintenance().server().send(
                    new GetServerWideConnectionStringsOperation("swcs/crud", ConnectionStringType.RAVEN));

            assertThat(getResult.getResults()).hasSize(1);
            ServerWideConnectionString fetched = getResult.getResults().get(0);
            assertThat(fetched.getName()).isEqualTo("swcs/crud");
            assertThat(fetched.getType()).isEqualTo(ConnectionStringType.RAVEN);
            assertThat(fetched.getConnectionString()).isInstanceOf(RavenConnectionString.class);
            assertThat(((RavenConnectionString) fetched.getConnectionString()).getDatabase()).isEqualTo("someDatabase");

            RemoveServerWideConnectionStringResult removeResult = store.maintenance().server().send(
                    new RemoveServerWideConnectionStringOperation<>((RavenConnectionString) fetched.getConnectionString()));
            assertThat(removeResult.getRaftCommandIndex()).isGreaterThan(0);

            GetServerWideConnectionStringsResult afterDelete = store.maintenance().server().send(
                    new GetServerWideConnectionStringsOperation());
            assertThat(afterDelete.getResults())
                    .noneMatch(x -> "swcs/crud".equals(x.getName()));
        }
    }

    @Test
    public void serverWideConnectionStringIsPropagatedToDatabases() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            ServerWideConnectionString serverWide = ravenServerWide("swcs/propagated", "someDatabase", store.getUrls()[0], null);

            putOrSkipWhenUnlicensed(store, serverWide);

            try {
                GetConnectionStringsResult connectionStrings = store.maintenance().send(new GetConnectionStringsOperation());

                assertThat(connectionStrings.getRavenConnectionStrings())
                        .as("the server-wide connection string should be propagated into the database record")
                        .isNotNull();
                assertThat(connectionStrings.getRavenConnectionStrings().keySet())
                        .anyMatch(x -> x.contains("swcs/propagated"));
            } finally {
                store.maintenance().server().send(
                        new RemoveServerWideConnectionStringOperation<>(
                                (RavenConnectionString) serverWide.getConnectionString()));
            }
        }
    }

    @Test
    public void excludedDatabasesAreRespected() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            ServerWideConnectionString serverWide = ravenServerWide(
                    "swcs/excluded", "someDatabase", store.getUrls()[0], new String[]{store.getDatabase()});

            putOrSkipWhenUnlicensed(store, serverWide);

            try {
                GetConnectionStringsResult connectionStrings = store.maintenance().send(new GetConnectionStringsOperation());

                if (connectionStrings.getRavenConnectionStrings() != null) {
                    assertThat(connectionStrings.getRavenConnectionStrings().keySet())
                            .as("an excluded database must not receive the server-wide connection string")
                            .noneMatch(x -> x.contains("swcs/excluded"));
                }
            } finally {
                store.maintenance().server().send(
                        new RemoveServerWideConnectionStringOperation<>(
                                (RavenConnectionString) serverWide.getConnectionString()));
            }
        }
    }
}
