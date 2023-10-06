package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.IMetadataDictionary;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

public class RavenDB_17154Test extends RemoteTestBase {

    @Test
    public void invokeOnAfterConversionToEntityAfterTrackingEntityInSession() throws Exception {

        AtomicInteger listenerOneCalled = new AtomicInteger();
        AtomicInteger listenerOneDocExists = new AtomicInteger();
        AtomicInteger listenerTwoCalled = new AtomicInteger();
        AtomicInteger listenerTwoDocExists = new AtomicInteger();

        try (DocumentStore store = getDocumentStore()) {
            // Register listener 1
            store.addAfterConversionToEntityListener((sender, event) -> {
                listenerOneCalled.incrementAndGet();
                IMetadataDictionary metadata = event.getSession().getMetadataFor(event.getEntity());
                if (metadata != null) {
                    listenerOneDocExists.incrementAndGet();
                }
            });

            // Insert data
            try (IDocumentSession session = store.openSession()) {
                session.store(new Entity("bob", "bob"));
                session.saveChanges();
            }

            // FIRST LOAD
            try (IDocumentSession session = store.openSession()) {
                // Register listener 2
                session.advanced().addAfterConversionToEntityListener((sender, event) -> {
                    listenerTwoCalled.incrementAndGet();
                    IMetadataDictionary metadata = event.getSession().getMetadataFor(event.getEntity());
                    if (metadata != null) {
                        listenerTwoDocExists.incrementAndGet();
                    }
                });

                Entity entity = session.load(Entity.class, "bob");
                assertThat(entity.getId())
                        .isEqualTo("bob");
            }

            // SECOND LOAD
            try (IDocumentSession session = store.openSession()) {
                // Register listener 2
                session.advanced().addAfterConversionToEntityListener((sender, event) -> {
                    listenerTwoCalled.incrementAndGet();
                    IMetadataDictionary metadata = event.getSession().getMetadataFor(event.getEntity());
                    if (metadata != null) {
                        listenerTwoDocExists.incrementAndGet();
                    }
                });

                Entity entity = session.load(Entity.class, "bob");
                assertThat(entity.getId())
                        .isEqualTo("bob");
            }

            assertThat(listenerOneCalled.get())
                    .isEqualTo(2);
            assertThat(listenerTwoCalled.get())
                    .isEqualTo(2);
            assertThat(listenerOneDocExists.get())
                    .isEqualTo(2);
            assertThat(listenerTwoDocExists.get())
                    .isEqualTo(2);

        }
    }

    public static class Entity {
        private String id;
        private String name;

        public Entity() {
        }

        public Entity(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
