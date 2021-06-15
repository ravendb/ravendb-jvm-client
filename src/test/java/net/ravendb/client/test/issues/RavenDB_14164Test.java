package net.ravendb.client.test.issues;

import net.ravendb.client.RavenTestHelper;
import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.ISessionDocumentTimeSeries;
import net.ravendb.client.documents.session.loaders.ITimeSeriesIncludeBuilder;
import net.ravendb.client.documents.session.timeSeries.TimeSeriesEntry;
import net.ravendb.client.infrastructure.entities.User;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class RavenDB_14164Test extends RemoteTestBase {

    @Test
    public void canGetTimeSeriesWithIncludeTagDocuments() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String[] tags = new String[] { "watches/fitbit", "watches/apple", "watches/sony" };

            Date baseline = RavenTestHelper.utcToday();

            String documentId = "users/ayende";

            try (IDocumentSession session = store.openSession()) {
                session.store(new User(), documentId);

                Watch watch1 = new Watch();
                watch1.setName("FitBit");
                watch1.setAccuracy(0.855);
                session.store(watch1, tags[0]);

                Watch watch2 = new Watch();
                watch2.setName("Apple");
                watch2.setAccuracy(0.9);
                session.store(watch2, tags[1]);

                Watch watch3 = new Watch();
                watch3.setName("Sony");
                watch3.setAccuracy(0.78);
                session.store(watch3, tags[2]);

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                ISessionDocumentTimeSeries tsf = session.timeSeriesFor(documentId, "heartRate");

                for (int i = 0; i <= 120; i++) {
                    tsf.append(DateUtils.addMinutes(baseline, i), i, tags[i % 3]);
                }

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                TimeSeriesEntry[] getResults = session.timeSeriesFor(documentId, "heartRate")
                        .get(baseline, DateUtils.addHours(baseline, 2), builder -> builder.includeTags());

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);

                assertThat(getResults)
                        .hasSize(121);
                assertThat(getResults[0].getTimestamp())
                        .isEqualTo(baseline);
                assertThat(getResults[getResults.length - 1].getTimestamp())
                        .isEqualTo(DateUtils.addHours(baseline, 2));

                // should not go to server

                Map<String, Watch> tagDocuments = session.load(Watch.class, tags);
                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);

                // assert tag documents

                assertThat(tagDocuments)
                        .hasSize(3);

                Watch tagDoc = tagDocuments.get("watches/fitbit");
                assertThat(tagDoc.getName())
                        .isEqualTo("FitBit");
                assertThat(tagDoc.getAccuracy())
                        .isEqualTo(0.855);

                tagDoc = tagDocuments.get("watches/apple");
                assertThat(tagDoc.getName())
                        .isEqualTo("Apple");
                assertThat(tagDoc.getAccuracy())
                        .isEqualTo(0.9);

                tagDoc = tagDocuments.get("watches/sony");
                assertThat(tagDoc.getName())
                        .isEqualTo("Sony");
                assertThat(tagDoc.getAccuracy())
                        .isEqualTo(0.78);
            }
        }
    }

    @Test
    public void canGetTimeSeriesWithIncludeTagsAndParentDocument() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String[] tags = new String[] { "watches/fitbit", "watches/apple", "watches/sony" };
            Date baseline = RavenTestHelper.utcToday();

            String documentId = "users/ayende";
            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("ayende");

                session.store(user, documentId);

                Watch watch1 = new Watch();
                watch1.setName("FitBit");
                watch1.setAccuracy(0.855);
                session.store(watch1, tags[0]);

                Watch watch2 = new Watch();
                watch2.setName("Apple");
                watch2.setAccuracy(0.9);
                session.store(watch2, tags[1]);

                Watch watch3 = new Watch();
                watch3.setName("Sony");
                watch3.setAccuracy(0.78);
                session.store(watch3, tags[2]);

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                ISessionDocumentTimeSeries tsf = session.timeSeriesFor(documentId, "heartRate");

                for (int i = 0; i <= 120; i++) {
                    tsf.append(DateUtils.addMinutes(baseline, i), i, tags[i % 3]);
                }

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                TimeSeriesEntry[] getResults = session.timeSeriesFor(documentId, "heartRate")
                        .get(baseline, DateUtils.addHours(baseline, 2), builder -> builder.includeTags().includeDocument());

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);

                assertThat(getResults)
                        .hasSize(121);
                assertThat(getResults[0].getTimestamp())
                        .isEqualTo(baseline);
                assertThat(getResults[getResults.length - 1].getTimestamp())
                        .isEqualTo(DateUtils.addHours(baseline, 2));

                // should not go to server

                User user = session.load(User.class, documentId);
                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);
                assertThat(user.getName())
                        .isEqualTo("ayende");

                // should not go to server

                Map<String, Watch> tagDocuments = session.load(Watch.class, tags);
                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);

                // assert tag documents

                assertThat(tagDocuments)
                        .hasSize(3);

                Watch tagDoc = tagDocuments.get("watches/fitbit");
                assertThat(tagDoc.getName())
                        .isEqualTo("FitBit");
                assertThat(tagDoc.getAccuracy())
                        .isEqualTo(0.855);

                tagDoc = tagDocuments.get("watches/apple");
                assertThat(tagDoc.getName())
                        .isEqualTo("Apple");
                assertThat(tagDoc.getAccuracy())
                        .isEqualTo(0.9);

                tagDoc = tagDocuments.get("watches/sony");
                assertThat(tagDoc.getName())
                        .isEqualTo("Sony");
                assertThat(tagDoc.getAccuracy())
                        .isEqualTo(0.78);
            }
        }
    }

    @Test
    public void canGetTimeSeriesWithInclude_CacheNotEmpty() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String[] tags = new String[] { "watches/fitbit", "watches/apple", "watches/sony" };
            Date baseline = RavenTestHelper.utcToday();

            String documentId = "users/ayende";
            try (IDocumentSession session = store.openSession()) {
                session.store(new User(), documentId);

                Watch watch1 = new Watch();
                watch1.setName("FitBit");
                watch1.setAccuracy(0.855);
                session.store(watch1, tags[0]);

                Watch watch2 = new Watch();
                watch2.setName("Apple");
                watch2.setAccuracy(0.9);
                session.store(watch2, tags[1]);

                Watch watch3 = new Watch();
                watch3.setName("Sony");
                watch3.setAccuracy(0.78);
                session.store(watch3, tags[2]);

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                ISessionDocumentTimeSeries tsf = session.timeSeriesFor(documentId, "heartRate");

                for (int i = 0; i <= 120; i++) {
                    String tag;
                    if (i < 60) {
                        tag = tags[0];
                    } else if (i < 90) {
                        tag = tags[1];
                    } else {
                        tag = tags[2];
                    }

                    tsf.append(DateUtils.addMinutes(baseline, i), i, tag);
                }

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                // get [00:00 - 01:00]
                TimeSeriesEntry[] getResults = session.timeSeriesFor(documentId, "heartRate")
                        .get(baseline, DateUtils.addHours(baseline, 1));
                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);

                assertThat(getResults)
                        .hasSize(61);
                assertThat(getResults[0].getTimestamp())
                        .isEqualTo(baseline);
                assertThat(getResults[getResults.length - 1].getTimestamp())
                        .isEqualTo(DateUtils.addHours(baseline, 1));

                // get [01:15 - 02:00] with includes
                getResults = session.timeSeriesFor(documentId, "heartRate")
                        .get(DateUtils.addMinutes(baseline, 75), DateUtils.addHours(baseline, 2),
                                ITimeSeriesIncludeBuilder::includeTags);

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(2);

                assertThat(getResults)
                        .hasSize(46);
                assertThat(getResults[0].getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseline, 75));
                assertThat(getResults[getResults.length - 1].getTimestamp())
                        .isEqualTo(DateUtils.addHours(baseline, 2));

                // should not go to server

                Map<String, Watch> tagsDocuments = session.load(Watch.class, new String[]{tags[1], tags[2]});
                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(2);

                // assert tag documents

                assertThat(tagsDocuments)
                        .hasSize(2);

                Watch tagDoc = tagsDocuments.get("watches/apple");
                assertThat(tagDoc.getName())
                        .isEqualTo("Apple");
                assertThat(tagDoc.getAccuracy())
                        .isEqualTo(0.9);

                tagDoc = tagsDocuments.get("watches/sony");
                assertThat(tagDoc.getName())
                        .isEqualTo("Sony");
                assertThat(tagDoc.getAccuracy())
                        .isEqualTo(0.78);

                // "watches/fitbit" should not be in cache

                Watch watch = session.load(Watch.class, tags[0]);
                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(3);
                assertThat(watch.getName())
                        .isEqualTo("FitBit");
                assertThat(watch.getAccuracy())
                        .isEqualTo(0.855);
            }
        }
    }

    @Test
    public void canGetTimeSeriesWithInclude_CacheNotEmpty2() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String[] tags = new String[] { "watches/fitbit", "watches/apple", "watches/sony" };
            Date baseline = RavenTestHelper.utcToday();

            String documentId = "users/ayende";
            try (IDocumentSession session = store.openSession()) {
                session.store(new User(), documentId);

                Watch watch1 = new Watch();
                watch1.setName("FitBit");
                watch1.setAccuracy(0.855);
                session.store(watch1, tags[0]);

                Watch watch2 = new Watch();
                watch2.setName("Apple");
                watch2.setAccuracy(0.9);
                session.store(watch2, tags[1]);

                Watch watch3 = new Watch();
                watch3.setName("Sony");
                watch3.setAccuracy(0.78);
                session.store(watch3, tags[2]);

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                ISessionDocumentTimeSeries tsf = session.timeSeriesFor(documentId, "heartRate");

                for (int i = 0; i <= 120; i++) {
                    String tag;
                    if (i < 60) {
                        tag = tags[0];
                    } else if (i < 90) {
                        tag = tags[1];
                    } else {
                        tag = tags[2];
                    }

                    tsf.append(DateUtils.addMinutes(baseline, i), i, tag);
                }

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                // get [00:00 - 01:00]
                TimeSeriesEntry[] getResults = session.timeSeriesFor(documentId, "heartRate")
                        .get(baseline, DateUtils.addHours(baseline, 1));

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);

                assertThat(getResults)
                        .hasSize(61);
                assertThat(getResults[0].getTimestamp())
                        .isEqualTo(baseline);
                assertThat(getResults[getResults.length - 1].getTimestamp())
                        .isEqualTo(DateUtils.addHours(baseline, 1));

                // get [01:30 - 02:00]
                getResults = session.timeSeriesFor(documentId, "heartRate")
                        .get(DateUtils.addMinutes(baseline, 90), DateUtils.addHours(baseline, 2));

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(2);

                assertThat(getResults)
                        .hasSize(31);
                assertThat(getResults[0].getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseline, 90));
                assertThat(getResults[getResults.length - 1].getTimestamp())
                        .isEqualTo(DateUtils.addHours(baseline, 2));

                // get [01:00 - 01:15] with includes
                getResults = session.timeSeriesFor(documentId, "heartRate")
                        .get(DateUtils.addHours(baseline, 1), DateUtils.addMinutes(baseline, 75),
                                builder -> builder.includeTags());

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(3);

                assertThat(getResults)
                        .hasSize(16);
                assertThat(getResults[0].getTimestamp())
                        .isEqualTo(DateUtils.addHours(baseline, 1));
                assertThat(getResults[getResults.length - 1].getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseline, 75));

                // should not go to server

                Watch watch = session.load(Watch.class, tags[1]);
                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(3);

                assertThat(watch.getName())
                        .isEqualTo("Apple");

                assertThat(watch.getAccuracy())
                        .isEqualTo(0.9);

                // tags[0] and tags[2] should not be in cache

                watch = session.load(Watch.class, tags[0]);
                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(4);

                assertThat(watch.getName())
                        .isEqualTo("FitBit");
                assertThat(watch.getAccuracy())
                        .isEqualTo(0.855);

                watch = session.load(Watch.class, tags[2]);
                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(5);
                assertThat(watch.getName())
                        .isEqualTo("Sony");
                assertThat(watch.getAccuracy())
                        .isEqualTo(0.78);
            }
        }
    }

    @Test
    public void canGetMultipleRangesWithIncludes() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String[] tags = new String[] { "watches/fitbit", "watches/apple", "watches/sony" };
            Date baseline = RavenTestHelper.utcToday();

            String documentId = "users/ayende";

            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("ayende");
                session.store(user, documentId);

                Watch watch1 = new Watch();
                watch1.setName("FitBit");
                watch1.setAccuracy(0.855);
                session.store(watch1, tags[0]);

                Watch watch2 = new Watch();
                watch2.setName("Apple");
                watch2.setAccuracy(0.9);
                session.store(watch2, tags[1]);

                Watch watch3 = new Watch();
                watch3.setName("Sony");
                watch3.setAccuracy(0.78);
                session.store(watch3, tags[2]);

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                ISessionDocumentTimeSeries tsf = session.timeSeriesFor(documentId, "heartRate");

                for (int i = 0; i <= 120; i++) {
                    tsf.append(DateUtils.addMinutes(baseline, i), i, tags[i % 3]);
                }

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                // get range [00:00 - 00:30]
                TimeSeriesEntry[] getResults = session.timeSeriesFor(documentId, "heartRate")
                        .get(baseline, DateUtils.addMinutes(baseline, 30));

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);

                assertThat(getResults)
                        .hasSize(31);
                assertThat(getResults[0].getTimestamp())
                        .isEqualTo(baseline);
                assertThat(getResults[getResults.length - 1].getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseline, 30));

                // get range [00:45 - 00:60]

                getResults = session.timeSeriesFor(documentId, "heartRate")
                        .get(DateUtils.addMinutes(baseline, 45), DateUtils.addHours(baseline, 1));

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(2);
                assertThat(getResults)
                        .hasSize(16);
                assertThat(getResults[0].getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseline, 45));
                assertThat(getResults[getResults.length - 1].getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseline, 60));

                // get range [01:30 - 02:00]
                getResults = session.timeSeriesFor(documentId, "heartRate")
                        .get(DateUtils.addMinutes(baseline, 90), DateUtils.addHours(baseline, 2));

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(3);

                assertThat(getResults)
                        .hasSize(31);

                assertThat(getResults[0].getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseline, 90));
                assertThat(getResults[getResults.length - 1].getTimestamp())
                        .isEqualTo(DateUtils.addHours(baseline, 2));

                // get multiple ranges with includes
                // ask for entire range [00:00 - 02:00] with includes
                // this will go to server to get the "missing parts" - [00:30 - 00:45] and [01:00 - 01:30]

                getResults = session.timeSeriesFor(documentId, "heartRate")
                        .get(baseline, DateUtils.addHours(baseline, 2),
                                builder -> builder.includeTags().includeDocument());

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(4);
                assertThat(getResults)
                        .hasSize(121);

                assertThat(getResults[0].getTimestamp())
                        .isEqualTo(baseline);
                assertThat(getResults[getResults.length - 1].getTimestamp())
                        .isEqualTo(DateUtils.addHours(baseline, 2));

                // should not go to server

                User user = session.load(User.class, documentId);
                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(4);
                assertThat(user.getName())
                        .isEqualTo("ayende");

                // should not go to server
                Map<String, Watch> tagDocuments = session.load(Watch.class, tags);
                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(4);

                // assert tag documents

                assertThat(tagDocuments)
                        .hasSize(3);

                Watch tagDoc = tagDocuments.get("watches/fitbit");
                assertThat(tagDoc.getName())
                        .isEqualTo("FitBit");
                assertThat(tagDoc.getAccuracy())
                        .isEqualTo(0.855);

                tagDoc = tagDocuments.get("watches/apple");
                assertThat(tagDoc.getName())
                        .isEqualTo("Apple");
                assertThat(tagDoc.getAccuracy())
                        .isEqualTo(0.9);

                tagDoc = tagDocuments.get("watches/sony");
                assertThat(tagDoc.getName())
                        .isEqualTo("Sony");
                assertThat(tagDoc.getAccuracy())
                        .isEqualTo(0.78);
            }
        }
    }

    @Test
    public void canGetTimeSeriesWithIncludeTags_WhenNotAllEntriesHaveTags() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String[] tags = new String[] { "watches/fitbit", "watches/apple", "watches/sony" };
            Date baseline = RavenTestHelper.utcToday();

            String documentId = "users/ayende";

            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                session.store(user, documentId);

                Watch watch1 = new Watch();
                watch1.setName("FitBit");
                watch1.setAccuracy(0.855);
                session.store(watch1, tags[0]);

                Watch watch2 = new Watch();
                watch2.setName("Apple");
                watch2.setAccuracy(0.9);
                session.store(watch2, tags[1]);

                Watch watch3 = new Watch();
                watch3.setName("Sony");
                watch3.setAccuracy(0.78);
                session.store(watch3, tags[2]);

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                ISessionDocumentTimeSeries tsf = session.timeSeriesFor(documentId, "heartRate");

                for (int i = 0; i <= 120; i++) {
                    String tag = i % 10 == 0
                            ? null
                            : tags[i % 3];
                    tsf.append(DateUtils.addMinutes(baseline, i), i, tag);
                }

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                TimeSeriesEntry[] getResults = session.timeSeriesFor(documentId, "heartRate")
                        .get(baseline, DateUtils.addHours(baseline, 2),
                                ITimeSeriesIncludeBuilder::includeTags);

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);

                assertThat(getResults)
                        .hasSize(121);
                assertThat(getResults[0].getTimestamp())
                        .isEqualTo(baseline);
                assertThat(getResults[getResults.length - 1].getTimestamp())
                        .isEqualTo(DateUtils.addHours(baseline, 2));

                // should not go to server
                Map<String, Watch> tagDocuments = session.load(Watch.class, tags);
                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);

                // assert tag documents

                assertThat(tagDocuments)
                        .hasSize(3);

                Watch tagDoc = tagDocuments.get("watches/fitbit");
                assertThat(tagDoc.getName())
                        .isEqualTo("FitBit");
                assertThat(tagDoc.getAccuracy())
                        .isEqualTo(0.855);

                tagDoc = tagDocuments.get("watches/apple");
                assertThat(tagDoc.getName())
                        .isEqualTo("Apple");
                assertThat(tagDoc.getAccuracy())
                        .isEqualTo(0.9);

                tagDoc = tagDocuments.get("watches/sony");
                assertThat(tagDoc.getName())
                        .isEqualTo("Sony");
                assertThat(tagDoc.getAccuracy())
                        .isEqualTo(0.78);
            }
        }
    }

    @Test
    public void includesShouldAffectTimeSeriesGetCommandEtag() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String[] tags = new String[] { "watches/fitbit", "watches/apple", "watches/sony" };
            Date baseline = RavenTestHelper.utcToday();

            String documentId = "users/ayende";

            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                session.store(user, documentId);

                Watch watch1 = new Watch();
                watch1.setName("FitBit");
                watch1.setAccuracy(0.855);
                session.store(watch1, tags[0]);

                Watch watch2 = new Watch();
                watch2.setName("Apple");
                watch2.setAccuracy(0.9);
                session.store(watch2, tags[1]);

                Watch watch3 = new Watch();
                watch3.setName("Sony");
                watch3.setAccuracy(0.78);
                session.store(watch3, tags[2]);

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                ISessionDocumentTimeSeries tsf = session.timeSeriesFor(documentId, "heartRate");

                for (int i = 0; i <= 120; i++) {
                    tsf.append(DateUtils.addMinutes(baseline, i), i, tags[i % 3]);
                }

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                TimeSeriesEntry[] getResults = session.timeSeriesFor(documentId, "heartRate")
                        .get(baseline, DateUtils.addHours(baseline, 2),
                                builder -> builder.includeTags());

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);

                assertThat(getResults)
                        .hasSize(121);
                assertThat(getResults[0].getTimestamp())
                        .isEqualTo(baseline);
                assertThat(getResults[getResults.length - 1].getTimestamp())
                        .isEqualTo(DateUtils.addHours(baseline, 2));

                // should not go to server

                Map<String, Watch> tagDocuments = session.load(Watch.class, tags);

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);

                // assert tag documents

                assertThat(tagDocuments)
                        .hasSize(3);

                Watch tagDoc = tagDocuments.get("watches/fitbit");
                assertThat(tagDoc.getName())
                        .isEqualTo("FitBit");
                assertThat(tagDoc.getAccuracy())
                        .isEqualTo(0.855);

                tagDoc = tagDocuments.get("watches/apple");
                assertThat(tagDoc.getName())
                        .isEqualTo("Apple");
                assertThat(tagDoc.getAccuracy())
                        .isEqualTo(0.9);

                tagDoc = tagDocuments.get("watches/sony");
                assertThat(tagDoc.getName())
                        .isEqualTo("Sony");
                assertThat(tagDoc.getAccuracy())
                        .isEqualTo(0.78);
            }

            try (IDocumentSession session = store.openSession()) {
                // update tags[0]

                Watch watch = session.load(Watch.class, tags[0]);
                watch.setAccuracy(watch.getAccuracy() + 0.05);
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                TimeSeriesEntry[] getResults = session.timeSeriesFor(documentId, "heartRate")
                        .get(baseline, DateUtils.addHours(baseline, 2),
                                builder -> builder.includeTags());

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);

                assertThat(getResults)
                        .hasSize(121);
                assertThat(getResults[0].getTimestamp())
                        .isEqualTo(baseline);
                assertThat(getResults[getResults.length - 1].getTimestamp())
                        .isEqualTo(DateUtils.addHours(baseline, 2));

                // should not go to server

                Map<String, Watch> tagDocuments = session.load(Watch.class, tags);

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);

                // assert tag documents

                assertThat(tagDocuments)
                        .hasSize(3);

                Watch tagDoc = tagDocuments.get("watches/fitbit");
                assertThat(tagDoc.getName())
                        .isEqualTo("FitBit");
                assertThat(tagDoc.getAccuracy())
                        .isEqualTo(0.905);
            }

            String newTag = "watches/google";

            try (IDocumentSession session = store.openSession()) {
                // add new watch

                Watch watch = new Watch();
                watch.setAccuracy(0.75);
                watch.setName("Google Watch");

                session.store(watch, newTag);

                // update a time series entry to have the new tag

                session.timeSeriesFor(documentId, "heartRate")
                        .append(DateUtils.addMinutes(baseline, 45), 90, newTag);

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                TimeSeriesEntry[] getResults = session.timeSeriesFor(documentId, "heartRate")
                        .get(baseline, DateUtils.addHours(baseline, 2),
                                builder -> builder.includeTags());

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);

                assertThat(getResults)
                        .hasSize(121);
                assertThat(getResults[0].getTimestamp())
                        .isEqualTo(baseline);
                assertThat(getResults[getResults.length - 1].getTimestamp())
                        .isEqualTo(DateUtils.addHours(baseline, 2));

                // should not go to server
                session.load(Watch.class, tags);
                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);

                // assert that newTag is in cache
                Watch watch = session.load(Watch.class, newTag);
                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);

                assertThat(watch.getName())
                        .isEqualTo("Google Watch");
                assertThat(watch.getAccuracy())
                        .isEqualTo(0.75);
            }
        }
    }

    public static class Watch {
        private String name;
        private double accuracy;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public double getAccuracy() {
            return accuracy;
        }

        public void setAccuracy(double accuracy) {
            this.accuracy = accuracy;
        }
    }
}
