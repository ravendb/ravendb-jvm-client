package net.ravendb.client.test.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.identity.HiLoIdGenerator;
import net.ravendb.client.documents.identity.MultiDatabaseHiLoIdGenerator;
import net.ravendb.client.documents.identity.NextId;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.infrastructure.entities.User;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

public class HiLoTest extends RemoteTestBase {

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
                ids.add(getNextId(hiLoKeyGenerator));

                hiloDoc.setMax(12);
                session.store(hiloDoc, null, "Raven/Hilo/users");
                session.saveChanges();

                for (int i = 0; i < 128; i++) {
                    long nextId = getNextId(hiLoKeyGenerator);
                    assertThat(ids)
                            .doesNotContain(nextId);
                    ids.add(nextId);
                }

                assertThat(new HashSet<>(ids))
                        .hasSize(ids.size());
            }
        }
    }
    private static long getNextId(HiLoIdGenerator idGenerator) {
        NextId nextId = idGenerator.getNextId();
        return nextId.getId();
    }

    @Test
    public void hiLoMultiDb() throws Exception {
        try (DocumentStore store = getDocumentStore()) {

            try (IDocumentSession session = store.openSession()) {
                HiloDoc hiloDoc = new HiloDoc();
                hiloDoc.setMax(64);
                session.store(hiloDoc, "Raven/Hilo/users");

                HiloDoc productsHilo = new HiloDoc();
                productsHilo.setMax(128);
                session.store(productsHilo, "Raven/Hilo/products");

                session.saveChanges();

                MultiDatabaseHiLoIdGenerator multiDbHilo = new MultiDatabaseHiLoIdGenerator(store);
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
    public void generate_HiLo_Ids() throws Exception {
        try (DocumentStore store = getDocumentStore()) {
            MultiDatabaseHiLoIdGenerator multiDbHiLo = new MultiDatabaseHiLoIdGenerator(store);

            ConcurrentHashMap<Long, Boolean> usersIds = new ConcurrentHashMap<>();
            ConcurrentHashMap<Long, Boolean> productsIds = new ConcurrentHashMap<>();

            int count = 10;

            CompletableFuture[] futures = IntStream.range(0, count).mapToObj(x -> CompletableFuture.runAsync(() -> {
                Long id = multiDbHiLo.generateNextIdFor(null, "Users");
                assertThat(usersIds)
                        .doesNotContainKey(id);
                usersIds.put(id, true);

                id = multiDbHiLo.generateNextIdFor(null, "Products");
                assertThat(productsIds)
                        .doesNotContainKey(id);
                productsIds.put(id, true);
            })).toArray(CompletableFuture[]::new);

            CompletableFuture.allOf(futures).get();

            assertThat(usersIds)
                    .hasSize(count);
            assertThat(productsIds)
                    .hasSize(count);

            futures = IntStream.range(0, count).mapToObj(x -> CompletableFuture.runAsync(() -> {
                long id = store.getHiLoIdGenerator().generateNextIdFor(null, "Users");
                assertThat(usersIds)
                        .doesNotContainKey(id);
                usersIds.put(id, true);

                id = store.getHiLoIdGenerator().generateNextIdFor(null, "Products");
                assertThat(productsIds)
                        .doesNotContainKey(id);
                productsIds.put(id, true);

                id = store.getHiLoIdGenerator().generateNextIdFor(null, User.class);
                assertThat(usersIds)
                        .doesNotContainKey(id);
                usersIds.put(id, true);

                id = store.getHiLoIdGenerator().generateNextIdFor(null, new Product());
                assertThat(productsIds)
                        .doesNotContainKey(id);
                productsIds.put(id, true);

                id = store.getHiLoIdGenerator().generateNextIdFor(null, new User());
                assertThat(usersIds)
                        .doesNotContainKey(id);
                usersIds.put(id, true);

                id = store.getHiLoIdGenerator().generateNextIdFor(null, Product.class);
                assertThat(productsIds)
                        .doesNotContainKey(id);
                productsIds.put(id, true);
            })).toArray(CompletableFuture[]::new);

            CompletableFuture.allOf(futures).get();

            assertThat(usersIds)
                    .hasSize(count * 4);
            assertThat(productsIds)
                    .hasSize(count * 4);

        }
    }

    @Test
    public void capacityShouldDouble() throws Exception {
        try (DocumentStore store = getDocumentStore()) {

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
        try (DocumentStore store = getDocumentStore()) {

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

    @SuppressWarnings("unchecked")
    @Test
    public void doesNotGetAnotherRangeWhenDoingParallelRequests() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {

            int parallelLevel = 32;

            List<User> users = IntStream.range(0, parallelLevel).mapToObj(x -> new User()).collect(Collectors.toList());

            CompletableFuture<Void>[] futures = IntStream.range(0, parallelLevel).mapToObj(x -> CompletableFuture.runAsync(() -> {
                User user = users.get(x);
                IDocumentSession session = store.openSession();
                session.store(user);
                session.saveChanges();
            })).toArray(CompletableFuture[]::new);

            CompletableFuture.allOf(futures).get();

            users.stream()
                    .map(User::getId)
                    .map(id -> id.split("/")[1])
                    .map(x -> x.split("-")[0])
                    .forEach(numericPart -> {
                        assertThat(Integer.valueOf(numericPart))
                                .isLessThan(33);
                    });
        }
    }
}
