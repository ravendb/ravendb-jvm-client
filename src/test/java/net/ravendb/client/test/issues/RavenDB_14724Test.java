package net.ravendb.client.test.issues;

import net.ravendb.client.Constants;
import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.operations.revisions.ConfigureRevisionsOperation;
import net.ravendb.client.documents.operations.revisions.RevisionsConfiguration;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.IMetadataDictionary;
import net.ravendb.client.infrastructure.entities.User;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class RavenDB_14724Test extends RemoteTestBase {

    @Test
    public void deleteDocumentAndRevisions() throws Exception {
        User user = new User();
        user.setName("Raven");
        String id = "user/1";

        try (DocumentStore store = getDocumentStore()) {
            setupRevisions(store, true, 5);

            try (IDocumentSession session = store.openSession()) {
                session.store(user, id);
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                user = session.load(User.class, id);
                user.setAge(10);

                session.store(user);
                session.saveChanges();

                IMetadataDictionary metadata = session.advanced().getMetadataFor(user);

                assertThat(metadata.getString(Constants.Documents.Metadata.FLAGS))
                        .isEqualTo("HasRevisions");

                List<User> revisions = session.advanced().revisions().getFor(User.class, id);
                assertThat(revisions)
                        .hasSize(2);

                session.delete(id);
                session.saveChanges();

                RevisionsConfiguration configuration = new RevisionsConfiguration();
                configuration.setDefaultConfig(null);

                ConfigureRevisionsOperation operation = new ConfigureRevisionsOperation(configuration);
                store.maintenance().send(operation);
            }

            try (IDocumentSession session = store.openSession()) {
                session.store(user, id);
                session.saveChanges();

                setupRevisions(store, true, 5);
            }

            try (IDocumentSession session = store.openSession()) {
                user = session.load(User.class, id);
                List<User> revisions = session.advanced().revisions().getFor(User.class, id);
                assertThat(revisions)
                        .isEmpty();
            }
        }
    }
}
