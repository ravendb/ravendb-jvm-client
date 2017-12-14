package net.ravendb.client.test.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.identity.HiLoIdGenerator;
import net.ravendb.client.documents.identity.MultiDatabaseHiLoIdGenerator;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.infrastructure.entities.User;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class HiLoTest extends RemoteTestBase { //TODO: extends replication test

    private static class HiloDoc {
        @JsonProperty("Max")
        private long max;

        public long getMax() {
            return max;
        }

        public void setMax(long max) {
            this.max = max;
        }
    }

    private static class Product {
        private String productName;

        public String getProductName() {
            return productName;
        }

        public void setProductName(String productName) {
            this.productName = productName;
        }
    }

    @Test
    public void hiloCanNotGoDown() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {

            try (IDocumentSession session = store.openSession()) {
                HiloDoc hiloDoc = new HiloDoc();
                hiloDoc.setMax(32);

                session.store(hiloDoc, "Raven/Hilo/users");
                session.saveChanges();

                HiLoIdGenerator hiLoKeyGenerator = new HiLoIdGenerator("users", store, store.getDatabase(), store.getConventions().getIdentityPartsSeparator());

                List<Long> ids = new ArrayList<>();
                ids.add(hiLoKeyGenerator.nextId());

                hiloDoc.setMax(12);
                session.store(hiloDoc, null, "Raven/Hilo/users");
                session.saveChanges();

                for (int i = 0; i < 128; i++) {
                    long nextId = hiLoKeyGenerator.nextId();
                    assertThat(ids)
                            .doesNotContain(nextId);
                    ids.add(nextId);
                }

                assertThat(new HashSet<>(ids))
                        .hasSize(ids.size());
            }
        }
    }

    @Test
    public void hiLoMultiDb() throws Exception {
        try (DocumentStore store = (DocumentStore) getDocumentStore()) {

            try (IDocumentSession session = store.openSession()) {
                HiloDoc hiloDoc = new HiloDoc();
                hiloDoc.setMax(64);
                session.store(hiloDoc, "Raven/Hilo/users");

                HiloDoc productsHilo = new HiloDoc();
                productsHilo.setMax(128);
                session.store(productsHilo, "Raven/Hilo/products");

                session.saveChanges();

                MultiDatabaseHiLoIdGenerator multiDbHilo = new MultiDatabaseHiLoIdGenerator(store, store.getConventions());
                String generateDocumentKey = multiDbHilo.generateDocumentId(null, new User());
                assertThat(generateDocumentKey)
                        .isEqualTo("users/65-A");

                generateDocumentKey = multiDbHilo.generateDocumentId(null, new Product());
                assertThat(generateDocumentKey)
                        .isEqualTo("products/129-A");

            }
        }
    }

    @Test
    public void capacityShouldDouble() throws Exception {
        try (DocumentStore store = (DocumentStore) getDocumentStore()) {

            HiLoIdGenerator hiLoIdGenerator = new HiLoIdGenerator("users", store, store.getDatabase(), store.getConventions().getIdentityPartsSeparator());

            try (IDocumentSession session = store.openSession()) {
                HiloDoc hiloDoc = new HiloDoc();
                hiloDoc.setMax(64);
                session.store(hiloDoc, "Raven/Hilo/users");
                session.saveChanges();

                for (int i = 0; i < 32; i++) {
                    hiLoIdGenerator.generateDocumentId(new User());
                }
            }

            try (IDocumentSession session = store.openSession()) {
                HiloDoc hiloDoc = session.load(HiloDoc.class, "Raven/Hilo/users");
                long max = hiloDoc.getMax();
                assertThat(max)
                        .isEqualTo(96);

                //we should be receiving a range of 64 now
                hiLoIdGenerator.generateDocumentId(new User());
            }

            try (IDocumentSession session = store.openSession()) {
                HiloDoc hiloDoc = session.load(HiloDoc.class, "Raven/Hilo/users");
                long max = hiloDoc.getMax();
                assertThat(max)
                        .isEqualTo(160);
            }
        }
    }

    @Test
    public void returnUnusedRangeOnClose() throws Exception {
        try (DocumentStore store = (DocumentStore) getDocumentStore()) {

            DocumentStore newStore = new DocumentStore();
            newStore.setUrls(store.getUrls());
            newStore.setDatabase(store.getDatabase());

            newStore.initialize();

            try (IDocumentSession session = newStore.openSession()) {
                HiloDoc hiloDoc = new HiloDoc();
                hiloDoc.setMax(32);
                session.store(hiloDoc, "Raven/Hilo/users");

                session.saveChanges();

                session.store(new User());
                session.store(new User());

                session.saveChanges();
            }

            newStore.close(); //on document store close, hilo-return should be called


            newStore = new DocumentStore();
            newStore.setUrls(store.getUrls());
            newStore.setDatabase(store.getDatabase());

            newStore.initialize();

            try (IDocumentSession session = newStore.openSession()) {
                HiloDoc hiloDoc = session.load(HiloDoc.class, "Raven/Hilo/users");
                long max = hiloDoc.getMax();
                assertThat(max)
                        .isEqualTo(34);
            }

            newStore.close(); //on document store close, hilo-return should be called
        }
    }
}
