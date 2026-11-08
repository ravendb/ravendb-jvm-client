package net.ravendb.client.test.issues;

import net.ravendb.client.Constants;
import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.operations.revisions.ConfigureRevisionsOperation;
import net.ravendb.client.documents.operations.revisions.DeleteRevisionsOperation;
import net.ravendb.client.documents.operations.revisions.RevisionsCollectionConfiguration;
import net.ravendb.client.documents.operations.revisions.RevisionsConfiguration;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.IMetadataDictionary;
import net.ravendb.client.infrastructure.EnableOnServer;
import net.ravendb.client.infrastructure.entities.Company;
import net.ravendb.client.infrastructure.entities.User;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@EnableOnServer(thresholdVersion = "6.2")
public class RavenDB_17494Test extends RemoteTestBase {

    private static List<String> getRevisionsCvs(IDocumentSession session, String id) {
        return session.advanced().revisions().getMetadataFor(id)
                .stream()
                .map(m -> m.getString(Constants.Documents.Metadata.CHANGE_VECTOR))
                .collect(Collectors.toList());
    }

    private static void setupRevisions(IDocumentStore store, Long maximumRevisionsToDeleteUponDocumentUpdate) {
        RevisionsCollectionConfiguration defaultConfig = new RevisionsCollectionConfiguration();
        defaultConfig.setDisabled(false);
        defaultConfig.setMaximumRevisionsToDeleteUponDocumentUpdate(maximumRevisionsToDeleteUponDocumentUpdate);

        RevisionsConfiguration configuration = new RevisionsConfiguration();
        configuration.setDefaultConfig(defaultConfig);

        store.maintenance().send(new ConfigureRevisionsOperation(configuration));
    }

    private static User user(String id, String name) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        return user;
    }

    private static Company company(String id, String name) {
        Company company = new Company();
        company.setId(id);
        company.setName(name);
        return company;
    }

    @Test
    public void deleteRevisionsManuallyEPTest() throws Exception {
        User user1 = user("Users/1-A", "Shahar1");
        User user2 = user("Users/2-B", "Shahar2");
        Company company1 = company("Companies/1-A", "Shahar1");
        Company company2 = company("Companies/2-B", "Shahar2");
        Company company3 = company("Companies/3-C", "Shahar3");

        try (IDocumentStore store = getDocumentStore()) {
            setupRevisions(store, null);

            try (IDocumentSession session = store.openSession()) {
                session.store(user1);
                session.store(user2);
                session.store(company1);
                session.store(company2);
                session.store(company3);
                session.saveChanges();
            }

            for (int i = 0; i < 10; i++) {
                try (IDocumentSession session = store.openSession()) {
                    session.load(User.class, user1.getId()).setName("Shahar1_" + i);
                    session.load(User.class, user2.getId()).setName("Shahar2_" + i);
                    session.load(Company.class, company1.getId()).setName("RavenDB1_" + i);
                    session.load(Company.class, company2.getId()).setName("RavenDB2_" + i);
                    session.load(Company.class, company3.getId()).setName("RavenDB3_" + i);

                    session.saveChanges();
                }

                // keep consecutive revisions at least 2 ms apart, so a millisecond-precision Date always fits between them
                Thread.sleep(2);
            }

            // The C# test samples DateTime.UtcNow on the client after the 6th and before the 10th update.
            // Java's Date has millisecond precision and the client clock is coarse on Windows (up to ~15 ms),
            // while the server stamps revisions with 100 ns precision, so client-side timestamps could
            // include/exclude a neighbouring revision. Derive the same range from the server's revision timestamps:
            // from = the update with i == 6, to = just after the update with i == 8.
            Date from;
            Date to;
            try (IDocumentSession session = store.openSession()) {
                List<Instant> company2RevisionTimes = session.advanced().revisions().getMetadataFor(company2.getId())
                        .stream()
                        .map(m -> Instant.parse(m.getString(Constants.Documents.Metadata.LAST_MODIFIED)))
                        .sorted()
                        .collect(Collectors.toList());
                assertThat(company2RevisionTimes).hasSize(11); // [0] = initial store, [i + 1] = update i

                from = new Date(company2RevisionTimes.get(7).toEpochMilli());
                to = new Date(company2RevisionTimes.get(9).toEpochMilli() + 1);
            }

            List<String> user1revisionsToDelete;

            try (IDocumentSession session = store.openSession()) {
                assertThat(session.advanced().revisions().getCountFor(user1.getId())).isEqualTo(11);
                assertThat(session.advanced().revisions().getCountFor(user2.getId())).isEqualTo(11);
                assertThat(session.advanced().revisions().getCountFor(company1.getId())).isEqualTo(11);
                assertThat(session.advanced().revisions().getCountFor(company2.getId())).isEqualTo(11);
                assertThat(session.advanced().revisions().getCountFor(company3.getId())).isEqualTo(11);

                List<String> u1revisionsCvs = getRevisionsCvs(session, user1.getId());
                user1revisionsToDelete = Arrays.asList(u1revisionsCvs.get(6), u1revisionsCvs.get(5), u1revisionsCvs.get(7));
            }

            DeleteRevisionsOperation.Result result = store.maintenance().send(
                    new DeleteRevisionsOperation(user1.getId(), user1revisionsToDelete));
            assertThat(result.getTotalDeletes()).isEqualTo(3);

            DeleteRevisionsOperation.Result result2 = store.maintenance().send(new DeleteRevisionsOperation(company1.getId()));
            assertThat(result2.getTotalDeletes()).isEqualTo(11);

            DeleteRevisionsOperation.Result result3 = store.maintenance().send(new DeleteRevisionsOperation(company2.getId(), from, to));
            assertThat(result3.getTotalDeletes()).isEqualTo(3);

            try (IDocumentSession session = store.openSession()) {
                assertThat(session.advanced().revisions().getCountFor(user1.getId())).isEqualTo(11 - 3);
                assertThat(session.advanced().revisions().getCountFor(user2.getId())).isEqualTo(11);

                assertThat(session.advanced().revisions().getCountFor(company1.getId())).isEqualTo(0);
                assertThat(session.advanced().revisions().getCountFor(company2.getId())).isEqualTo(11 - 3); // missing revisions 6, 7, 8
                assertThat(session.advanced().revisions().getCountFor(company3.getId())).isEqualTo(11);

                List<String> u1revisionsCvs = getRevisionsCvs(session, user1.getId());
                assertThat(u1revisionsCvs).doesNotContainAnyElementsOf(user1revisionsToDelete);
            }
        }
    }

    @Test
    public void deleteRevisionsManuallyEpMultipleIDsTest() throws Exception {
        Company company1 = company("Companies/1-A", "RavenDB_0");
        Company company2 = company("Companies/2-B", "RavenDB_0");
        Company company3 = company("Companies/3-C", "RavenDB_0");
        Company company4 = company("Companies/4-C", "RavenDB_0");

        try (IDocumentStore store = getDocumentStore()) {
            setupRevisions(store, 4L);

            try (IDocumentSession session = store.openSession()) {
                session.store(company1);
                session.store(company2);
                session.store(company3);
                session.store(company4);
                session.saveChanges();
            }

            modify(store, company1.getId(), 4);
            modify(store, company2.getId(), 12);
            modify(store, company3.getId(), 8);
            modify(store, company4.getId(), 12);

            try (IDocumentSession session = store.openSession()) {
                assertThat(session.advanced().revisions().getCountFor(company1.getId())).isEqualTo(4);
                assertThat(session.advanced().revisions().getCountFor(company2.getId())).isEqualTo(12);
                assertThat(session.advanced().revisions().getCountFor(company3.getId())).isEqualTo(8);
                assertThat(session.advanced().revisions().getCountFor(company4.getId())).isEqualTo(12);
            }

            DeleteRevisionsOperation.Result result = store.maintenance().send(new DeleteRevisionsOperation(
                    Arrays.asList(company1.getId(), company2.getId(), company3.getId(), company4.getId())));
            assertThat(result.getTotalDeletes()).isEqualTo(12 * 3);

            try (IDocumentSession session = store.openSession()) {
                assertThat(session.advanced().revisions().getCountFor(company1.getId())).isEqualTo(0);
                assertThat(session.advanced().revisions().getCountFor(company2.getId())).isEqualTo(0);
                assertThat(session.advanced().revisions().getCountFor(company3.getId())).isEqualTo(0);
                assertThat(session.advanced().revisions().getCountFor(company4.getId())).isEqualTo(0);
            }
        }
    }

    private static void modify(IDocumentStore store, String id, int upTo) {
        for (int i = 1; i < upTo; i++) {
            try (IDocumentSession session = store.openSession()) {
                session.load(Company.class, id).setName("RavenDB_" + i);
                session.saveChanges();
            }
        }
    }

    @Test
    public void validatesParameters() {
        assertThatThrownBy(() -> new DeleteRevisionsOperation((List<String>) null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("request 'DocumentIds' cannot be null or empty.");

        assertThatThrownBy(() -> new DeleteRevisionsOperation(new ArrayList<>()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("request 'DocumentIds' cannot be null or empty.");

        assertThatThrownBy(() -> new DeleteRevisionsOperation(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("request 'DocumentIds' contains null or empty ids.");

        Date now = new Date();
        assertThatThrownBy(() -> new DeleteRevisionsOperation("users/1", now, now))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("'To' must be greater than 'From'.");

        assertThatThrownBy(() -> new DeleteRevisionsOperation("users/1", Collections.singletonList("")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("request 'RevisionsChangeVectors' contains null or empty change-vectors.");
    }
}
