package net.ravendb.client.test.client.sharding;

import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.Constants;
import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.exceptions.RavenException;
import net.ravendb.client.infrastructure.EnableOnServer;
import net.ravendb.client.infrastructure.entities.Company;
import net.ravendb.client.infrastructure.entities.Order;
import net.ravendb.client.infrastructure.entities.User;
import net.ravendb.client.serverwide.DatabaseRecord;
import net.ravendb.client.serverwide.DatabaseTopology;
import net.ravendb.client.serverwide.operations.GetDatabaseRecordOperation;
import net.ravendb.client.serverwide.sharding.*;
import net.ravendb.client.util.ClientShardHelper;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@EnableOnServer(thresholdVersion = "6.2")
public class PrefixedShardingTest extends RemoteTestBase {

    private static final int NUMBER_OF_BUCKETS = 1024 * 1024;

    private IDocumentStore getShardedDocumentStore(PrefixedShardingSetting... prefixed) throws Exception {
        Consumer<DatabaseRecord> customize = record -> {
            ShardingConfiguration sharding = new ShardingConfiguration();
            Map<Integer, DatabaseTopology> shards = new HashMap<>();
            for (int i = 0; i < 3; i++) {
                shards.put(i, new DatabaseTopology());
            }
            sharding.setShards(shards);
            if (prefixed.length > 0) {
                sharding.setPrefixed(new ArrayList<>(Arrays.asList(prefixed)));
            }
            record.setSharding(sharding);
        };
        return getDocumentStore("test_db", false, null, customize);
    }

    private static PrefixedShardingSetting setting(String prefix, Integer... shards) {
        PrefixedShardingSetting setting = new PrefixedShardingSetting(prefix);
        setting.setShards(new ArrayList<>(Arrays.asList(shards)));
        return setting;
    }

    private static ShardingConfiguration getShardingConfiguration(IDocumentStore store) {
        return store.maintenance().server().send(new GetDatabaseRecordOperation(store.getDatabase())).getSharding();
    }

    private static void storeUsers(IDocumentStore store, String prefix, int count) {
        try (IDocumentSession session = store.openSession()) {
            for (int i = 0; i < count; i++) {
                session.store(new User(), prefix + i);
            }
            session.saveChanges();
        }
    }

    @Test
    public void shouldThrowOnAttemptToAddPrefixThatDoesntEndWithSlashOrComma() throws Exception {
        try (IDocumentStore store = getShardedDocumentStore()) {
            assertThatThrownBy(() -> store.maintenance().send(new AddPrefixedShardingSettingOperation(setting("asia", 1, 2))))
                    .isInstanceOf(RavenException.class)
                    .hasMessageContaining("Cannot add prefix 'asia' to ShardingConfiguration.Prefixed. In order to define sharding by prefix, the prefix string must end with '/' or '-' characters");
        }
    }

    @Test
    public void shouldThrowOnPrefixSettingWithNoShards() throws Exception {
        try (IDocumentStore store = getShardedDocumentStore()) {
            assertThatThrownBy(() -> store.maintenance().send(new AddPrefixedShardingSettingOperation(setting("users/"))))
                    .isInstanceOf(RavenException.class);

            store.maintenance().send(new AddPrefixedShardingSettingOperation(setting("users/", 0)));

            assertThat(getShardingConfiguration(store).getPrefixed()).hasSize(1);
        }
    }

    @Test
    public void canAddPrefixIfNoDocsStartingWith() throws Exception {
        try (IDocumentStore store = getShardedDocumentStore(setting("eu/", 0))) {
            storeUsers(store, "eu/items/", 10);

            ShardingConfiguration shardingConfiguration = getShardingConfiguration(store);
            assertThat(shardingConfiguration.getBucketRanges()).hasSize(4);

            store.maintenance().send(new AddPrefixedShardingSettingOperation(setting("asia/", 1, 2)));

            shardingConfiguration = getShardingConfiguration(store);
            assertThat(shardingConfiguration.getPrefixed()).hasSize(2);
            assertThat(shardingConfiguration.getBucketRanges()).hasSize(6);
            assertThat(shardingConfiguration.getBucketRanges().get(4).getBucketRangeStart()).isEqualTo(NUMBER_OF_BUCKETS * 2);
        }

        // check that we can add prefixes even if none were defined in database creation
        try (IDocumentStore newStore = getShardedDocumentStore()) {
            newStore.maintenance().send(new AddPrefixedShardingSettingOperation(setting("users/", 0, 2)));

            assertThat(getShardingConfiguration(newStore).getPrefixed()).hasSize(1);
        }
    }

    @Test
    public void shouldNotAllowToDeletePrefixIfWeHaveDocsStartingWith() throws Exception {
        try (IDocumentStore store = getShardedDocumentStore(setting("asia/", 1, 2))) {
            storeUsers(store, "asia/items/", 10);

            assertThatThrownBy(() -> store.maintenance().send(new DeletePrefixedShardingSettingOperation("asia/")))
                    .isInstanceOf(RavenException.class);
        }
    }

    @Test
    public void canDeletePrefixIfNoDocsStartingWith() throws Exception {
        try (IDocumentStore store = getShardedDocumentStore(setting("eu/", 0, 1))) {
            storeUsers(store, "asia/items/", 10);

            assertThat(getShardingConfiguration(store).getBucketRanges()).hasSize(5);

            store.maintenance().send(new DeletePrefixedShardingSettingOperation("eu/"));

            ShardingConfiguration shardingConfiguration = getShardingConfiguration(store);
            assertThat(shardingConfiguration.getPrefixed()).isEmpty();
            assertThat(shardingConfiguration.getBucketRanges()).hasSize(3);
        }
    }

    @Test
    public void canDeleteOnePrefixThenAddAnotherIfNoDocsStartingWith() throws Exception {
        try (IDocumentStore store = getShardedDocumentStore(setting("eu/", 0, 1), setting("us/", 1, 2))) {
            storeUsers(store, "asia/items/", 10);

            assertThat(getShardingConfiguration(store).getBucketRanges()).hasSize(7);

            store.maintenance().send(new DeletePrefixedShardingSettingOperation("eu/"));

            ShardingConfiguration shardingConfiguration = getShardingConfiguration(store);
            assertThat(shardingConfiguration.getPrefixed()).hasSize(1);
            assertThat(shardingConfiguration.getBucketRanges()).hasSize(5);

            store.maintenance().send(new AddPrefixedShardingSettingOperation(setting("africa/", 0, 2)));

            shardingConfiguration = getShardingConfiguration(store);
            assertThat(shardingConfiguration.getPrefixed()).hasSize(2);
            List<ShardBucketRange> ranges = shardingConfiguration.getBucketRanges();
            assertThat(ranges).hasSize(7);
            assertThat(ranges.get(3).getBucketRangeStart()).isEqualTo(NUMBER_OF_BUCKETS);
            assertThat(ranges.get(4).getBucketRangeStart()).isEqualTo(NUMBER_OF_BUCKETS * 3 / 2);
            assertThat(ranges.get(5).getBucketRangeStart()).isEqualTo(NUMBER_OF_BUCKETS * 2);
            assertThat(ranges.get(6).getBucketRangeStart()).isEqualTo(NUMBER_OF_BUCKETS * 5 / 2);
        }
    }

    @Test
    public void canUpdatePrefixSetting() throws Exception {
        try (IDocumentStore store = getShardedDocumentStore(setting("eu/", 0, 1), setting("us/", 1, 2))) {
            storeUsers(store, "eu/users/", 10);

            List<ShardBucketRange> bucketRanges = getShardingConfiguration(store).getBucketRanges();
            assertThat(bucketRanges).hasSize(7);

            // update 'eu/' prefix setting : add shard #2
            store.maintenance().send(new UpdatePrefixedShardingSettingOperation(setting("eu/", 0, 1, 2)));

            ShardingConfiguration shardingConfiguration = getShardingConfiguration(store);
            assertThat(shardingConfiguration.getPrefixed()).hasSize(2);
            assertThat(shardingConfiguration.getPrefixed().get(1).getPrefix()).isEqualTo("eu/");
            assertThat(shardingConfiguration.getPrefixed().get(1).getShards()).hasSize(3);

            // shard #2 should get no bucket ranges for the 'eu/' prefix
            assertThat(shardingConfiguration.getBucketRanges()).hasSize(bucketRanges.size());
            for (int index = 0; index < bucketRanges.size(); index++) {
                assertThat(shardingConfiguration.getBucketRanges().get(index).getBucketRangeStart())
                        .isEqualTo(bucketRanges.get(index).getBucketRangeStart());
                assertThat(shardingConfiguration.getBucketRanges().get(index).getShardNumber())
                        .isEqualTo(bucketRanges.get(index).getShardNumber());
            }

            // we cannot remove shard #1 because there are bucket ranges mapped to shard #1 for this prefix
            assertThatThrownBy(() -> store.maintenance().send(new UpdatePrefixedShardingSettingOperation(setting("us/", 2))))
                    .isInstanceOf(RavenException.class);
        }
    }

    @Test
    public void prefixesOperationsShouldBeCaseInsensitive() throws Exception {
        try (IDocumentStore store = getShardedDocumentStore(setting("Users/", 0, 1), setting("Companies/", 0, 1, 2))) {
            store.maintenance().send(new UpdatePrefixedShardingSettingOperation(setting("users/", 0, 1, 2)));

            ShardingConfiguration shardingConfiguration = getShardingConfiguration(store);
            assertThat(shardingConfiguration.getPrefixed()).hasSize(2);
            assertThat(shardingConfiguration.getPrefixed().get(0).getPrefix()).isEqualTo("Users/");
            assertThat(shardingConfiguration.getPrefixed().get(0).getShards()).containsExactly(0, 1, 2);

            store.maintenance().send(new DeletePrefixedShardingSettingOperation("COMPANIES/"));

            assertThat(getShardingConfiguration(store).getPrefixed()).hasSize(1);

            store.maintenance().send(new AddPrefixedShardingSettingOperation(setting("products/", 2)));

            storeUsers(store, "Products/", 10);

            for (int shard = 0; shard < 3; shard++) {
                try (IDocumentSession session = store.openSession(ClientShardHelper.toShardName(store.getDatabase(), shard))) {
                    User[] docs = session.advanced().loadStartingWith(User.class, "PRODUCTS/");
                    assertThat(docs).hasSize(shard == 2 ? 10 : 0);
                }
            }

            assertThatThrownBy(() -> store.maintenance().send(new UpdatePrefixedShardingSettingOperation(setting("Products/", 1))))
                    .isInstanceOf(RavenException.class);
        }
    }

    @Test
    public void prefixedShardingOperationsValidateArguments() {
        assertThatThrownBy(() -> new DeletePrefixedShardingSettingOperation(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DeletePrefixedShardingSettingOperation(""))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AddPrefixedShardingSettingOperation(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new UpdatePrefixedShardingSettingOperation(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void canQueryByPrefix() throws Exception {
        try (IDocumentStore store = getShardedDocumentStore(setting("users/us/", 0), setting("users/eu/", 1), setting("users/asia/", 2))) {
            storeRegionUsers(store);

            try (IDocumentSession session = store.openSession()) {
                List<User> results = session.query(User.class)
                        .shardContext(s -> s.byPrefix("users/us/"))
                        .toList();
                assertThat(results).hasSize(3);

                List<User> results2 = session.query(User.class)
                        .shardContext(s -> s.byPrefix("users/eu/"))
                        .toList();
                assertThat(results2).hasSize(2);

                List<User> results3 = session.query(User.class)
                        .shardContext(s -> s.byDocumentId("users/asia/"))
                        .toList();
                assertThat(results3).hasSize(4);
            }

            try (IDocumentSession session = store.openSession()) {
                assertThat(session.advanced().documentQuery(User.class)
                        .shardContext(s -> s.byPrefix("users/us/"))
                        .toList()).hasSize(3);

                assertThat(session.advanced().documentQuery(User.class)
                        .shardContext(s -> s.byPrefix("users/eu/"))
                        .toList()).hasSize(2);

                assertThat(session.advanced().documentQuery(User.class)
                        .shardContext(s -> s.byPrefix("users/asia/"))
                        .toList()).hasSize(4);
            }
        }
    }

    @Test
    public void canQueryByPrefix_WithPrefixedDocsSpreadAcrossMultipleShards() throws Exception {
        try (IDocumentStore store = getShardedDocumentStore(setting("orders/", 0, 1), setting("products/", 1), setting("companies/", 2))) {
            try (IDocumentSession session = store.openSession()) {
                for (int i = 0; i < 100; i++) {
                    session.store(new Order(), "orders/" + i);
                }

                session.store(new User(), "products/1");
                session.store(new User(), "products/2");
                session.store(new Company(), "companies/1");
                session.store(new User(), "users/ayende");

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                List<Order> orders = session.query(Order.class)
                        .shardContext(s -> s.byPrefix("orders/"))
                        .toList();
                assertThat(orders).hasSize(100);

                List<Company> companies = session.query(Company.class)
                        .shardContext(s -> s.byDocumentId("COMPANIES/")) // should be case-insensitive
                        .toList();
                assertThat(companies).hasSize(1);
            }

            int numOfOrdersInShard1;
            try (IDocumentSession session = store.openSession(ClientShardHelper.toShardName(store.getDatabase(), 1))) {
                numOfOrdersInShard1 = session.query(Order.class).count();
                assertThat(numOfOrdersInShard1).isPositive();
            }

            try (IDocumentSession session = store.openSession()) {
                // all docs starting with 'products/' reside on shard #1 => query should return just the orders from shard #1
                List<Order> orders = session.query(Order.class)
                        .shardContext(s -> s.byPrefix("products/"))
                        .toList();
                assertThat(orders).hasSize(numOfOrdersInShard1);
            }
        }
    }

    @Test
    public void queryByPrefix_ShouldThrowOnNonExistingPrefix() throws Exception {
        try (IDocumentStore store = getShardedDocumentStore(setting("orders/", 0), setting("products/", 1), setting("companies/", 2))) {
            try (IDocumentSession session = store.openSession()) {
                session.store(new Order(), "orders/1");
                session.store(new Company(), "companies/1");
                session.store(new User(), "users/1");
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                assertThatThrownBy(() -> session.query(User.class)
                        .shardContext(s -> s.byPrefix("users/"))
                        .toList())
                        .isInstanceOf(RavenException.class)
                        .hasMessageContaining("Prefix 'users/' wasn't found in Sharding Configuration");
            }
        }
    }

    @Test
    public void canQueryByPrefix_Raw() throws Exception {
        try (IDocumentStore store = getShardedDocumentStore(setting("users/", 1))) {
            try (IDocumentSession session = store.openSession()) {
                for (int i = 0; i < 10; i++) {
                    session.store(new User(), "users/" + i);
                    session.store(new Order(), "orders/" + i);
                    session.store(new Company(), "companies/" + i);
                }
                session.saveChanges();
            }

            int numOfDocsInShard1;
            try (IDocumentSession session = store.openSession(ClientShardHelper.toShardName(store.getDatabase(), 1))) {
                numOfDocsInShard1 = session.advanced().rawQuery(ObjectNode.class, "from @all_docs").toList().size();
                assertThat(numOfDocsInShard1).isPositive();
            }

            try (IDocumentSession session = store.openSession()) {
                Map<String, Object> p = new HashMap<>();
                p.put("Prefixes", Collections.singletonList("users/"));

                List<ObjectNode> results = session.advanced().rawQuery(ObjectNode.class, "from @all_docs")
                        .addParameter(Constants.Documents.Querying.Sharding.SHARD_CONTEXT_PARAMETER_NAME, p)
                        .toList();
                assertThat(results).hasSize(numOfDocsInShard1);

                List<ObjectNode> users = session.advanced().rawQuery(ObjectNode.class, "from Users")
                        .addParameter(Constants.Documents.Querying.Sharding.SHARD_CONTEXT_PARAMETER_NAME, p)
                        .toList();
                assertThat(users).hasSize(10);
            }
        }
    }

    @Test
    public void canQueryByMultiplePrefixes() throws Exception {
        try (IDocumentStore store = getShardedDocumentStore(setting("users/asia/", 0), setting("users/eu/", 1), setting("users/us/", 2))) {
            storeRegionUsers(store);

            try (IDocumentSession session = store.openSession()) {
                List<User> results = session.query(User.class)
                        .shardContext(s -> s.byPrefixes(new String[]{"users/us/", "users/eu/"}))
                        .orderBy("id()")
                        .toList();
                assertRegionUsers(results);
            }

            try (IDocumentSession session = store.openSession()) {
                List<User> results = session.advanced().documentQuery(User.class)
                        .shardContext(s -> s.byPrefixes(Arrays.asList("users/us/", "users/eu/")))
                        .orderBy("id()")
                        .toList();
                assertRegionUsers(results);
            }
        }
    }

    private static void storeRegionUsers(IDocumentStore store) {
        try (IDocumentSession session = store.openSession()) {
            for (String id : new String[]{"users/us/1", "users/us/2", "users/us/3", "users/eu/1", "users/eu/2",
                    "users/asia/1", "users/asia/2", "users/asia/3", "users/asia/4"}) {
                session.store(new User(), id);
            }
            session.saveChanges();
        }
    }

    private static void assertRegionUsers(List<User> results) {
        assertThat(results).extracting(User::getId)
                .containsExactly("users/eu/1", "users/eu/2", "users/us/1", "users/us/2", "users/us/3");
    }
}
