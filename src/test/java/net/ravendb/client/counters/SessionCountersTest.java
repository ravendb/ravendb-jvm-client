package net.ravendb.client.counters;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.operations.counters.*;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.ISessionDocumentCounters;
import net.ravendb.client.infrastructure.entities.Company;
import net.ravendb.client.infrastructure.entities.Employee;
import net.ravendb.client.infrastructure.entities.Order;
import net.ravendb.client.infrastructure.entities.User;
import net.ravendb.client.serverwide.DatabaseRecord;
import net.ravendb.client.serverwide.operations.CreateDatabaseOperation;
import net.ravendb.client.serverwide.operations.DeleteDatabasesOperation;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class SessionCountersTest extends RemoteTestBase {

    @Test
    public void sessionIncrementCounter() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                User user1 = new User();
                user1.setName("Aviv1");

                User user2 = new User();
                user2.setName("Aviv2");

                session.store(user1, "users/1-A");
                session.store(user2, "users/2-A");
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                session.countersFor("users/1-A").increment("likes", 100);
                session.countersFor("users/1-A").increment("downloads", 500);
                session.countersFor("users/2-A").increment("votes", 1000);

                session.saveChanges();
            }

            List<CounterDetail> counters = store.operations().send(new GetCountersOperation("users/1-A", new String[]{"likes", "downloads"}))
                    .getCounters();

            assertThat(counters)
                    .hasSize(2);

            assertThat(counters.stream().filter(x -> x.getCounterName().equals("likes")).findFirst().get().getTotalValue())
                    .isEqualTo(100);

            assertThat(counters.stream().filter(x -> x.getCounterName().equals("downloads")).findFirst().get().getTotalValue())
                    .isEqualTo(500);

            counters = store.operations().send(new GetCountersOperation("users/2-A", new String[]{ "votes" }))
                    .getCounters();

            assertThat(counters)
                    .hasSize(1);

            assertThat(counters.stream().filter(x -> x.getCounterName().equals("votes")).findFirst().get().getTotalValue())
                    .isEqualTo(1000);
        }
    }

    @Test
    public void sessionDeleteCounter() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                User user1 = new User();
                user1.setName("Aviv1");

                User user2 = new User();
                user2.setName("Aviv2");

                session.store(user1, "users/1-A");
                session.store(user2, "users/2-A");
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                session.countersFor("users/1-A").increment("likes", 100);
                session.countersFor("users/1-A").increment("downloads", 500);
                session.countersFor("users/2-A").increment("votes", 1000);

                session.saveChanges();
            }

            List<CounterDetail> counters = store.operations()
                    .send(new GetCountersOperation("users/1-A", new String[]{"likes", "downloads"}))
                    .getCounters();

            assertThat(counters)
                    .hasSize(2);

            try (IDocumentSession session = store.openSession()) {
                session.countersFor("users/1-A").delete("likes");
                session.countersFor("users/1-A").delete("downloads");
                session.countersFor("users/2-A").delete("votes");

                session.saveChanges();
            }

            counters = store.operations()
                    .send(new GetCountersOperation("users/1-A", new String[]{ "likes", "downloads" }))
                    .getCounters();

            assertThat(counters)
                    .hasSize(2);
            assertThat(counters.get(0))
                    .isNull();
            assertThat(counters.get(1))
                    .isNull();

            counters = store.operations()
                    .send(new GetCountersOperation("users/2-A", new String[]{"votes"}))
                    .getCounters();

            assertThat(counters)
                    .hasSize(1);
            assertThat(counters.get(0))
                    .isNull();
        }
    }

    @Test
    public void sessionGetCounters() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                User user1 = new User();
                user1.setName("Aviv1");

                User user2 = new User();
                user2.setName("Aviv2");

                session.store(user1, "users/1-A");
                session.store(user2, "users/2-A");
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                session.countersFor("users/1-A").increment("likes", 100);
                session.countersFor("users/1-A").increment("downloads", 500);
                session.countersFor("users/2-A").increment("votes", 1000);

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                Map<String, Long> dic = session.countersFor("users/1-A").getAll();

                assertThat(dic)
                        .hasSize(2)
                        .containsEntry("likes", 100L)
                        .containsEntry("downloads", 500L);

                assertThat(session.countersFor("users/2-A").get("votes"))
                        .isEqualTo(1000);
            }

            try (IDocumentSession session = store.openSession()) {
                Map<String, Long> dic = session.countersFor("users/1-A").get(Arrays.asList("likes", "downloads"));
                assertThat(dic)
                        .hasSize(2)
                        .containsEntry("likes", 100L)
                        .containsEntry("downloads", 500L);
            }

            try (IDocumentSession session = store.openSession()) {
                Map<String, Long> dic = session.countersFor("users/1-A").get(Collections.singletonList("likes"));
                assertThat(dic)
                        .hasSize(1)
                        .containsEntry("likes", 100L);
            }
        }
    }

    @Test
    public void sessionGetCountersWithNonDefaultDatabase() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String dbName = "db-" + UUID.randomUUID().toString().substring(10);

            store.maintenance().server().send(new CreateDatabaseOperation(new DatabaseRecord(dbName)));

            try {

                try (IDocumentSession session = store.openSession(dbName)) {
                    User user1 = new User();
                    user1.setName("Aviv1");

                    User user2 = new User();
                    user2.setName("Aviv2");

                    session.store(user1, "users/1-A");
                    session.store(user2, "users/2-A");
                    session.saveChanges();
                }

                try (IDocumentSession session = store.openSession(dbName)) {
                    session.countersFor("users/1-A").increment("likes", 100);
                    session.countersFor("users/1-A").increment("downloads", 500);
                    session.countersFor("users/2-A").increment("votes", 1_000);

                    session.saveChanges();
                }

                try (IDocumentSession session = store.openSession(dbName)) {
                    Map<String, Long> dic = session.countersFor("users/1-A").getAll();

                    assertThat(dic)
                            .hasSize(2)
                            .containsEntry("likes", 100L)
                            .containsEntry("downloads", 500L);

                    Map<String, Long> x = session.countersFor("users/2-A").getAll();
                    Long val = session.countersFor("users/2-A").get("votes");
                    assertThat(val)
                            .isEqualTo(1000L);
                }
            } finally {
                store.maintenance().server().send(new DeleteDatabasesOperation(dbName, true));
            }
        }
    }

    @Test
    public void getCountersFor() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                User user1 = new User();
                user1.setName("Aviv1");
                session.store(user1, "users/1-A");

                User user2 = new User();
                user2.setName("Aviv2");
                session.store(user2, "users/2-A");

                User user3 = new User();
                user3.setName("Aviv3");
                session.store(user3, "users/3-A");
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                session.countersFor("users/1-A").increment("likes", 100);
                session.countersFor("users/1-A").increment("downloads", 100);
                session.countersFor("users/2-A").increment("votes", 1000);

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                User user = session.load(User.class, "users/1-A");
                List<String> counters = session.advanced().getCountersFor(user);

                assertThat(counters)
                        .hasSize(2)
                        .containsExactly("downloads", "likes");

                user = session.load(User.class, "users/2-A");
                counters = session.advanced().getCountersFor(user);

                assertThat(counters)
                        .hasSize(1)
                        .containsExactly("votes");

                user = session.load(User.class, "users/3-A");
                counters = session.advanced().getCountersFor(user);
                assertThat(counters)
                        .isNull();
            }
        }
    }

    @Test
    public void differentTypesOfCountersOperationsInOneSession() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                User user1 = new User();
                user1.setName("Aviv1");
                session.store(user1, "users/1-A");

                User user2 = new User();
                user2.setName("Aviv2");
                session.store(user2, "users/2-A");

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                session.countersFor("users/1-A").increment("likes", 100);
                session.countersFor("users/1-A").increment("downloads", 100);
                session.countersFor("users/2-A").increment("votes", 1000);

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                session.countersFor("users/1-A").increment("likes", 100);
                session.countersFor("users/1-A").delete("downloads");
                session.countersFor("users/2-A").increment("votes", -600);

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                Long val = session.countersFor("users/1-A").get("likes");
                assertThat(val)
                        .isEqualTo(200L);

                val = session.countersFor("users/1-A").get("downloads");
                assertThat(val)
                        .isNull();

                val = session.countersFor("users/2-A").get("votes");
                assertThat(val)
                        .isEqualTo(400);
            }
        }
    }

    @Test
    public void shouldThrow() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Aviv");
                session.store(user);
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                User user = session.load(User.class, "users/1-A");
                session.countersFor(user).increment("likes", 100);
                session.saveChanges();

                assertThat(session.countersFor(user).get("likes"))
                        .isEqualTo(100);
            }
        }
    }

    @Test
    public void sessionShouldTrackCounters() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Aviv");
                session.store(user, "users/1-A");
                session.countersFor("users/1-A").increment("likes", 100);

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                assertThat(session.advanced().getNumberOfRequests())
                        .isZero();

                Long val = session.countersFor("users/1-A").get("likes");
                assertThat(val)
                        .isEqualTo(100);
                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);

                session.countersFor("users/1-A").get("likes");
                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);
            }
        }
    }

    @Test
    public void sessionShouldKeepNullsInCountersCache() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Aviv");
                session.store(user, "users/1-A");
                session.countersFor("users/1-A").increment("likes", 100);
                session.countersFor("users/1-A").increment("dislikes", 200);
                session.countersFor("users/1-A").increment("downloads", 300);

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                Long val = session.countersFor("users/1-A").get("score");
                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);
                assertThat(val)
                        .isNull();

                val = session.countersFor("users/1-A").get("score");
                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);
                assertThat(val)
                        .isNull();

                Map<String, Long> dic = session.countersFor("users/1-A").getAll();
                //should not contain null value for "score"

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(2);
                assertThat(dic)
                        .hasSize(3)
                        .containsEntry("likes", 100L)
                        .containsEntry("dislikes", 200L)
                        .containsEntry("downloads", 300L);
            }
        }
    }

    @Test
    public void sessionShouldKnowWhenItHasAllCountersInCacheAndAvoidTripToServer_WhenUsingEntity() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Aviv");
                session.store(user, "users/1-A");
                session.countersFor("users/1-A").increment("likes", 100);
                session.countersFor("users/1-A").increment("dislikes", 200);
                session.countersFor("users/1-A").increment("downloads", 300);

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                User user = session.load(User.class, "users/1-A");
                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);

                ISessionDocumentCounters userCounters = session.countersFor(user);

                Long val = userCounters.get("likes");
                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(2);
                assertThat(val)
                        .isEqualTo(100);

                val = userCounters.get("dislikes");
                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(3);
                assertThat(val)
                        .isEqualTo(200);

                val = userCounters.get("downloads");
                // session should know at this point that it has all counters

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(4);
                assertThat(val)
                        .isEqualTo(300);

                Map<String, Long> dic = userCounters.getAll(); // should not go to server
                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(4);

                assertThat(dic)
                        .hasSize(3)
                        .containsEntry("likes", 100L)
                        .containsEntry("dislikes", 200L)
                        .containsEntry("downloads", 300L);

                val = userCounters.get("score"); //should not go to server
                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(4);
                assertThat(val)
                        .isNull();
            }
        }
    }

    @Test
    public void sessionShouldUpdateMissingCountersInCacheAndRemoveDeletedCounters_AfterRefresh() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Aviv");
                session.store(user, "users/1-A");
                session.countersFor("users/1-A").increment("likes", 100);
                session.countersFor("users/1-A").increment("dislikes", 200);
                session.countersFor("users/1-A").increment("downloads", 300);

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                User user = session.load(User.class, "users/1-A");
                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);

                ISessionDocumentCounters userCounters = session.countersFor(user);
                Map<String, Long> dic = userCounters.getAll();
                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(2);

                assertThat(dic)
                        .hasSize(3)
                        .containsEntry("likes", 100L)
                        .containsEntry("dislikes", 200L)
                        .containsEntry("downloads", 300L);

                try (IDocumentSession session2 = store.openSession()) {
                    session2.countersFor("users/1-A").increment("likes");
                    session2.countersFor("users/1-A").delete("dislikes");
                    session2.countersFor("users/1-A").increment("score", 1000); // new counter
                    session2.saveChanges();
                }

                session.advanced().refresh(user);

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(3);

                // Refresh updated the document in session,
                // cache should know that it's missing 'score' by looking
                // at the document's metadata and go to server again to get all.
                // this should override the cache entirely and therefor
                // 'dislikes' won't be in cache anymore

                dic = userCounters.getAll();
                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(4);

                assertThat(dic)
                        .hasSize(3)
                        .containsEntry("likes", 101L)
                        .containsEntry("downloads", 300L)
                        .containsEntry("score", 1000L);

                // cache should know that it got all and not go to server,
                // and it shouldn't have 'dislikes' entry anymore
                Long val = userCounters.get("dislikes");
                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(4);
                assertThat(val)
                        .isNull();
            }
        }
    }

    @Test
    public void sessionShouldUpdateMissingCountersInCacheAndRemoveDeletedCounters_AfterLoadFromServer() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Aviv");
                session.store(user, "users/1-A");
                session.countersFor("users/1-A").increment("likes", 100);
                session.countersFor("users/1-A").increment("dislikes", 200);
                session.countersFor("users/1-A").increment("downloads", 300);

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {

                ISessionDocumentCounters userCounters = session.countersFor("users/1-A");
                Map<String, Long> dic = userCounters.getAll();
                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);

                assertThat(dic)
                        .hasSize(3)
                        .containsEntry("likes", 100L)
                        .containsEntry("dislikes", 200L)
                        .containsEntry("downloads", 300L);

                try (IDocumentSession session2 = store.openSession()) {
                    session2.countersFor("users/1-A").increment("likes");
                    session2.countersFor("users/1-A").delete("dislikes");
                    session2.countersFor("users/1-A").increment("score", 1000); // new counter
                    session2.saveChanges();
                }

                User user = session.load(User.class, "users/1-A");

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(2);

                // Refresh updated the document in session,
                // cache should know that it's missing 'score' by looking
                // at the document's metadata and go to server again to get all.
                // this should override the cache entirely and therefor
                // 'dislikes' won't be in cache anymore

                dic = userCounters.getAll();
                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(3);

                assertThat(dic)
                        .hasSize(3)
                        .containsEntry("likes", 101L)
                        .containsEntry("downloads", 300L)
                        .containsEntry("score", 1000L);

                // cache should know that it got all and not go to server,
                // and it shouldn't have 'dislikes' entry anymore
                Long val = userCounters.get("dislikes");
                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(3);
                assertThat(val)
                        .isNull();
            }
        }
    }

    @Test
    public void sessionClearShouldClearCountersCache() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Aviv");
                session.store(user, "users/1-A");
                session.countersFor("users/1-A").increment("likes", 100);
                session.countersFor("users/1-A").increment("dislikes", 200);
                session.countersFor("users/1-A").increment("downloads", 300);

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                ISessionDocumentCounters userCounters = session.countersFor("users/1-A");
                Map<String, Long> dic = userCounters.getAll();
                assertThat(dic)
                        .hasSize(3)
                        .containsEntry("likes", 100L)
                        .containsEntry("dislikes", 200L)
                        .containsEntry("downloads", 300L);

                try (IDocumentSession session2 = store.openSession()) {
                    session2.countersFor("users/1-A").increment("likes");
                    session2.countersFor("users/1-A").delete("dislikes");
                    session2.countersFor("users/1-A").increment("score", 1000); // new counter
                    session2.saveChanges();
                }

                session.advanced().clear(); // should clear countersCache

                dic = userCounters.getAll(); // should go to server again
                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(2);

                assertThat(dic)
                        .hasSize(3)
                        .containsEntry("likes", 101L)
                        .containsEntry("downloads", 300L)
                        .containsEntry("score", 1000L);
            }
        }
    }

    @Test
    public void sessionEvictShouldRemoveEntryFromCountersCache() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Aviv");
                session.store(user, "users/1-A");
                session.countersFor("users/1-A").increment("likes", 100);
                session.countersFor("users/1-A").increment("dislikes", 200);
                session.countersFor("users/1-A").increment("downloads", 300);

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                User user = session.load(User.class, "users/1-A");
                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);

                ISessionDocumentCounters userCounters = session.countersFor("users/1-A");
                Map<String, Long> dic = userCounters.getAll();
                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(2);
                assertThat(dic)
                        .hasSize(3)
                        .containsEntry("likes", 100L)
                        .containsEntry("dislikes", 200L)
                        .containsEntry("downloads", 300L);

                try (IDocumentSession session2 = store.openSession()) {
                    session2.countersFor("users/1-A").increment("likes");
                    session2.countersFor("users/1-A").delete("dislikes");
                    session2.countersFor("users/1-A").increment("score", 1000); // new counter
                    session2.saveChanges();
                }

                session.advanced().evict(user);  // should remove 'users/1-A' entry from  CountersByDocId

                dic = userCounters.getAll(); // should go to server again
                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(3);

                assertThat(dic)
                        .hasSize(3)
                        .containsEntry("likes", 101L)
                        .containsEntry("downloads", 300L)
                        .containsEntry("score", 1000L);
            }
        }
    }

    @Test
    public void sessionShouldAlwaysLoadCountersFromCacheAfterGetAll() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Aviv");
                session.store(user, "users/1-A");
                session.countersFor("users/1-A").increment("likes", 100);
                session.countersFor("users/1-A").increment("dislikes", 200);
                session.countersFor("users/1-A").increment("downloads", 300);

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                Map<String, Long> dic = session.countersFor("users/1-A").getAll();
                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);

                assertThat(dic)
                        .hasSize(3)
                        .containsEntry("likes", 100L)
                        .containsEntry("dislikes", 200L)
                        .containsEntry("downloads", 300L);

                //should not go to server after GetAll() request
                Long val = session.countersFor("users/1-A").get("likes");
                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);
                assertThat(val)
                        .isEqualTo(100);

                val = session.countersFor("users/1-A").get("votes");
                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);
                assertThat(val)
                        .isNull();
            }
        }
    }

    @Test
    public void sessionShouldOverrideExistingCounterValuesInCacheAfterGetAll() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Aviv");
                session.store(user, "users/1-A");
                session.countersFor("users/1-A").increment("likes", 100);
                session.countersFor("users/1-A").increment("dislikes", 200);
                session.countersFor("users/1-A").increment("downloads", 300);

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                Long val = session.countersFor("users/1-A").get("likes");
                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);
                assertThat(val)
                        .isEqualTo(100);

                val = session.countersFor("users/1-A").get("score");
                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(2);
                assertThat(val)
                        .isNull();

                DocumentCountersOperation operation = new DocumentCountersOperation();
                operation.setDocumentId("users/1-A");
                operation.setOperations(Collections.singletonList(CounterOperation.create("likes", CounterOperationType.INCREMENT, 400)));

                CounterBatch counterBatch = new CounterBatch();
                counterBatch.setDocuments(Collections.singletonList(operation));

                store.operations().send(new CounterBatchOperation(counterBatch));

                Map<String, Long> dic = session.countersFor("users/1-A").getAll();
                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(3);
                assertThat(dic)
                        .hasSize(3)
                        .containsEntry("dislikes", 200L)
                        .containsEntry("downloads", 300L)
                        .containsEntry("likes", 500L);

                val = session.countersFor("users/1-A").get("score");

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(3);// null values should still be in cache
                assertThat(val)
                        .isNull();
            }
        }
    }

    @Test
    public void sessionIncrementCounterShouldUpdateCounterValueAfterSaveChanges() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Aviv");
                session.store(user, "users/1-A");
                session.countersFor("users/1-A").increment("likes", 100);
                session.countersFor("users/1-A").increment("dislikes", 300);

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                Long val = session.countersFor("users/1-A").get("likes");
                assertThat(val)
                        .isEqualTo(100L);
                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);

                session.countersFor("users/1-A").increment("likes", 50);  // should not increment the counter value in cache
                val = session.countersFor("users/1-A").get("likes");
                assertThat(val)
                        .isEqualTo(100L);
                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);

                session.countersFor("users/1-A").increment("dislikes", 200);  // should not add the counter to cache
                val = session.countersFor("users/1-A").get("dislikes"); // should go to server
                assertThat(val)
                        .isEqualTo(300);
                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(2);

                session.countersFor("users/1-A").increment("score", 1000);  // should not add the counter to cache
                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(2);

                // SaveChanges should updated counters values in cache
                // according to increment result
                session.saveChanges();
                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(3);

                // should not go to server for these
                val = session.countersFor("users/1-A").get("likes");
                assertThat(val)
                        .isEqualTo(150L);

                val = session.countersFor("users/1-A").get("dislikes");
                assertThat(val)
                        .isEqualTo(500L);

                val = session.countersFor("users/1-A").get("score");
                assertThat(val)
                        .isEqualTo(1000L);

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(3);
            }
        }
    }

    @Test
    public void sessionShouldRemoveCounterFromCacheAfterCounterDeletion() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Aviv");
                session.store(user, "users/1-A");
                session.countersFor("users/1-A").increment("likes", 100);

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                Long val = session.countersFor("users/1-A").get("likes");
                assertThat(val)
                        .isEqualTo(100L);
                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);

                session.countersFor("users/1-A").delete("likes");
                session.saveChanges();
                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(2);

                val = session.countersFor("users/1-A").get("likes");
                assertThat(val)
                        .isNull();
                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(3);
            }
        }
    }

    @Test
    public void sessionShouldRemoveCountersFromCacheAfterDocumentDeletion() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Aviv");
                session.store(user, "users/1-A");
                session.countersFor("users/1-A").increment("likes", 100);
                session.countersFor("users/1-A").increment("dislikes", 200);

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                Map<String, Long> dic = session.countersFor("users/1-A").get(Arrays.asList("likes", "dislikes"));

                assertThat(dic)
                        .hasSize(2)
                        .containsEntry("likes", 100L)
                        .containsEntry("dislikes", 200L);
                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);

                session.delete("users/1-A");
                session.saveChanges();

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(2);

                Long val = session.countersFor("users/1-A").get("likes");
                assertThat(val)
                        .isNull();

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(3);
            }
        }
    }

    @Test
    public void sessionIncludeSingleCounter() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Aviv");
                session.store(user, "users/1-A");

                session.countersFor("users/1-A").increment("likes", 100);
                session.countersFor("users/1-A").increment("dislikes", 200);

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                User user = session.load(User.class, "users/1-A", i -> i.includeCounter("likes"));
                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);

                Long counter = session.countersFor(user).get("likes");
                assertThat(counter)
                        .isEqualTo(100);

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);
            }
        }
    }

    @Test
    public void sessionChainedIncludeCounter() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                Company company = new Company();
                company.setName("HR");
                session.store(company, "companies/1-A");

                Order order = new Order();
                order.setCompany("companies/1-A");
                session.store(order, "orders/1-A");

                session.countersFor("orders/1-A").increment("likes", 100);
                session.countersFor("orders/1-A").increment("dislikes", 200);

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                Order order = session.load(Order.class, "orders/1-A", i -> i.includeCounter("likes").includeCounter("dislikes"));

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);

                Long counter = session.countersFor(order).get("likes");
                assertThat(counter)
                        .isEqualTo(100);

                counter = session.countersFor(order).get("dislikes");
                assertThat(counter)
                        .isEqualTo(200);

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);
            }
        }
    }

    @Test
    public void sessionChainedIncludeAndIncludeCounter() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                Company company = new Company();
                company.setName("HR");
                session.store(company, "companies/1-A");

                Employee employee = new Employee();
                employee.setFirstName("Aviv");
                session.store(employee, "employees/1-A");

                Order order = new Order();
                order.setCompany("companies/1-A");
                order.setEmployee("employees/1-A");
                session.store(order, "orders/1-A");

                session.countersFor("orders/1-A").increment("likes", 100);
                session.countersFor("orders/1-A").increment("dislikes", 200);
                session.countersFor("orders/1-A").increment("downloads", 300);

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                Order order = session.load(Order.class, "orders/1-A",
                        i -> i.includeCounter("likes")
                                .includeDocuments("company")
                                .includeCounter("dislikes")
                                .includeCounter("downloads")
                                .includeDocuments("employee"));

                Company company = session.load(Company.class, order.getCompany());
                assertThat(company.getName())
                        .isEqualTo("HR");

                Employee employee = session.load(Employee.class, order.getEmployee());
                assertThat(employee.getFirstName())
                        .isEqualTo("Aviv");

                Map<String, Long> dic = session.countersFor(order).getAll();
                assertThat(dic)
                        .hasSize(3)
                        .containsEntry("likes", 100L)
                        .containsEntry("dislikes", 200L)
                        .containsEntry("downloads", 300L);

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);
            }
        }
    }

    @Test
    public void sessionIncludeCounters() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                Company company = new Company();
                company.setName("HR");
                session.store(company, "companies/1-A");

                Order order = new Order();
                order.setCompany("companies/1-A");
                session.store(order, "orders/1-A");

                session.countersFor("orders/1-A").increment("likes", 100);
                session.countersFor("orders/1-A").increment("dislikes", 200);

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                Order order = session.load(Order.class, "orders/1-A", i -> i.includeDocuments("company").includeCounters(new String[]{"likes", "dislikes"}));

                Company company = session.load(Company.class, order.getCompany());
                assertThat(company.getName())
                        .isEqualTo("HR");

                Map<String, Long> dic = session.countersFor(order).getAll();
                assertThat(dic)
                        .hasSize(2)
                        .containsEntry("likes", 100L)
                        .containsEntry("dislikes", 200L);

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);
            }

        }
    }

    @Test
    public void sessionIncludeAllCounters() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                Company company = new Company();
                company.setName("HR");
                session.store(company, "companies/1-A");

                Order order = new Order();
                order.setCompany("companies/1-A");
                session.store(order, "orders/1-A");

                session.countersFor("orders/1-A").increment("likes", 100);
                session.countersFor("orders/1-A").increment("dislikes", 200);
                session.countersFor("orders/1-A").increment("downloads", 300);

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                Order order = session.load(Order.class, "orders/1-A", i -> i.includeDocuments("company").includeAllCounters());

                Company company = session.load(Company.class, order.getCompany());
                assertThat(company.getName())
                        .isEqualTo("HR");

                Map<String, Long> dic = session.countersFor(order).getAll();
                assertThat(dic)
                        .hasSize(3)
                        .containsEntry("likes", 100L)
                        .containsEntry("dislikes", 200L)
                        .containsEntry("downloads", 300L);

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);
            }

        }
    }

    @Test
    public void sessionIncludeSingleCounterAfterIncludeAllCountersShouldThrow() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                Company company = new Company();
                company.setName("HR");
                session.store(company, "companies/1-A");

                Order order = new Order();
                order.setCompany("companies/1-A");
                session.store(order, "orders/1-A");

                session.countersFor("orders/1-A").increment("likes", 100);
                session.countersFor("orders/1-A").increment("dislikes", 200);
                session.countersFor("orders/1-A").increment("downloads", 300);

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {

                assertThatThrownBy(() -> {
                    session.load(Order.class, "orders/1-A", i -> i.includeDocuments("company").includeAllCounters().includeCounter("likes"));
                }).isExactlyInstanceOf(IllegalStateException.class);
            }
        }
    }

    @Test
    public void sessionIncludeAllCountersAfterIncludeSingleCounterShouldThrow() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                Company company = new Company();
                company.setName("HR");
                session.store(company, "companies/1-A");

                Order order = new Order();
                order.setCompany("companies/1-A");
                session.store(order, "orders/1-A");

                session.countersFor("orders/1-A").increment("likes", 100);
                session.countersFor("orders/1-A").increment("dislikes", 200);
                session.countersFor("orders/1-A").increment("downloads", 300);

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {

                assertThatThrownBy(() -> {
                    session.load(Order.class, "orders/1-A", i -> i.includeDocuments("company").includeCounter("likes").includeAllCounters());
                }).isExactlyInstanceOf(IllegalStateException.class);
            }
        }
    }

    @Test
    public void sessionIncludeCountersShouldRegisterMissingCounters() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                Company company = new Company();
                company.setName("HR");
                session.store(company, "companies/1-A");

                Order order = new Order();
                order.setCompany("companies/1-A");
                session.store(order, "orders/1-A");

                session.countersFor("orders/1-A").increment("likes", 100);
                session.countersFor("orders/1-A").increment("dislikes", 200);
                session.countersFor("orders/1-A").increment("downloads", 300);

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                Order order = session.load(Order.class, "orders/1-A",
                        i -> i.includeDocuments("company")
                                .includeCounters(new String[]{"likes", "downloads", "dances"})
                                .includeCounter("dislikes")
                                .includeCounter("cats"));

                Company company = session.load(Company.class, order.getCompany());
                assertThat(company.getName())
                        .isEqualTo("HR");

                // should not go to server
                Map<String, Long> dic = session.countersFor(order).getAll();
                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);

                assertThat(dic)
                        .hasSize(5)
                        .containsEntry("likes", 100L)
                        .containsEntry("dislikes", 200L)
                        .containsEntry("downloads", 300L);

                //missing counters should be in cache
                assertThat(dic.get("dances"))
                        .isNull();
                assertThat(dic.get("cats"))
                        .isNull();

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);

            }
        }
    }

    @Test
    public void sessionIncludeCountersMultipleLoads() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                Company company1 = new Company();
                company1.setName("HR");
                session.store(company1, "companies/1-A");

                Order order1 = new Order();
                order1.setCompany("companies/1-A");
                session.store(order1, "orders/1-A");

                Company company2 = new Company();
                company2.setName("HP");
                session.store(company2, "companies/2-A");

                Order order2 = new Order();
                order2.setCompany("companies/2-A");
                session.store(order2, "orders/2-A");

                session.countersFor("orders/1-A").increment("likes", 100);
                session.countersFor("orders/1-A").increment("dislikes", 200);

                session.countersFor("orders/2-A").increment("score", 300);
                session.countersFor("orders/2-A").increment("downloads", 400);

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                Map<String, Order> orders = session.load(Order.class, Arrays.asList("orders/1-A", "orders/2-A"), i -> i.includeDocuments("company").includeAllCounters());

                Order order = orders.get("orders/1-A");
                Company company = session.load(Company.class, order.getCompany());
                assertThat(company.getName())
                        .isEqualTo("HR");

                Map<String, Long> dic = session.countersFor(order).getAll();
                assertThat(dic)
                        .hasSize(2)
                        .containsEntry("likes", 100L)
                        .containsEntry("dislikes", 200L);

                order = orders.get("orders/2-A");
                company = session.load(Company.class, order.getCompany());
                assertThat(company.getName())
                        .isEqualTo("HP");

                dic = session.countersFor(order).getAll();
                assertThat(dic)
                        .hasSize(2)
                        .containsEntry("score", 300L)
                        .containsEntry("downloads", 400L);

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);
            }
        }
    }
}
