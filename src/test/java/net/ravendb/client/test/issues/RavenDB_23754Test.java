package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.operations.CollectionStatistics;
import net.ravendb.client.documents.operations.DetailedCollectionStatistics;
import net.ravendb.client.documents.operations.GetCollectionStatisticsOperation;
import net.ravendb.client.documents.operations.GetDetailedCollectionStatisticsOperation;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.ISessionDocumentTimeSeries;
import net.ravendb.client.infrastructure.EnableOnServer;
import net.ravendb.client.infrastructure.entities.Address;
import net.ravendb.client.infrastructure.entities.Company;
import net.ravendb.client.infrastructure.entities.User;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

@EnableOnServer(thresholdVersion = "7.0.3")
public class RavenDB_23754Test extends RemoteTestBase {

    @Test
    public void getCollectionStatsTests() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                User user1 = new User();
                user1.setName("user1");
                session.store(user1, "users/1");

                User user2 = new User();
                user2.setName("user2");
                session.store(user2, "users/2");

                User user3 = new User();
                user3.setName("user3");
                session.store(user3, "users/3");

                Company company1 = new Company();
                company1.setName("com1");
                session.store(company1, "com/1");

                Company company2 = new Company();
                company2.setName("com2");
                session.store(company2, "com/2");

                Address address = new Address();
                address.setCity("city1");
                session.store(address, "add/1");

                session.saveChanges();

                session.advanced().revisions().forceRevisionCreationFor("users/1");
                session.advanced().revisions().forceRevisionCreationFor("com/1");

                session.delete("users/3");

                session.advanced().attachments().store("users/1", "hello.txt",
                        new ByteArrayInputStream("hello".getBytes(StandardCharsets.UTF_8)));

                session.countersFor("users/1").increment("likes", 5);
                session.countersFor("com/1").increment("views", 10);

                Date tsTime = new Date();
                ISessionDocumentTimeSeries ts = session.timeSeriesFor("users/1", "heartrate");
                ts.append(tsTime, 72, "wrist");
                session.saveChanges();

                ts.delete(DateUtils.addMinutes(tsTime, -5), DateUtils.addMinutes(tsTime, -1));
                session.saveChanges();
            }

            CollectionStatistics collectionStats = store.maintenance().send(new GetCollectionStatisticsOperation());

            assertThat(collectionStats.getCollections())
                    .hasSize(3);
            assertThat(collectionStats.getCountOfDocuments())
                    .isEqualTo(5);
            assertThat(collectionStats.getCountOfConflicts())
                    .isZero();

            DetailedCollectionStatistics detailedCollectionStats = store.maintenance().send(new GetDetailedCollectionStatisticsOperation());

            assertThat(detailedCollectionStats.getCollections())
                    .hasSize(3);
            assertThat(detailedCollectionStats.getCountOfDocuments())
                    .isEqualTo(5);
            assertThat(detailedCollectionStats.getCountOfConflicts())
                    .isZero();
            assertThat(detailedCollectionStats.getCountOfRevisionDocuments())
                    .isEqualTo(2);
            assertThat(detailedCollectionStats.getCountOfTombstones())
                    .isEqualTo(1);
            assertThat(detailedCollectionStats.getCountOfAttachments())
                    .isEqualTo(1);
            assertThat(detailedCollectionStats.getCountOfCounterEntries())
                    .isEqualTo(2);
            assertThat(detailedCollectionStats.getCountOfTimeSeriesSegments())
                    .isEqualTo(1);
            assertThat(detailedCollectionStats.getCountOfTimeSeriesDeletedRanges())
                    .isEqualTo(1);
            assertThat(detailedCollectionStats.getCountOfDocumentsConflicts())
                    .isZero();
            assertThat(detailedCollectionStats.getCollections().get("Users").getCountOfDocuments())
                    .isEqualTo(2);
            assertThat(detailedCollectionStats.getCollections().get("Companies").getCountOfDocuments())
                    .isEqualTo(2);
            assertThat(detailedCollectionStats.getCollections().get("Addresses").getCountOfDocuments())
                    .isEqualTo(1);

            // collection names are case insensitive, as in C#
            assertThat(detailedCollectionStats.getCollections().get("users").getCountOfDocuments())
                    .isEqualTo(2);
            assertThat(collectionStats.getCollections().get("companies"))
                    .isEqualTo(2);
        }
    }
}
