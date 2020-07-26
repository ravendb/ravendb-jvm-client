package net.ravendb.client.test.issues;

import net.ravendb.client.ReplicationTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.ISessionDocumentCounters;
import net.ravendb.client.infrastructure.entities.Company;
import net.ravendb.client.infrastructure.entities.User;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class RavenDB_15080Test extends ReplicationTestBase {

    @Test
    public void canSplitLowerCasedAndUpperCasedCounterNames() throws Exception {
        try (IDocumentStore storeA = getDocumentStore()) {
            try (IDocumentSession session = storeA.openSession()) {
                User user = new User();
                user.setName("Aviv1");
                session.store(user, "users/1");

                ISessionDocumentCounters countersFor = session.countersFor("users/1");

                for (int i = 0; i < 500; i++) {
                    String str = "abc" + i;
                    countersFor.increment(str);
                }

                session.saveChanges();
            }

            try (IDocumentSession session = storeA.openSession()) {
                ISessionDocumentCounters countersFor = session.countersFor("users/1");

                for (int i = 0; i < 500; i++) {
                    String str = "Xyz" + i;
                    countersFor.increment(str);
                }

                session.saveChanges();
            }
        }
    }

    @Test
    public void counterOperationsShouldBeCaseInsensitiveToCounterName() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Aviv");
                session.store(user, "users/1");

                session.countersFor("users/1").increment("abc");
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                // should NOT create a new counter
                session.countersFor("users/1").increment("ABc");
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                assertThat(session.countersFor("users/1").getAll())
                        .hasSize(1);
            }

            try (IDocumentSession session = store.openSession()) {
                // get should be case-insensitive to counter name

                Long val = session.countersFor("users/1").get("AbC");
                assertThat(val)
                        .isEqualTo(2);

                User doc = session.load(User.class, "users/1");
                List<String> countersNames = session.advanced().getCountersFor(doc);
                assertThat(countersNames)
                        .hasSize(1);
                assertThat(countersNames.get(0))
                        .isEqualTo("abc"); // metadata counter-names should preserve their original casing
            }

            try (IDocumentSession session = store.openSession()) {
                session.countersFor("users/1").increment("XyZ");
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                Long val = session.countersFor("users/1")
                        .get("xyz");
                assertThat(val)
                        .isEqualTo(1);

                User doc = session.load(User.class, "users/1");
                List<String> counterNames = session.advanced().getCountersFor(doc);
                assertThat(counterNames)
                        .hasSize(2);

                // metadata counter-names should preserve their original casing
                assertThat(counterNames.get(0))
                        .isEqualTo("abc");
                assertThat(counterNames.get(1))
                        .isEqualTo("XyZ");
            }

            try (IDocumentSession session = store.openSession()) {
                // delete should be case-insensitive to counter name

                session.countersFor("users/1").delete("aBC");
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                Long val = session.countersFor("users/1").get("abc");
                assertThat(val)
                        .isNull();

                User doc = session.load(User.class, "users/1");
                List<String> counterNames = session.advanced().getCountersFor(doc);
                assertThat(counterNames)
                        .hasSize(1)
                        .containsOnly("XyZ");
            }

            try (IDocumentSession session = store.openSession()) {
                session.countersFor("users/1").delete("xyZ");
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                Long val = session.countersFor("users/1").get("Xyz");
                assertThat(val)
                        .isNull();

                User doc = session.load(User.class, "users/1");
                List<String> counterNames = session.advanced().getCountersFor(doc);
                assertThat(counterNames)
                        .isNull();
            }
        }
    }

    @Test
    public void countersShouldBeCaseInsensitive() throws Exception {
        // RavenDB-14753

        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                Company company = new Company();
                company.setName("HR");

                session.store(company, "companies/1");
                session.countersFor(company).increment("Likes", 999);

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                Company company = session.load(Company.class, "companies/1");
                session.countersFor(company)
                        .delete("lIkEs");

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                Company company = session.load(Company.class, "companies/1");
                Map<String, Long> counters = session.countersFor(company).getAll();
                assertThat(counters)
                        .isEmpty();
            }
        }
    }

    @Test
    public void deletedCounterShouldNotBePresentInMetadataCounters() throws Exception {
        // RavenDB-14753

        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                Company company = new Company();
                company.setName("HR");

                session.store(company, "companies/1");
                session.countersFor(company).increment("Likes", 999);
                session.countersFor(company).increment("Cats", 999);

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                Company company = session.load(Company.class, "companies/1");
                session.countersFor(company)
                        .delete("lIkEs");

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                Company company = session.load(Company.class, "companies/1");
                company.setName("RavenDB");

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                Company company = session.load(Company.class, "companies/1");
                List<String> counters = session.advanced().getCountersFor(company);
                assertThat(counters)
                        .hasSize(1);
                assertThat(counters)
                        .containsOnly("Cats");
            }
        }
    }

    @Test
    public void getCountersForDocumentShouldReturnNamesInTheirOriginalCasing() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                session.store(new User(), "users/1");

                ISessionDocumentCounters countersFor = session.countersFor("users/1");
                countersFor.increment("AviV");
                countersFor.increment("Karmel");
                countersFor.increment("PAWEL");

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                // GetAll should return counter names in their original casing
                Map<String, Long> all = session.countersFor("users/1")
                        .getAll();

                assertThat(all)
                        .hasSize(3);

                Set<String> keys = all.keySet();
                assertThat(keys)
                        .contains("AviV")
                        .contains("Karmel")
                        .contains("PAWEL");
            }
        }
    }

    @Test
    public void canDeleteAndReInsertCounter() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                Company company = new Company();
                company.setName("HR");

                session.store(company, "companies/1");
                session.countersFor(company).increment("Likes", 999);
                session.countersFor(company).increment("Cats", 999);

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                Company company = session.load(Company.class, "companies/1");
                session.countersFor(company)
                        .delete("Likes");

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                Company company = session.load(Company.class, "companies/1");
                List<String> counters = session.advanced().getCountersFor(company);

                assertThat(counters)
                        .hasSize(1)
                        .contains("Cats");

                Long counter = session.countersFor(company)
                        .get("Likes");
                assertThat(counter)
                        .isNull();

                Map<String, Long> all = session.countersFor(company)
                        .getAll();

                assertThat(all)
                        .hasSize(1);
            }

            try (IDocumentSession session = store.openSession()) {
                session.countersFor("companies/1").increment("Likes");
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                Company company = session.load(Company.class, "companies/1");
                List<String> counters = session.advanced().getCountersFor(company);

                assertThat(counters)
                        .hasSize(2)
                        .contains("Cats")
                        .contains("Likes");

                Long counter = session.countersFor(company)
                        .get("Likes");
                assertThat(counter)
                        .isNotNull();
                assertThat(counter)
                        .isEqualTo(1);
            }
        }
    }

    @Test
    public void countersSessionCacheShouldBeCaseInsensitiveToCounterName() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                Company company = new Company();
                company.setName("HR");

                session.store(company, "companies/1");
                session.countersFor(company)
                        .increment("Likes", 333);
                session.countersFor(company)
                        .increment("Cats", 999);

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                Company company = session.load(Company.class, "companies/1");

                // the document is now tracked by the session,
                // so now counters-cache has access to '@counters' from metadata

                // searching for the counter's name in '@counters' should be done in a case insensitive manner
                // counter name should be found in '@counters' => go to server

                Long counter = session.countersFor(company)
                        .get("liKes");
                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(2);
                assertThat(counter)
                        .isNotNull();
                assertThat(counter)
                        .isEqualTo(333);

                counter = session.countersFor(company).get("cats");
                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(3);
                assertThat(counter)
                        .isNotNull();
                assertThat(counter)
                        .isEqualTo(999);

                counter = session.countersFor(company).get("caTS");
                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(3);
                assertThat(counter)
                        .isNotNull();
                assertThat(counter)
                        .isEqualTo(999);
            }
        }
    }
}
