package net.ravendb.client.test.issues;

import net.ravendb.client.ReplicationTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.operations.connectionStrings.PutConnectionStringOperation;
import net.ravendb.client.documents.operations.etl.RavenConnectionString;
import net.ravendb.client.documents.operations.replication.ExternalReplication;
import net.ravendb.client.documents.operations.replication.UpdateExternalReplicationOperation;
import net.ravendb.client.documents.session.ForceRevisionStrategy;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.infrastructure.entities.User;
import net.ravendb.client.test.client.timeSeries.TimeSeriesTypedSessionTest;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.Calendar;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

public class RavenDB_15076Test extends ReplicationTestBase {

    @Test
    public void counters_and_force_revisions() throws Exception {
        Date today = DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH);

        try (IDocumentStore storeA = getDocumentStore()) {
            try (IDocumentStore storeB = getDocumentStore()) {
                try (IDocumentSession s = storeA.openSession()) {
                    User breed = new User();
                    breed.setName("German Shepherd");
                    s.store(breed, "users/ayende/dogs/arava");

                    User color = new User();
                    color.setName("Gray/White");
                    s.store(color, "users/pheobe");

                    User oren = new User();
                    oren.setName("Oren");
                    s.store(oren, "users/ayende");

                    s.countersFor("users/ayende")
                            .increment("test");
                    s.countersFor("users/pheobe")
                            .increment("test");

                    TimeSeriesTypedSessionTest.HeartRateMeasure heartRateMeasure1 = new TimeSeriesTypedSessionTest.HeartRateMeasure();
                    heartRateMeasure1.setHeartRate(34);
                    s.timeSeriesFor(TimeSeriesTypedSessionTest.HeartRateMeasure.class, "users/pheobe")
                            .append(today, heartRateMeasure1, "test/things/out");

                    TimeSeriesTypedSessionTest.HeartRateMeasure heartRateMeasure2 = new TimeSeriesTypedSessionTest.HeartRateMeasure();
                    heartRateMeasure2.setHeartRate(55);
                    s.timeSeriesFor(TimeSeriesTypedSessionTest.HeartRateMeasure.class, "users/ayende")
                            .append(today, heartRateMeasure2, "test/things/out");

                    s.advanced().attachments().store("users/ayende", "test.bin",
                            new ByteArrayInputStream("hello".getBytes()));
                    s.advanced().attachments().store("users/pheobe", "test.bin",
                            new ByteArrayInputStream("hello".getBytes()));

                    s.advanced().revisions().forceRevisionCreationFor("users/ayende", ForceRevisionStrategy.NONE);
                    s.advanced().revisions().forceRevisionCreationFor("users/pheobe", ForceRevisionStrategy.NONE);
                    s.saveChanges();
                }

                try (IDocumentSession s = storeA.openSession()) {
                    User color2 = new User();
                    color2.setName("Gray/White 2");
                    s.store(color2, "users/pheobe");

                    User user2 = new User();
                    user2.setName("Oren 2");
                    s.store(user2, "users/ayende");

                    s.advanced().revisions().forceRevisionCreationFor("users/ayende");
                    s.advanced().revisions().forceRevisionCreationFor("users/pheobe");
                    s.saveChanges();
                }

                RavenConnectionString ravenConnectionString = new RavenConnectionString();
                ravenConnectionString.setDatabase(storeB.getDatabase());
                ravenConnectionString.setName(storeB.getDatabase() + "ConStr");
                ravenConnectionString.setTopologyDiscoveryUrls(storeA.getUrls());

                PutConnectionStringOperation<RavenConnectionString> putConnectionStringOperation = new PutConnectionStringOperation<>(ravenConnectionString);
                storeA.maintenance().send(putConnectionStringOperation);

                ExternalReplication externalReplication = new ExternalReplication();
                externalReplication.setName("erpl");
                externalReplication.setConnectionStringName(storeB.getDatabase() + "ConStr");
                UpdateExternalReplicationOperation updateExternalReplicationOperation = new UpdateExternalReplicationOperation(externalReplication);
                storeA.maintenance().send(updateExternalReplicationOperation);

                assertThat(waitForDocumentToReplicate(storeB, User.class, "users/ayende", 10_000))
                        .isNotNull();
                assertThat(waitForDocumentToReplicate(storeB, User.class, "users/pheobe", 10_000))
                        .isNotNull();
            }
        }
    }
}
