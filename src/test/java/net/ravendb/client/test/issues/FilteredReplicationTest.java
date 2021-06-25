package net.ravendb.client.test.issues;

import net.ravendb.client.RavenTestHelper;
import net.ravendb.client.ReplicationTestBase;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.operations.attachments.CloseableAttachmentResult;
import net.ravendb.client.documents.operations.connectionStrings.PutConnectionStringOperation;
import net.ravendb.client.documents.operations.etl.RavenConnectionString;
import net.ravendb.client.documents.operations.replication.*;
import net.ravendb.client.documents.session.ForceRevisionStrategy;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.timeSeries.TimeSeriesValue;
import net.ravendb.client.infrastructure.GenerateCertificateOperation;
import net.ravendb.client.infrastructure.entities.User;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;

public class FilteredReplicationTest extends ReplicationTestBase {

    @Test
    public void seasame_st() throws Exception {

        try (DocumentStore hooper = getSecuredDocumentStore()) {
            try (DocumentStore bert = getSecuredDocumentStore()) {

                GenerateCertificateOperation.PullReplicationCertificate certificate
                        = hooper.maintenance().send(new GenerateCertificateOperation());

                try (IDocumentSession session = hooper.openSession()) {
                    session.store(Item.ofType("Eggs"), "menus/breakfast");
                    session.store(Item.ofName("Bird Seed Milkshake"), "recipes/bird-seed-milkshake");
                    session.store(Item.ofName("3 USD"), "prices/eastus/2");
                    session.store(Item.ofName("3 EUR"), "prices/eu/1");
                    session.saveChanges();
                }

                try (IDocumentSession s = bert.openSession()) {
                    s.store(Item.ofName("Candy"), "orders/bert/3");
                    s.saveChanges();
                }

                PullReplicationDefinition pullReplicationDefinition = new PullReplicationDefinition();
                pullReplicationDefinition.setName("Franchises");
                pullReplicationDefinition.setMode(EnumSet.of(PullReplicationMode.HUB_TO_SINK, PullReplicationMode.SINK_TO_HUB));
                pullReplicationDefinition.setWithFiltering(true);

                hooper.maintenance().send(new PutPullReplicationAsHubOperation(pullReplicationDefinition));

                ReplicationHubAccess replicationHubAccess = new ReplicationHubAccess();
                replicationHubAccess.setName("Franchises");
                replicationHubAccess.setAllowedSinkToHubPaths(new String[] { "orders/bert/*" });
                replicationHubAccess.setAllowedHubToSinkPaths(new String[]{ "menus/*", "prices/eastus/*", "recipes/*" });
                replicationHubAccess.setCertificateBase64(certificate.getPublicKey());

                hooper.maintenance().send(new RegisterReplicationHubAccessOperation("Franchises", replicationHubAccess));

                RavenConnectionString ravenConnectionString = new RavenConnectionString();
                ravenConnectionString.setDatabase(hooper.getDatabase());
                ravenConnectionString.setName("HopperConStr");
                ravenConnectionString.setTopologyDiscoveryUrls(hooper.getUrls());

                bert.maintenance().send(new PutConnectionStringOperation<>(ravenConnectionString));

                PullReplicationAsSink pullReplicationAsSink = new PullReplicationAsSink();
                pullReplicationAsSink.setName("sink");
                pullReplicationAsSink.setConnectionStringName("HopperConStr");
                pullReplicationAsSink.setCertificateWithPrivateKey(certificate.getCertificate());
                pullReplicationAsSink.setHubName("Franchises");
                pullReplicationAsSink.setMode(EnumSet.of(PullReplicationMode.HUB_TO_SINK, PullReplicationMode.SINK_TO_HUB));

                bert.maintenance().send(new UpdatePullReplicationAsSinkOperation(pullReplicationAsSink));

                assertThat(waitForDocumentToReplicate(bert, Item.class, "menus/breakfast", 10_000))
                        .isNotNull();
                assertThat(waitForDocumentToReplicate(bert, Item.class, "recipes/bird-seed-milkshake", 10_000))
                        .isNotNull();
                assertThat(waitForDocumentToReplicate(bert, Item.class, "prices/eastus/2", 10_000))
                        .isNotNull();
                assertThat(waitForDocumentToReplicate(hooper, Item.class, "orders/bert/3", 10_000))
                        .isNotNull();

                try (IDocumentSession session = bert.openSession()) {
                    assertThat(session.load(Item.class, "prices/eu/1"))
                            .isNull();
                }
            }
        }
    }

    public static class HeartRateMeasure {
        @TimeSeriesValue(idx = 0)
        private double heartRate;

        public double getHeartRate() {
            return heartRate;
        }

        public void setHeartRate(double heartRate) {
            this.heartRate = heartRate;
        }

        public static HeartRateMeasure create(double heartRate) {
            HeartRateMeasure heartRateMeasure = new HeartRateMeasure();
            heartRateMeasure.setHeartRate(heartRate);
            return heartRateMeasure;
        }
    }

    @Test
    public void can_pull_via_filtered_replication() throws Exception {
        try (IDocumentStore storeA = getSecuredDocumentStore()) {
            try (IDocumentStore storeB = getSecuredDocumentStore()) {
                String dbNameA = storeA.getDatabase();
                String dbNameB = storeB.getDatabase();

                GenerateCertificateOperation.PullReplicationCertificate certificate
                        = storeA.maintenance().send(new GenerateCertificateOperation());

                try (IDocumentSession s = storeA.openSession()) {
                    User user1 = new User();
                    user1.setName("German Shepherd");
                    s.store(user1, "users/ayende/dogs/arava");

                    User user2 = new User();
                    user2.setName("Gray/White");
                    s.store(user2, "users/pheobe");

                    User user3 = new User();
                    user3.setName("Oren");
                    s.store(user3, "users/ayende");

                    s.countersFor("users/ayende")
                            .increment("test");
                    s.countersFor("users/pheobe")
                            .increment("test");

                    s.timeSeriesFor(HeartRateMeasure.class, "users/pheobe")
                            .append(RavenTestHelper.utcToday(), HeartRateMeasure.create(34), "test/things/out");

                    s.timeSeriesFor(HeartRateMeasure.class, "users/ayende")
                            .append(RavenTestHelper.utcToday(), HeartRateMeasure.create(55), "test/things/out");

                    s.advanced().attachments().store("users/ayende", "test.bin", new ByteArrayInputStream("hello".getBytes(StandardCharsets.UTF_8)));
                    s.advanced().attachments().store("users/pheobe", "test.bin", new ByteArrayInputStream("hello".getBytes(StandardCharsets.UTF_8)));

                    s.advanced().revisions().forceRevisionCreationFor("users/ayende", ForceRevisionStrategy.NONE);
                    s.advanced().revisions().forceRevisionCreationFor("users/pheobe", ForceRevisionStrategy.NONE);

                    s.saveChanges();
                }

                try (IDocumentSession s = storeA.openSession()) {
                    s.load(User.class, "users/pheobe");
                    s.load(User.class, "users/ayende");
                }

                try (IDocumentSession s = storeA.openSession()) {
                    User user1 = new User();
                    user1.setName("Gray/White 2");
                    s.store(user1, "users/pheobe");

                    User user2 = new User();
                    user2.setName("Oren 2");
                    s.store(user2, "users/ayende");

                    s.advanced().revisions().forceRevisionCreationFor("users/ayende");
                    s.advanced().revisions().forceRevisionCreationFor("users/pheobe");

                    s.saveChanges();
                }

                try (IDocumentSession s = storeB.openSession()) {
                    s.load(User.class, "users/pheobe");
                    s.load(User.class, "users/ayende");
                }

                PullReplicationDefinition pullReplicationDefinition = new PullReplicationDefinition();
                pullReplicationDefinition.setName("pull");
                pullReplicationDefinition.setMode(EnumSet.of(PullReplicationMode.SINK_TO_HUB, PullReplicationMode.HUB_TO_SINK));
                pullReplicationDefinition.setWithFiltering(true);
                storeA.maintenance().send(new PutPullReplicationAsHubOperation(pullReplicationDefinition));

                ReplicationHubAccess replicationHubAccess = new ReplicationHubAccess();
                replicationHubAccess.setName("Arava");
                replicationHubAccess.setAllowedHubToSinkPaths(new String[] { "users/ayende", "users/ayende/*" });
                replicationHubAccess.setCertificateBase64(certificate.getPublicKey());

                storeA.maintenance().send(new RegisterReplicationHubAccessOperation("pull", replicationHubAccess));

                RavenConnectionString ravenConnectionString = new RavenConnectionString();
                ravenConnectionString.setDatabase(dbNameA);
                ravenConnectionString.setName(dbNameA + "ConStr");
                ravenConnectionString.setTopologyDiscoveryUrls(storeA.getUrls());

                storeB.maintenance().send(new PutConnectionStringOperation<>(ravenConnectionString));

                PullReplicationAsSink pullReplicationAsSink = new PullReplicationAsSink();
                pullReplicationAsSink.setConnectionStringName(dbNameA + "ConStr");
                pullReplicationAsSink.setCertificateWithPrivateKey(certificate.getCertificate());
                pullReplicationAsSink.setHubName("pull");

                storeB.maintenance().send(new UpdatePullReplicationAsSinkOperation(pullReplicationAsSink));

                assertThat(waitForDocumentToReplicate(storeB, User.class, "users/ayende", 10_000))
                        .isNotNull();

                try (IDocumentSession s = storeB.openSession()) {
                    assertThat(s.load(User.class, "users/pheobe"))
                            .isNull();
                    assertThat(s.advanced().revisions().get(User.class, "user/pheobe", DateUtils.addDays(RavenTestHelper.utcToday(), 1)))
                            .isNull();
                    assertThat(s.countersFor("users/pheobe").get("test"))
                            .isNull();
                    assertThat(s.timeSeriesFor(HeartRateMeasure.class, "users/pheobe").get())
                            .isNull();
                    try (CloseableAttachmentResult attachmentResult = s.advanced().attachments().get("users/pheobe", "test.bin")) {
                        assertThat(attachmentResult)
                                .isNull();
                    }

                    assertThat(s.load(User.class, "users/ayende/dogs/arava"))
                            .isNotNull();
                    assertThat(s.load(User.class, "users/ayende"))
                            .isNotNull();
                    assertThat(s.advanced().revisions().get(User.class, "users/ayende", DateUtils.addDays(RavenTestHelper.utcToday(), 1)))
                            .isNotNull();

                    assertThat(s.advanced().revisions().get(User.class, "users/ayende", DateUtils.addDays(RavenTestHelper.utcToday(), 1)))
                            .isNotNull();
                    assertThat(s.countersFor("users/ayende").get("test"))
                            .isNotNull();
                    assertThat(s.timeSeriesFor(HeartRateMeasure.class, "users/ayende").get())
                            .isNotEmpty();
                    try (CloseableAttachmentResult attachmentResult = s.advanced().attachments().get("users/ayende", "test.bin")) {
                        assertThat(attachmentResult)
                                .isNotNull();
                    }
                }

                try (IDocumentSession s = storeA.openSession()) {
                    s.delete("users/ayende/dogs/arava");
                    s.saveChanges();
                }

                waitForDocumentDeletion(storeB, "users/ayende/dogs/arava");

                try (IDocumentSession s = storeB.openSession()) {
                    assertThat(s.load(User.class, "users/pheobe"))
                            .isNull();
                    assertThat(s.load(User.class, "users/ayende/dogs/arava"))
                            .isNull();

                    assertThat(s.load(User.class, "users/ayende"))
                            .isNotNull();

                    assertThat(s.advanced().revisions().get(User.class, "users/ayende", DateUtils.addDays(RavenTestHelper.utcToday(), 1)))
                            .isNotNull();
                    assertThat(s.countersFor("users/ayende").get("test"))
                            .isNotNull();
                    assertThat(s.timeSeriesFor(HeartRateMeasure.class, "users/ayende").get())
                            .isNotEmpty();
                    try (CloseableAttachmentResult attachmentResult = s.advanced().attachments().get("users/ayende", "test.bin")) {
                        assertThat(attachmentResult)
                                .isNotNull();
                    }
                }
            }
        }
    }

    @Test
    public void can_push_via_filtered_replication() throws Exception {
        try (IDocumentStore storeA = getSecuredDocumentStore()) {
            try (IDocumentStore storeB = getSecuredDocumentStore()) {
                String dbNameA = storeA.getDatabase();
                String dbNameB = storeB.getDatabase();

                GenerateCertificateOperation.PullReplicationCertificate certificate
                        = storeA.maintenance().send(new GenerateCertificateOperation());

                try (IDocumentSession s = storeA.openSession()) {
                    User user1 = new User();
                    user1.setName("German Shepherd");
                    s.store(user1, "users/ayende/dogs/arava");

                    User user2 = new User();
                    user2.setName("Gray/White");
                    s.store(user2, "users/pheobe");

                    User user3 = new User();
                    user3.setName("Oren");
                    s.store(user3, "users/ayende");

                    s.countersFor("users/ayende")
                            .increment("test");
                    s.countersFor("users/pheobe")
                            .increment("test");

                    s.timeSeriesFor(HeartRateMeasure.class, "users/pheobe")
                            .append(RavenTestHelper.utcToday(), HeartRateMeasure.create(34), "test/things/out");

                    s.timeSeriesFor(HeartRateMeasure.class, "users/ayende")
                            .append(RavenTestHelper.utcToday(), HeartRateMeasure.create(55), "test/things/out");

                    s.advanced().attachments().store("users/ayende", "test.bin", new ByteArrayInputStream("hello".getBytes(StandardCharsets.UTF_8)));
                    s.advanced().attachments().store("users/pheobe", "test.bin", new ByteArrayInputStream("hello".getBytes(StandardCharsets.UTF_8)));

                    s.advanced().revisions().forceRevisionCreationFor("users/ayende", ForceRevisionStrategy.NONE);
                    s.advanced().revisions().forceRevisionCreationFor("users/pheobe", ForceRevisionStrategy.NONE);

                    s.saveChanges();
                }

                try (IDocumentSession s = storeA.openSession()) {
                    s.load(User.class, "users/pheobe");
                    s.load(User.class, "users/ayende");
                }

                try (IDocumentSession s = storeA.openSession()) {
                    User user1 = new User();
                    user1.setName("Gray/White 2");
                    s.store(user1, "users/pheobe");

                    User user2 = new User();
                    user2.setName("Oren 2");
                    s.store(user2, "users/ayende");

                    s.advanced().revisions().forceRevisionCreationFor("users/ayende");
                    s.advanced().revisions().forceRevisionCreationFor("users/pheobe");

                    s.saveChanges();
                }

                try (IDocumentSession s = storeA.openSession()) {
                    s.load(User.class, "users/pheobe");
                    s.load(User.class, "users/ayende");
                }

                PullReplicationDefinition pullReplicationDefinition = new PullReplicationDefinition();
                pullReplicationDefinition.setName("push");
                pullReplicationDefinition.setMode(EnumSet.of(PullReplicationMode.SINK_TO_HUB, PullReplicationMode.HUB_TO_SINK));
                pullReplicationDefinition.setWithFiltering(true);
                storeB.maintenance().send(new PutPullReplicationAsHubOperation(pullReplicationDefinition));

                ReplicationHubAccess replicationHubAccess = new ReplicationHubAccess();
                replicationHubAccess.setName("Arava");
                replicationHubAccess.setAllowedSinkToHubPaths(new String[] { "users/ayende", "users/ayende/*" });
                replicationHubAccess.setCertificateBase64(certificate.getPublicKey());
                storeB.maintenance().send(new RegisterReplicationHubAccessOperation("push", replicationHubAccess));

                RavenConnectionString ravenConnectionString = new RavenConnectionString();
                ravenConnectionString.setDatabase(dbNameB);
                ravenConnectionString.setName(dbNameB + "ConStr");
                ravenConnectionString.setTopologyDiscoveryUrls(storeA.getUrls());

                storeA.maintenance().send(new PutConnectionStringOperation<>(ravenConnectionString));

                PullReplicationAsSink pullReplicationAsSink = new PullReplicationAsSink();
                pullReplicationAsSink.setConnectionStringName(dbNameB + "ConStr");
                pullReplicationAsSink.setMode(EnumSet.of(PullReplicationMode.SINK_TO_HUB));
                pullReplicationAsSink.setCertificateWithPrivateKey(certificate.getCertificate());
                pullReplicationAsSink.setHubName("push");

                storeA.maintenance().send(new UpdatePullReplicationAsSinkOperation(pullReplicationAsSink));

                assertThat(waitForDocumentToReplicate(storeB, User.class, "users/ayende", 10_000))
                        .isNotNull();

                try (IDocumentSession s = storeB.openSession()) {
                    assertThat(s.load(User.class, "users/pheobe"))
                            .isNull();
                    assertThat(s.advanced().revisions().get(User.class, "user/pheobe", DateUtils.addDays(RavenTestHelper.utcToday(), 1)))
                            .isNull();
                    assertThat(s.countersFor("users/pheobe").get("test"))
                            .isNull();
                    assertThat(s.timeSeriesFor(HeartRateMeasure.class, "users/pheobe").get())
                            .isNull();
                    try (CloseableAttachmentResult attachmentResult = s.advanced().attachments().get("users/pheobe", "test.bin")) {
                        assertThat(attachmentResult)
                                .isNull();
                    }

                    assertThat(s.load(User.class, "users/ayende/dogs/arava"))
                            .isNotNull();
                    assertThat(s.load(User.class, "users/ayende"))
                            .isNotNull();
                    assertThat(s.advanced().revisions().get(User.class, "users/ayende", DateUtils.addDays(RavenTestHelper.utcToday(), 1)))
                            .isNotNull();

                    assertThat(s.advanced().revisions().get(User.class, "users/ayende", DateUtils.addDays(RavenTestHelper.utcToday(), 1)))
                            .isNotNull();
                    assertThat(s.countersFor("users/ayende").get("test"))
                            .isNotNull();
                    assertThat(s.timeSeriesFor(HeartRateMeasure.class, "users/ayende").get())
                            .isNotEmpty();
                    try (CloseableAttachmentResult attachmentResult = s.advanced().attachments().get("users/ayende", "test.bin")) {
                        assertThat(attachmentResult)
                                .isNotNull();
                    }
                }

                try (IDocumentSession s = storeA.openSession()) {
                    s.delete("users/ayende/dogs/arava");
                    s.saveChanges();
                }

                waitForDocumentDeletion(storeB, "users/ayende/dogs/arava");

                try (IDocumentSession s = storeB.openSession()) {
                    assertThat(s.load(User.class, "users/pheobe"))
                            .isNull();
                    assertThat(s.load(User.class, "users/ayende/dogs/arava"))
                            .isNull();

                    assertThat(s.load(User.class, "users/ayende"))
                            .isNotNull();

                    assertThat(s.advanced().revisions().get(User.class, "users/ayende", DateUtils.addDays(RavenTestHelper.utcToday(), 1)))
                            .isNotNull();
                    assertThat(s.countersFor("users/ayende").get("test"))
                            .isNotNull();
                    assertThat(s.timeSeriesFor(HeartRateMeasure.class, "users/ayende").get())
                            .isNotEmpty();
                    try (CloseableAttachmentResult attachmentResult = s.advanced().attachments().get("users/ayende", "test.bin")) {
                        assertThat(attachmentResult)
                                .isNotNull();
                    }
                }
            }
        }
    }

    public static class Item {
        private String type;
        private String name;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public static Item ofType(String type) {
            Item menu = new Item();
            menu.setType(type);

            return menu;
        }

        public static Item ofName(String name) {
            Item menu = new Item();
            menu.setName(name);

            return menu;
        }
    }
}
