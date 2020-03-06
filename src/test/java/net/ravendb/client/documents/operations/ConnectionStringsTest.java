package net.ravendb.client.documents.operations;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.operations.connectionStrings.*;
import net.ravendb.client.documents.operations.etl.sql.SqlConnectionString;
import net.ravendb.client.serverwide.ConnectionStringType;
import net.ravendb.client.documents.operations.etl.RavenConnectionString;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ConnectionStringsTest extends RemoteTestBase {

    @Test
    public void canCreateGetAndDeleteConnectionStrings() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {

            RavenConnectionString ravenConnectionString1 = new RavenConnectionString();
            ravenConnectionString1.setDatabase("db1");
            ravenConnectionString1.setTopologyDiscoveryUrls(new String[] { "http://localhost:8080" });
            ravenConnectionString1.setName("r1");

            SqlConnectionString sqlConnectionString1 = new SqlConnectionString();
            sqlConnectionString1.setFactoryName("test");
            sqlConnectionString1.setConnectionString("test");
            sqlConnectionString1.setName("s1");

            PutConnectionStringResult putResult = store.maintenance().send(new PutConnectionStringOperation<>(ravenConnectionString1));
            store.maintenance().send(new PutConnectionStringOperation<>(sqlConnectionString1));
            assertThat(putResult.getRaftCommandIndex())
                    .isPositive();

            GetConnectionStringsResult connectionStrings = store.maintenance().send(new GetConnectionStringsOperation());
            assertThat(connectionStrings.getRavenConnectionStrings())
                    .containsKey("r1")
                    .hasSize(1);

            assertThat(connectionStrings.getSqlConnectionStrings())
                    .containsKey("s1")
                    .hasSize(1);

            GetConnectionStringsResult ravenOnly = store.maintenance().send(new GetConnectionStringsOperation("r1", ConnectionStringType.RAVEN));
            assertThat(ravenOnly.getRavenConnectionStrings())
                    .containsKey("r1")
                    .hasSize(1);
            assertThat(ravenOnly.getSqlConnectionStrings())
                    .isEmpty();

            GetConnectionStringsResult sqlOnly = store.maintenance().send(new GetConnectionStringsOperation("s1", ConnectionStringType.SQL));
            assertThat(sqlOnly.getRavenConnectionStrings())
                    .isEmpty();
            assertThat(sqlOnly.getSqlConnectionStrings())
                    .containsKey("s1")
                    .hasSize(1);

            RemoveConnectionStringResult removeResult = store.maintenance().send(new RemoveConnectionStringOperation<>(sqlOnly.getSqlConnectionStrings().values().stream().findFirst().get()));
            assertThat(removeResult.getRaftCommandIndex())
                    .isPositive();



            GetConnectionStringsResult afterDelete = store.maintenance().send(new GetConnectionStringsOperation("s1", ConnectionStringType.SQL));
            assertThat(afterDelete.getRavenConnectionStrings())
                    .isEmpty();
            assertThat(afterDelete.getSqlConnectionStrings())
                    .isEmpty();
        }
    }
}
