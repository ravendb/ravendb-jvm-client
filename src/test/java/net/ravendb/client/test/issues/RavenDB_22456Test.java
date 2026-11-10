package net.ravendb.client.test.issues;

import net.ravendb.client.Constants;
import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.operations.revisions.ConfigureRevisionsOperation;
import net.ravendb.client.documents.operations.revisions.RevertRevisionsByIdOperation;
import net.ravendb.client.documents.operations.revisions.RevisionsCollectionConfiguration;
import net.ravendb.client.documents.operations.revisions.RevisionsConfiguration;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.exceptions.RavenException;
import net.ravendb.client.infrastructure.DisabledOnPullRequest;
import net.ravendb.client.infrastructure.EnableOnServer;
import net.ravendb.client.infrastructure.entities.User;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@EnableOnServer(thresholdVersion = "6.2")
public class RavenDB_22456Test extends RemoteTestBase {

    private static void setupRevisions(IDocumentStore store) {
        RevisionsCollectionConfiguration defaultConfig = new RevisionsCollectionConfiguration();
        defaultConfig.setDisabled(false);

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

    private static String revisionCv(IDocumentSession session, String id, int index) {
        return session.advanced().revisions().getMetadataFor(id).get(index)
                .getString(Constants.Documents.Metadata.CHANGE_VECTOR);
    }

    private static void storeAndModify(IDocumentStore store, User... users) {
        try (IDocumentSession session = store.openSession()) {
            for (User user : users) {
                session.store(user);
            }
            session.saveChanges();
        }

        for (int i = 0; i < 10; i++) {
            try (IDocumentSession session = store.openSession()) {
                for (User user : users) {
                    session.load(User.class, user.getId()).setName(user.getName() + "_" + i);
                }
                session.saveChanges();
            }
        }
    }

    @DisabledOnPullRequest
    @Test
    public void revertByDocumentEP() throws Exception {
        User user1 = user("Users/1-A", "Shahar1");
        User user2 = user("Users/2-B", "Shahar2");

        try (IDocumentStore store = getDocumentStore()) {
            setupRevisions(store);
            storeAndModify(store, user1, user2);

            String user1revertCv;

            try (IDocumentSession session = store.openSession()) {
                assertThat(session.advanced().revisions().getCountFor(user1.getId())).isEqualTo(11);
                assertThat(session.advanced().revisions().getCountFor(user2.getId())).isEqualTo(11);

                user1revertCv = revisionCv(session, user1.getId(), 5);
            }

            store.operations().send(new RevertRevisionsByIdOperation(user1.getId(), user1revertCv));

            try (IDocumentSession session = store.openSession()) {
                User u1 = session.load(User.class, user1.getId());
                assertThat(u1.getName()).isEqualTo("Shahar1_4");

                assertThat(session.advanced().revisions().getCountFor(user1.getId())).isEqualTo(12);
                assertThat(session.advanced().revisions().getCountFor(user2.getId())).isEqualTo(11);
            }
        }
    }

    @DisabledOnPullRequest
    @Test
    public void revertByDocumentEpMultipleDocs() throws Exception {
        User user1 = user("Users/1-A", "Shahar1");
        User user2 = user("Users/2-B", "Shahar2");
        User company1 = user("Companies/1-A", "RavenDB1");
        User company2 = user("Companies/2-B", "RavenDB2");

        try (IDocumentStore store = getDocumentStore()) {
            setupRevisions(store);
            storeAndModify(store, user1, user2, company1, company2);

            String user1revertCv;
            String company1revertCv;

            try (IDocumentSession session = store.openSession()) {
                assertThat(session.advanced().revisions().getCountFor(user1.getId())).isEqualTo(11);
                assertThat(session.advanced().revisions().getCountFor(user2.getId())).isEqualTo(11);

                user1revertCv = revisionCv(session, user1.getId(), 5);
                company1revertCv = revisionCv(session, company1.getId(), 5);
            }

            Map<String, String> idToChangeVector = new HashMap<>();
            idToChangeVector.put(user1.getId(), user1revertCv);
            idToChangeVector.put(company1.getId(), company1revertCv);
            store.operations().send(new RevertRevisionsByIdOperation(idToChangeVector));

            try (IDocumentSession session = store.openSession()) {
                assertThat(session.load(User.class, user1.getId()).getName()).isEqualTo("Shahar1_4");
                assertThat(session.advanced().revisions().getCountFor(user1.getId())).isEqualTo(12);

                assertThat(session.load(User.class, company1.getId()).getName()).isEqualTo("RavenDB1_4");
                assertThat(session.advanced().revisions().getCountFor(company1.getId())).isEqualTo(12);

                assertThat(session.load(User.class, user2.getId()).getName()).isEqualTo("Shahar2_9");
                assertThat(session.advanced().revisions().getCountFor(user2.getId())).isEqualTo(11);

                assertThat(session.load(User.class, company2.getId()).getName()).isEqualTo("RavenDB2_9");
                assertThat(session.advanced().revisions().getCountFor(company2.getId())).isEqualTo(11);
            }
        }
    }

    @DisabledOnPullRequest
    @Test
    public void revertByDocumentEpFail() throws Exception {
        User user1 = user("Users/1-A", "Shahar1");
        User user2 = user("Users/2-B", "Shahar2");

        try (IDocumentStore store = getDocumentStore()) {
            setupRevisions(store);
            storeAndModify(store, user1, user2);

            String user2revertCv;
            try (IDocumentSession session = store.openSession()) {
                assertThat(session.advanced().revisions().getCountFor(user1.getId())).isEqualTo(11);
                assertThat(session.advanced().revisions().getCountFor(user2.getId())).isEqualTo(11);

                user2revertCv = revisionCv(session, user2.getId(), 5);
            }

            assertThatThrownBy(() -> store.operations().send(
                    new RevertRevisionsByIdOperation(user1.getId(), "A:253-jyJQ+3eQ5kuHIGZ+aqSVow")))
                    .isInstanceOf(RavenException.class)
                    .hasMessageContaining("Revision with the cv \"A:253-jyJQ+3eQ5kuHIGZ+aqSVow\" doesn't exist (id: 'Users/1-A')");

            assertThatThrownBy(() -> store.operations().send(
                    new RevertRevisionsByIdOperation(user1.getId(), user2revertCv)))
                    .isInstanceOf(RavenException.class)
                    .hasMessageContaining("Revision with the cv \"" + user2revertCv
                            + "\" doesn't belong to the doc \"Users/1-A\" but to the doc \"Users/2-B\"");

            try (IDocumentSession session = store.openSession()) {
                assertThat(session.advanced().revisions().getCountFor(user1.getId())).isEqualTo(11);
            }
        }
    }

    @Test
    public void validatesArguments() {
        assertThatThrownBy(() -> new RevertRevisionsByIdOperation(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("idToChangeVector cannot be null.");

        assertThatThrownBy(() -> new RevertRevisionsByIdOperation(new HashMap<>()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("idToChangeVector must contain at least one item.");

        assertThatThrownBy(() -> new RevertRevisionsByIdOperation("", "A:1-x"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("id cannot be null or empty.");

        assertThatThrownBy(() -> new RevertRevisionsByIdOperation("users/1", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("cv cannot be null or empty.");
    }
}
