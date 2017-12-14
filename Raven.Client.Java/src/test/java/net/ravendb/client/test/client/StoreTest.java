package net.ravendb.client.test.client;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.IMetadataDictionary;
import net.ravendb.client.infrastructure.entities.User;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class StoreTest extends RemoteTestBase {


    @Test
    public void storeDocument() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {

            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("RavenDB");
                session.store(user, "users/1");
                session.saveChanges();

                user = session.load(User.class, "users/1");
                assertThat(user)
                        .isNotNull();
                assertThat(user.getName())
                        .isEqualTo("RavenDB");
            }
        }
    }

    @Test
    public void storeDocuments() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {

            try (IDocumentSession session = store.openSession()) {
                User user1 = new User();
                user1.setName("RavenDB");
                session.store(user1, "users/1");

                User user2 = new User();
                user2.setName("Hibernating Rhinos");
                session.store(user2, "users/2");

                session.saveChanges();

                Map<String, User> users = session.load(User.class, "users/1", "users/2");
                assertThat(users)
                        .hasSize(2);
            }
        }
    }

    @Test
    public void notifyAfterStore() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            final IMetadataDictionary[] storeLevelCallBack = new IMetadataDictionary[1];
            final IMetadataDictionary[] sessionLevelCallback = new IMetadataDictionary[1];

            store.addAfterStoreListener(((sender, event) -> storeLevelCallBack[0] = event.getDocumentMetadata()));

            try (IDocumentSession session = store.openSession()) {

                session.advanced().addAfterStoreListener(((sender, event) ->
                        sessionLevelCallback[0] = event.getDocumentMetadata()));

                User user1 = new User();
                user1.setName("RavenDB");
                session.store(user1, "users/1");

                session.saveChanges();

                assertThat(session.advanced().isLoaded("users/1"))
                        .isTrue();

                assertThat(session.advanced().getChangeVectorFor(user1))
                        .isNotNull();
            }

            assertThat(storeLevelCallBack[0])
                    .isNotNull()
                    .isEqualTo(sessionLevelCallback[0]);

            assertThat(sessionLevelCallback[0])
                    .isNotNull();

            IMetadataDictionary iMetadataDictionary = sessionLevelCallback[0];
            for (Map.Entry<String, Object> entry : iMetadataDictionary.entrySet()) {
                assertThat(entry.getKey())
                        .isNotNull();
                assertThat(entry.getValue())
                        .isNotNull();
            }
        }
    }
}
