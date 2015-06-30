package net.ravendb.tests.issues;

import com.mysema.query.annotations.QueryEntity;
import net.ravendb.client.IDocumentSession;
import net.ravendb.client.IDocumentStore;
import net.ravendb.client.document.DocumentStore;
import net.ravendb.client.document.ShardedBulkInsertOperation;
import net.ravendb.client.shard.ShardStrategy;
import net.ravendb.client.shard.ShardedDocumentStore;
import net.ravendb.tests.bundles.replication.ReplicationBase;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class RavenDB_3420Test extends ReplicationBase {

    @Test
    public void bulkInseretSharded() throws InterruptedException {
        try (
            DocumentStore store1 = createStore();
            DocumentStore store2 = createStore()) {

            HashMap<String, IDocumentStore> shards = new HashMap<>();
            shards.put("Shard1", store1);
            shards.put("Shard2", store2);

            ShardStrategy shardStrategy = new ShardStrategy(shards);
            QRavenDB_3420Test_Profile p = QRavenDB_3420Test_Profile.profile;
            shardStrategy.shardingOn(p.location);

            try (ShardedDocumentStore shardedDocumentStore = new ShardedDocumentStore(shardStrategy)) {
                shardedDocumentStore.initialize();

                Profile profile1 = new Profile();
                profile1.setId("bulk1");
                profile1.setName("Hila");
                profile1.setLocation("Shard1");

                Profile profile2 = new Profile();
                profile2.setName("Jay");
                profile2.setLocation("Shard2");

                Profile profile3 = new Profile();
                profile3.setName("Jay");
                profile3.setLocation("Shard1");

                try (ShardedBulkInsertOperation bulkInsert = shardedDocumentStore.shardedBulkInsert()) {
                    bulkInsert.store(profile1);
                    bulkInsert.store(profile2);
                    bulkInsert.store(profile3);
                }
            }

            try (IDocumentStore store12 = new DocumentStore(store1.getUrl()).initialize()) {
                try (IDocumentSession session = store12.openSession()) {
                    Profile docs = session.load(Profile.class, "Shard1/bulk1");
                    Profile docs2 = session.load(Profile.class, "shard1/profiles/2");

                    List<Profile> totalDocs = session.query(Profile.class).toList();

                    assertEquals("Shard1", docs.getLocation());
                    assertEquals("Shard1", docs2.getLocation());
                    assertEquals(2, totalDocs.size());
                }
            }

            try (IDocumentStore store22 = new DocumentStore(store2.getUrl()).initialize()) {
                try (IDocumentSession session = store22.openSession()) {
                    Profile docs = session.load(Profile.class, "shard2/profiles/1");
                    List<Profile> totalDocs = session.query(Profile.class).toList();

                    assertEquals("Shard2", docs.getLocation());
                    assertEquals(1, totalDocs.size());
                }
            }
        }
    }

    @QueryEntity
    public static class Profile {
        private String id;
        private String name;
        private String location;

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

        public String getLocation() {
            return location;
        }

        public void setLocation(String location) {
            this.location = location;
        }
    }
}
