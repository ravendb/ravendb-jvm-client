package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.operations.CollectionRevisionsStatistics;
import net.ravendb.client.documents.operations.GetCollectionRevisionsStatisticsOperation;
import net.ravendb.client.documents.operations.revisions.ConfigureRevisionsOperation;
import net.ravendb.client.documents.operations.revisions.RevisionsCollectionConfiguration;
import net.ravendb.client.documents.operations.revisions.RevisionsConfiguration;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.infrastructure.DisabledOnPullRequest;
import net.ravendb.client.infrastructure.EnableOnServer;
import net.ravendb.client.infrastructure.entities.Company;
import net.ravendb.client.infrastructure.entities.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@EnableOnServer(thresholdVersion = "7.0")
public class RavenDB_14963Test extends RemoteTestBase {

    @DisabledOnPullRequest
    @Test
    public void getCollectionRevisionsStatistics() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            RevisionsCollectionConfiguration defaultConfig = new RevisionsCollectionConfiguration();
            defaultConfig.setDisabled(false);
            RevisionsConfiguration configuration = new RevisionsConfiguration();
            configuration.setDefaultConfig(defaultConfig);
            store.maintenance().send(new ConfigureRevisionsOperation(configuration));

            try (IDocumentSession session = store.openSession()) {
                User u1 = new User();
                u1.setName("1");
                session.store(u1, "Users/1");
                session.saveChanges();
                u1.setName("11");
                session.saveChanges();

                User u2 = new User();
                u2.setName("2");
                session.store(u2, "Users/2");
                session.saveChanges();
                u2.setName("22");
                session.saveChanges();

                Company c1 = new Company();
                c1.setName("1");
                session.store(c1, "Company/1");
                session.saveChanges();
                c1.setName("11");
                session.saveChanges();

                Company c2 = new Company();
                c2.setName("2");
                session.store(c2, "Company/2");
                session.saveChanges();
                c2.setName("22");
                session.saveChanges();

                Company c3 = new Company();
                c3.setName("11");
                session.store(c3, "Company/11");
                session.saveChanges();
                c3.setName("1111");
                session.saveChanges();
            }

            CollectionRevisionsStatistics stats = store.maintenance().send(new GetCollectionRevisionsStatisticsOperation());
            assertThat(stats.getCountOfRevisions()).isEqualTo(10);
            assertThat(stats.getCollections())
                    .hasSize(2)
                    .containsEntry("Users", 4L)
                    .containsEntry("Companies", 6L);
        }
    }
}
