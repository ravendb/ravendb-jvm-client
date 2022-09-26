package net.ravendb.client.test.issues;

import net.ravendb.client.Constants;
import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.IMetadataDictionary;
import net.ravendb.client.infrastructure.entities.User;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

public class RavenDB_17996Test extends RemoteTestBase {

    @Test
    public void metadata_that_didnt_change_doesnt_cause_saveChanges() throws Exception {
        try (DocumentStore store = getDocumentStore()) {
            String id;

            try (IDocumentSession session = store.openSession()) {
                User user = new User();

                session.store(user);
                id = user.getId();
                session.saveChanges();
            }

            Consumer<Consumer<IMetadataDictionary>> assertRequestCountEqual = (Consumer<IMetadataDictionary> action) -> {
                try (IDocumentSession session = store.openSession()) {
                    User entity = session.load(User.class, id);

                    IMetadataDictionary metadataFor = session.advanced().getMetadataFor(entity);

                    int requestsBefore = session.advanced().getNumberOfRequests();

                    action.accept(metadataFor);
                    session.saveChanges();

                    assertThat(session.advanced().getNumberOfRequests())
                            .isEqualTo(requestsBefore);
                }
            };

            assertRequestCountEqual.accept(metadataFor -> {
                for (String s : metadataFor.keySet()) {
                }
            });

            assertRequestCountEqual.accept(metadataFor -> metadataFor.put("@collection", "Users"));
            assertRequestCountEqual.accept(metadataFor -> metadataFor.remove(Constants.Documents.Metadata.REFRESH));
        }
    }

    @Test
    public void metadata_that_changed_caused_saveChanges() throws Exception {
        try (DocumentStore store = getDocumentStore()) {
            String id;

            try (IDocumentSession session = store.openSession()) {
                User user = new User();

                session.store(user);
                id = user.getId();
                session.saveChanges();
            }

            Consumer<Consumer<IMetadataDictionary>> assertRequestCountNotEqual = (Consumer<IMetadataDictionary> action) -> {
                try (IDocumentSession session = store.openSession()) {
                    User entity = session.load(User.class, id);

                    IMetadataDictionary metadataFor = session.advanced().getMetadataFor(entity);

                    int requestsBefore = session.advanced().getNumberOfRequests();

                    action.accept(metadataFor);
                    session.saveChanges();

                    assertThat(session.advanced().getNumberOfRequests())
                            .isNotEqualTo(requestsBefore);
                }
            };

            assertRequestCountNotEqual.accept(metadataFor -> metadataFor.remove(Constants.Documents.Metadata.LAST_MODIFIED));
            assertRequestCountNotEqual.accept(metadataFor -> metadataFor.put("@1234", "Users"));
        }
    }

    @Test
    public void metadata_clear_caused_saveChanges() throws Exception {
        try (DocumentStore store = getDocumentStore()) {
            String id;

            try (IDocumentSession session = store.openSession()) {
                User user = new User();

                session.store(user);
                id = user.getId();

                IMetadataDictionary metadataFor = session.advanced().getMetadataFor(user);
                metadataFor.put("test", "1");

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                User entity = session.load(User.class, id);
                IMetadataDictionary metadataFor = session.advanced().getMetadataFor(entity);

                int requestsBefore = session.advanced().getNumberOfRequests();

                metadataFor.clear();

                session.saveChanges();

                assertThat(session.advanced().getNumberOfRequests())
                        .isNotEqualTo(requestsBefore);
            }

            try (IDocumentSession session = store.openSession()) {
                User user = session.load(User.class, id);
                IMetadataDictionary metadata = session.advanced().getMetadataFor(user);

                assertThat(metadata.containsKey("test"))
                        .isFalse();
            }
        }
    }

    @Test
    public void metadata_clear_that_didnt_change_doesnt_cause_saveChanges() throws Exception {
        try (DocumentStore store = getDocumentStore()) {
            String id;
            try (IDocumentSession session = store.openSession()) {

                User user = new User();
                session.store(user);

                id = user.getId();
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                User entity = session.load(User.class, id);

                IMetadataDictionary metadataFor = session.advanced().getMetadataFor(entity);
                int requestsBefore = session.advanced().getNumberOfRequests();

                metadataFor.clear();

                session.saveChanges();

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(requestsBefore);
            }
        }
    }
}
