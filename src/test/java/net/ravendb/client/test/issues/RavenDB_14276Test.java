package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.session.BeforeStoreEventArgs;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.IMetadataDictionary;
import net.ravendb.client.infrastructure.entities.User;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class RavenDB_14276Test extends RemoteTestBase {

    @Test
    public void can_Update_Metadata_With_Nested_Dictionary() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            store.addBeforeStoreListener((sender, event) -> onBeforeStore(event));

            String docId = "users/1";

            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Some document");

                session.store(user, docId);

                IMetadataDictionary metadata = session.advanced().getMetadataFor(user);
                metadata.put("Custom-Metadata", _dictionary);

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                User user = session.load(User.class, docId);
                user.setName("Updated document");
                session.saveChanges();
            }

            verifyData(store, docId);
        }
    }

    @Test
    public void can_Update_Metadata_With_Nested_Dictionary_Same_Session() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            store.addBeforeStoreListener((sender, event) -> onBeforeStore(event));

            String docId = "users/1";

            try (IDocumentSession session = store.openSession()) {
                User savedUser = new User();
                savedUser.setName("Some document");
                session.store(savedUser, docId);

                IMetadataDictionary metadata = session.advanced().getMetadataFor(savedUser);
                metadata.put("Custom-Metadata", _dictionary);

                session.saveChanges();

                User user = session.load(User.class, docId);
                user.setName("Updated document");
                session.saveChanges();
            }

            verifyData(store, docId);
        }
    }

    private static void onBeforeStore(BeforeStoreEventArgs eventArgs) {
        if (eventArgs.getDocumentMetadata().containsKey("Some-MetadataEntry")) {
            IMetadataDictionary metadata = eventArgs.getSession().getMetadataFor(eventArgs.getEntity());
            metadata.put("Some-MetadataEntry", "Updated");
        } else {
            eventArgs.getDocumentMetadata().put("Some-MetadataEntry", "Created");
        }
    }

    private static void verifyData(IDocumentStore store, String docId) {
        try (IDocumentSession session = store.openSession()) {
            User user = session.load(User.class, docId);
            assertThat(user.getName())
                    .isEqualTo("Updated document");

            IMetadataDictionary metadata = session.advanced().getMetadataFor(user);
            IMetadataDictionary dictionary = (IMetadataDictionary) metadata.get("Custom-Metadata");
            IMetadataDictionary nestedDictionary = (IMetadataDictionary) dictionary.get("123");

            assertThat(nestedDictionary.getLong("aaaa"))
                    .isEqualTo(1);

            nestedDictionary = (IMetadataDictionary) dictionary.get("321");
            assertThat(nestedDictionary.getLong("bbbb"))
                    .isEqualTo(2);
        }
    }

    private final Map<String, Map<String, Integer>> _dictionary;

    public RavenDB_14276Test() {
        _dictionary = new HashMap<>();
        Map<String, Integer> firstMap = new HashMap<>();
        firstMap.put("aaaa", 1);

        Map<String, Integer> secondMap = new HashMap<>();
        secondMap.put("bbbb", 2);

        _dictionary.put("123", firstMap);
        _dictionary.put("321", secondMap);
    }
}
