package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.indexes.AbstractIndexCreationTask;
import net.ravendb.client.documents.operations.compareExchange.CompareExchangeValue;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.QueryStatistics;
import net.ravendb.client.documents.session.SessionOptions;
import net.ravendb.client.documents.session.TransactionMode;
import net.ravendb.client.infrastructure.orders.Address;
import net.ravendb.client.infrastructure.orders.Company;
import net.ravendb.client.infrastructure.orders.Employee;
import net.ravendb.client.primitives.Reference;
import org.checkerframework.checker.units.qual.A;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

public class RavenDB_14006Test extends RemoteTestBase {

    @Test
    public void compareExchangeValueTrackingInSession() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            SessionOptions sessionOptions = new SessionOptions();
            sessionOptions.setTransactionMode(TransactionMode.CLUSTER_WIDE);

            try (IDocumentSession session = store.openSession(sessionOptions)) {
                Company company = new Company();
                company.setId("companies/1");
                company.setExternalId("companies/cf");
                company.setName("CF");

                session.store(company);

                int numberOfRequests = session.advanced().getNumberOfRequests();

                Address address = new Address();
                address.setCity("Torun");
                session.advanced()
                        .clusterTransaction()
                        .createCompareExchangeValue(company.getExternalId(), address);

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(numberOfRequests);

                CompareExchangeValue<Address> value1 = session.advanced().clusterTransaction().getCompareExchangeValue(Address.class, company.getExternalId());

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(numberOfRequests);

                assertThat(value1.getValue())
                        .isEqualTo(address);
                assertThat(value1.getKey())
                        .isEqualTo(company.getExternalId());
                assertThat(value1.getIndex())
                        .isZero();

                session.saveChanges();

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(numberOfRequests + 1);

                assertThat(value1.getValue())
                        .isEqualTo(address);
                assertThat(value1.getKey())
                        .isEqualTo(company.getExternalId());
                assertThat(value1.getIndex())
                        .isPositive();

                CompareExchangeValue<Address> value2 = session.advanced().clusterTransaction().getCompareExchangeValue(Address.class, company.getExternalId());

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(numberOfRequests + 1);

                assertThat(value2)
                        .isSameAs(value1);

                session.saveChanges();

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(numberOfRequests + 1);

                session.advanced().clear();

                CompareExchangeValue<Address> value3 = session.advanced().clusterTransaction().getCompareExchangeValue(Address.class, company.getExternalId());
                assertThat(value3)
                        .isNotSameAs(value2);
            }

            try (IDocumentSession session = store.openSession(sessionOptions)) {
                Address address = new Address();
                address.setCity("Hadera");
                session.advanced().clusterTransaction().createCompareExchangeValue("companies/hr", address);

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession(sessionOptions)) {
                int numberOfRequests = session.advanced().getNumberOfRequests();

                CompareExchangeValue<Address> value1 = session.advanced().clusterTransaction().getCompareExchangeValue(Address.class, "companies/cf");

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(numberOfRequests + 1);

                CompareExchangeValue<Address> value2 = session.advanced().clusterTransaction().getCompareExchangeValue(Address.class, "companies/hr");

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(numberOfRequests + 2);

                Map<String, CompareExchangeValue<Address>> values = session.advanced().clusterTransaction().getCompareExchangeValues(Address.class,
                        new String[]{"companies/cf", "companies/hr"});

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(numberOfRequests + 2);

                assertThat(values.size())
                        .isEqualTo(2);
                assertThat(values.get(value1.getKey()))
                        .isEqualTo(value1);
                assertThat(values.get(value2.getKey()))
                        .isEqualTo(value2);

                values = session.advanced().clusterTransaction().getCompareExchangeValues(Address.class,
                        new String[] { "companies/cf", "companies/hr", "companies/hx" });

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(numberOfRequests + 3);

                assertThat(values.size())
                        .isEqualTo(3);

                assertThat(values.get(value1.getKey()))
                        .isSameAs(value1);
                assertThat(values.get(value2.getKey()))
                        .isSameAs(value2);

                CompareExchangeValue<Address> value3 = session.advanced().clusterTransaction().getCompareExchangeValue(Address.class, "companies/hx");
                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(numberOfRequests + 3);

                assertThat(value3)
                        .isNull();
                assertThat(values.get("companies/hx"))
                        .isNull();

                session.saveChanges();

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(numberOfRequests + 3);

                Address address = new Address();
                address.setCity("Bydgoszcz");

                session.advanced().clusterTransaction().createCompareExchangeValue("companies/hx", address);

                session.saveChanges();

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(numberOfRequests + 4);
            }
        }
    }

    @Test
    public void compareExchangeValueTrackingInSession_NoTracking() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            Company company = new Company();
            company.setId("companies/1");
            company.setExternalId("companies/cf");
            company.setName("CF");

            SessionOptions sessionOptions = new SessionOptions();
            sessionOptions.setTransactionMode(TransactionMode.CLUSTER_WIDE);

            try (IDocumentSession session = store.openSession(sessionOptions)) {
                session.store(company);

                Address address = new Address();
                address.setCity("Torun");
                session.advanced().clusterTransaction().createCompareExchangeValue(company.getExternalId(), address);
                session.saveChanges();
            }

            SessionOptions sessionOptionsNoTracking = new SessionOptions();
            sessionOptionsNoTracking.setNoTracking(true);
            sessionOptionsNoTracking.setTransactionMode(TransactionMode.CLUSTER_WIDE);

            try (IDocumentSession session = store.openSession(sessionOptionsNoTracking)) {
                int numberOfRequests = session.advanced().getNumberOfRequests();
                CompareExchangeValue<Address> value1 =
                        session.advanced().clusterTransaction().getCompareExchangeValue(
                                Address.class, company.getExternalId());

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(numberOfRequests + 1);

                CompareExchangeValue<Address> value2 =
                        session.advanced().clusterTransaction().getCompareExchangeValue(
                                Address.class, company.getExternalId());

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(numberOfRequests + 2);

                assertThat(value2)
                        .isNotSameAs(value1);

                CompareExchangeValue<Address> value3 =
                        session.advanced().clusterTransaction().getCompareExchangeValue(
                                Address.class, company.getExternalId());

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(numberOfRequests + 3);
                assertThat(value3)
                        .isNotSameAs(value2);
            }

            try (IDocumentSession session = store.openSession(sessionOptionsNoTracking)) {
                int numberOfRequests = session.advanced().getNumberOfRequests();

                Map<String, CompareExchangeValue<Address>> value1 =
                        session.advanced().clusterTransaction()
                                .getCompareExchangeValues(Address.class, company.getExternalId());

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(numberOfRequests + 1);

                Map<String, CompareExchangeValue<Address>> value2 =
                        session.advanced().clusterTransaction()
                                .getCompareExchangeValues(Address.class, company.getExternalId());

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(numberOfRequests + 2);

                assertThat(value2.get(company.getExternalId()))
                        .isNotSameAs(value1.get(company.getExternalId()));

                Map<String, CompareExchangeValue<Address>> value3 =
                        session.advanced().clusterTransaction()
                                .getCompareExchangeValues(Address.class, company.getExternalId());

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(numberOfRequests + 3);

                assertThat(value3.get(company.getExternalId()))
                        .isNotSameAs(value2.get(company.getExternalId()));
            }

            try (IDocumentSession session = store.openSession(sessionOptionsNoTracking)) {
                int numberOfRequests = session.advanced().getNumberOfRequests();

                Map<String, CompareExchangeValue<Address>> value1 =
                        session.advanced().clusterTransaction()
                                .getCompareExchangeValues(Address.class, new String[] { company.getExternalId() });

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(numberOfRequests + 1);

                Map<String, CompareExchangeValue<Address>> value2 =
                        session.advanced().clusterTransaction()
                                .getCompareExchangeValues(Address.class, new String[] { company.getExternalId() });

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(numberOfRequests + 2);

                assertThat(value2.get(company.getExternalId()))
                        .isNotSameAs(value1.get(company.getExternalId()));

                Map<String, CompareExchangeValue<Address>> value3 =
                        session.advanced().clusterTransaction()
                                .getCompareExchangeValues(Address.class, new String[] { company.getExternalId() });

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(numberOfRequests + 3);

                assertThat(value3.get(company.getExternalId()))
                        .isNotSameAs(value2.get(company.getExternalId()));
            }
        }
    }

    @Test
    public void canUseCompareExchangeValueIncludesInLoad() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            SessionOptions sessionOptions = new SessionOptions();
            sessionOptions.setTransactionMode(TransactionMode.CLUSTER_WIDE);

            try (IDocumentSession session = store.openSession(sessionOptions)) {
                Employee employee = new Employee();
                employee.setId("employees/1");
                employee.setNotes(Arrays.asList("companies/cf", "companies/hr"));

                session.store(employee);

                Company company = new Company();
                company.setId("companies/1");
                company.setExternalId("companies/cf");
                company.setName("CF");
                session.store(company);

                Address address1 = new Address();
                address1.setCity("Torun");
                session.advanced().clusterTransaction().createCompareExchangeValue("companies/cf", address1);

                Address address2 = new Address();
                address2.setCity("Hadera");
                session.advanced().clusterTransaction().createCompareExchangeValue("companies/hr", address2);

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession(sessionOptions)) {
                Company company1 = session.load(Company.class, "companies/1",
                        i -> i.includeCompareExchangeValue("externalId"));

                int numberOfRequests = session.advanced().getNumberOfRequests();

                CompareExchangeValue<Address> value1 = session.advanced()
                        .clusterTransaction().getCompareExchangeValue(Address.class, company1.getExternalId());

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(numberOfRequests);

                assertThat(value1)
                        .isNotNull();
                assertThat(value1.getIndex())
                        .isPositive();
                assertThat(value1.getKey())
                        .isEqualTo(company1.getExternalId());
                assertThat(value1.getValue())
                        .isNotNull();
                assertThat(value1.getValue().getCity())
                        .isEqualTo("Torun");

                Company company2 = session.load(Company.class, "companies/1", i -> i.includeCompareExchangeValue("externalId"));

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(numberOfRequests);

                assertThat(company2)
                        .isSameAs(company1);

                Employee employee1 = session.load(Employee.class, "employees/1", i -> i.includeCompareExchangeValue("notes"));

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(numberOfRequests + 1);

                CompareExchangeValue<Address> value2 = session.advanced().clusterTransaction().getCompareExchangeValue(Address.class, employee1.getNotes().get(0));
                CompareExchangeValue<Address> value3 = session.advanced().clusterTransaction().getCompareExchangeValue(Address.class, employee1.getNotes().get(1));

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(numberOfRequests + 1);

                assertThat(value2)
                        .isSameAs(value1);
                assertThat(value3)
                        .isNotSameAs(value2);

                Map<String, CompareExchangeValue<Address>> values = session.advanced().clusterTransaction().getCompareExchangeValues(Address.class, employee1.getNotes().toArray(new String[0]));

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(numberOfRequests + 1);

                assertThat(values.size())
                        .isEqualTo(2);
                assertThat(values.get(value2.getKey()))
                        .isSameAs(value2);
                assertThat(values.get(value3.getKey()))
                        .isSameAs(value3);
            }
        }
    }

    @Test
    public void canUseCompareExchangeValueIncludesInQueries_Dynamic() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            SessionOptions sessionOptions = new SessionOptions();
            sessionOptions.setTransactionMode(TransactionMode.CLUSTER_WIDE);

            try (IDocumentSession session = store.openSession(sessionOptions)) {
                Employee employee = new Employee();
                employee.setId("employees/1");
                employee.setNotes(Arrays.asList("companies/cf", "companies/hr"));
                session.store(employee);

                Company company = new Company();
                company.setId("companies/1");
                company.setExternalId("companies/cf");
                company.setName("CF");
                session.store(company);

                Address address1 = new Address();
                address1.setCity("Torun");
                session.advanced().clusterTransaction().createCompareExchangeValue("companies/cf", address1);

                Address address2 = new Address();
                address2.setCity("Hadera");
                session.advanced().clusterTransaction().createCompareExchangeValue("companies/hr", address2);

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession(sessionOptions)) {
                Reference<QueryStatistics> queryStats = new Reference<>();
                List<Company> companies = session.query(Company.class)
                        .statistics(queryStats)
                        .include(b -> b.includeCompareExchangeValue("externalId"))
                        .toList();

                assertThat(companies.size())
                        .isOne();
                assertThat(queryStats.value.getDurationInMs())
                        .isNotNegative();
                Long resultEtag = queryStats.value.getResultEtag();

                int numberOfRequests = session.advanced().getNumberOfRequests();

                CompareExchangeValue<Address> value1 = session.advanced().clusterTransaction().getCompareExchangeValue(Address.class, companies.get(0).getExternalId());
                assertThat(value1.getValue().getCity())
                        .isEqualTo("Torun");

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(numberOfRequests);

                companies = session.query(Company.class)
                        .statistics(queryStats)
                        .include(b -> b.includeCompareExchangeValue("externalId"))
                        .toList();

                assertThat(companies)
                        .hasSize(1);
                assertThat(queryStats.value.getDurationInMs())
                        .isEqualTo(-1); // from cache
                assertThat(queryStats.value.getResultEtag())
                        .isEqualTo(resultEtag);

                try (IDocumentSession innerSession = store.openSession(sessionOptions)) {
                    CompareExchangeValue<Address> value = innerSession.advanced().clusterTransaction().getCompareExchangeValue(Address.class, companies.get(0).getExternalId());
                    value.getValue().setCity("Bydgoszcz");

                    innerSession.saveChanges();
                }

                companies = session.query(Company.class)
                        .statistics(queryStats)
                        .include(b -> b.includeCompareExchangeValue("externalId"))
                        .toList();

                assertThat(companies)
                        .hasSize(1);
                assertThat(queryStats.value.getDurationInMs())
                        .isNotNegative(); // not from cache
                assertThat(queryStats.value.getResultEtag())
                        .isNotEqualTo(resultEtag);

                value1 = session.advanced().clusterTransaction().getCompareExchangeValue(Address.class, companies.get(0).getExternalId());
                assertThat(value1.getValue().getCity())
                        .isEqualTo("Bydgoszcz");
            }
        }
    }

    @Test
    public void canUseCompareExchangeValueIncludesInQueries_Dynamic_JavaScript() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {

            SessionOptions sessionOptions = new SessionOptions();
            sessionOptions.setTransactionMode(TransactionMode.CLUSTER_WIDE);

            try (IDocumentSession session = store.openSession(sessionOptions)) {
                Employee employee = new Employee();
                employee.setId("employees/1");
                employee.setNotes(Arrays.asList("companies/cf", "companies/hr"));
                session.store(employee);

                Company company = new Company();
                company.setId("companies/1");
                company.setExternalId("companies/cf");
                company.setName("CF");
                session.store(company);

                Address address1 = new Address();
                address1.setCity("Torun");
                session.advanced().clusterTransaction().createCompareExchangeValue("companies/cf", address1);

                Address address2 = new Address();
                address2.setCity("Hadera");
                session.advanced().clusterTransaction().createCompareExchangeValue("companies/hr", address2);

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession(sessionOptions)) {
                Reference<QueryStatistics> statsRef = new Reference<>();
                List<Company> companies = session.advanced().rawQuery(Company.class, "declare function incl(c) {\n" +
                        "    includes.cmpxchg(c.externalId);\n" +
                        "    return c;\n" +
                        "}\n" +
                        "from Companies as c\n" +
                        "select incl(c)")
                        .statistics(statsRef)
                        .toList();

                assertThat(companies)
                        .hasSize(1);
                assertThat(statsRef.value.getDurationInMs())
                        .isNotNegative();
                Long resultEtag = statsRef.value.getResultEtag();

                int numberOfRequests = session.advanced().getNumberOfRequests();

                CompareExchangeValue<Address> value1 = session.advanced().clusterTransaction().getCompareExchangeValue(Address.class, companies.get(0).getExternalId());
                assertThat(value1.getValue().getCity())
                        .isEqualTo("Torun");

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(numberOfRequests);

                companies = session.advanced().rawQuery(Company.class, "declare function incl(c) {\n" +
                        "    includes.cmpxchg(c.externalId);\n" +
                        "    return c;\n" +
                        "}\n" +
                        "from Companies as c\n" +
                        "select incl(c)")
                        .statistics(statsRef)
                        .toList();

                assertThat(companies)
                        .hasSize(1);
                assertThat(statsRef.value.getDurationInMs())
                        .isEqualTo(-1); // from cache
                assertThat(statsRef.value.getResultEtag())
                        .isEqualTo(resultEtag);

                try (IDocumentSession innerSession = store.openSession(sessionOptions)) {
                    CompareExchangeValue<Address> value = innerSession.advanced().clusterTransaction().getCompareExchangeValue(Address.class, companies.get(0).getExternalId());
                    value.getValue().setCity("Bydgoszcz");

                    innerSession.saveChanges();
                }

                companies = session.advanced().rawQuery(Company.class, "declare function incl(c) {\n" +
                        "    includes.cmpxchg(c.externalId);\n" +
                        "    return c;\n" +
                        "}\n" +
                        "from Companies as c\n" +
                        "select incl(c)")
                        .statistics(statsRef)
                        .toList();

                assertThat(companies)
                        .hasSize(1);
                assertThat(statsRef.value.getDurationInMs())
                        .isNotNegative(); // not from cache
                assertThat(statsRef.value.getResultEtag())
                        .isNotEqualTo(resultEtag);

                value1 = session.advanced().clusterTransaction().getCompareExchangeValue(Address.class, companies.get(0).getExternalId());
                assertThat(value1.getValue().getCity())
                        .isEqualTo("Bydgoszcz");
            }
        }
    }

    @Test
    public void canUseCompareExchangeValueIncludesInQueries_Static() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            new Companies_ByName().execute(store);

            SessionOptions sessionOptions = new SessionOptions();
            sessionOptions.setTransactionMode(TransactionMode.CLUSTER_WIDE);

            try (IDocumentSession session = store.openSession(sessionOptions)) {
                Employee employee = new Employee();
                employee.setId("employees/1");
                employee.setNotes(Arrays.asList("companies/cf", "companies/hr"));
                session.store(employee);

                Company company = new Company();
                company.setId("companies/1");
                company.setExternalId("companies/cf");
                company.setName("CF");
                session.store(company);

                Address address1 = new Address();
                address1.setCity("Torun");
                session.advanced().clusterTransaction().createCompareExchangeValue("companies/cf", address1);

                Address address2 = new Address();
                address2.setCity("Hadera");
                session.advanced().clusterTransaction().createCompareExchangeValue("companies/hr", address2);

                session.saveChanges();
            }

            waitForIndexing(store);

            try (IDocumentSession session = store.openSession(sessionOptions)) {
                Reference<QueryStatistics> statsRef = new Reference<>();
                List<Company> companies = session.query(Company.class, Companies_ByName.class)
                        .statistics(statsRef)
                        .include(b -> b.includeCompareExchangeValue("externalId"))
                        .toList();

                assertThat(companies)
                        .hasSize(1);
                assertThat(statsRef.value.getDurationInMs())
                        .isNotNegative();
                Long resultEtag = statsRef.value.getResultEtag();

                int numberOfRequests = session.advanced().getNumberOfRequests();

                CompareExchangeValue<Address> value1 = session.advanced().clusterTransaction().getCompareExchangeValue(Address.class, companies.get(0).getExternalId());
                assertThat(value1.getValue().getCity())
                        .isEqualTo("Torun");

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(numberOfRequests);

                companies = session.query(Company.class, Companies_ByName.class)
                        .statistics(statsRef)
                        .include(b -> b.includeCompareExchangeValue("externalId"))
                        .toList();

                assertThat(companies)
                        .hasSize(1);
                assertThat(statsRef.value.getDurationInMs())
                        .isEqualTo(-1); // from cache
                assertThat(statsRef.value.getResultEtag())
                        .isEqualTo(resultEtag);

                try (IDocumentSession innerSession = store.openSession(sessionOptions)) {
                    CompareExchangeValue<Address> value = innerSession.advanced().clusterTransaction().getCompareExchangeValue(Address.class, companies.get(0).getExternalId());
                    value.getValue().setCity("Bydgoszcz");

                    innerSession.saveChanges();

                    waitForIndexing(store);
                }

                companies = session.query(Company.class, Companies_ByName.class)
                        .statistics(statsRef)
                        .include(b -> b.includeCompareExchangeValue("externalId"))
                        .toList();

                assertThat(companies)
                        .hasSize(1);
                assertThat(statsRef.value.getDurationInMs())
                        .isNotNegative(); // not from cache
                assertThat(statsRef.value.getResultEtag())
                        .isNotEqualTo(resultEtag);

                value1 = session.advanced().clusterTransaction().getCompareExchangeValue(Address.class, companies.get(0).getExternalId());
                assertThat(value1.getValue().getCity())
                        .isEqualTo("Bydgoszcz");
            }
        }
    }

    @Test
    public void canUseCompareExchangeValueIncludesInQueries_Static_JavaScript() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            new Companies_ByName().execute(store);

            SessionOptions sessionOptions = new SessionOptions();
            sessionOptions.setTransactionMode(TransactionMode.CLUSTER_WIDE);

            try (IDocumentSession session = store.openSession(sessionOptions)) {
                Employee employee = new Employee();
                employee.setId("employees/1");
                employee.setNotes(Arrays.asList("companies/cf", "companies/hr"));
                session.store(employee);

                Company company = new Company();
                company.setId("companies/1");
                company.setExternalId("companies/cf");
                company.setName("CF");
                session.store(company);

                Address address1 = new Address();
                address1.setCity("Torun");
                session.advanced().clusterTransaction().createCompareExchangeValue("companies/cf", address1);

                Address address2 = new Address();
                address2.setCity("Hadera");
                session.advanced().clusterTransaction().createCompareExchangeValue("companies/hr", address2);

                session.saveChanges();
            }

            waitForIndexing(store);

            try (IDocumentSession session = store.openSession(sessionOptions)) {
                Reference<QueryStatistics> statsRef = new Reference<>();
                List<Company> companies = session.advanced().rawQuery(Company.class, "declare function incl(c) {\n" +
                        "    includes.cmpxchg(c.externalId);\n" +
                        "    return c;\n" +
                        "}\n" +
                        "from index 'Companies/ByName' as c\n" +
                        "select incl(c)")
                        .statistics(statsRef)
                        .toList();

                assertThat(companies)
                        .hasSize(1);
                assertThat(statsRef.value.getDurationInMs())
                        .isNotNegative();
                Long resultEtag = statsRef.value.getResultEtag();

                int numberOfRequests = session.advanced().getNumberOfRequests();

                CompareExchangeValue<Address> value1 = session.advanced().clusterTransaction().getCompareExchangeValue(Address.class, companies.get(0).getExternalId());
                assertThat(value1.getValue().getCity())
                        .isEqualTo("Torun");

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(numberOfRequests);

                companies = session.advanced().rawQuery(Company.class, "declare function incl(c) {\n" +
                        "    includes.cmpxchg(c.externalId);\n" +
                        "    return c;\n" +
                        "}\n" +
                        "from index 'Companies/ByName' as c\n" +
                        "select incl(c)")
                        .statistics(statsRef)
                        .toList();

                assertThat(companies)
                        .hasSize(1);
                assertThat(statsRef.value.getDurationInMs())
                        .isEqualTo(-1); // from cache
                assertThat(statsRef.value.getResultEtag())
                        .isEqualTo(resultEtag);

                try (IDocumentSession innerSession = store.openSession(sessionOptions)) {
                    CompareExchangeValue<Address> value = innerSession.advanced().clusterTransaction().getCompareExchangeValue(Address.class, companies.get(0).getExternalId());
                    value.getValue().setCity("Bydgoszcz");

                    innerSession.saveChanges();

                    waitForIndexing(store);
                }

                companies = session.advanced().rawQuery(Company.class, "declare function incl(c) {\n" +
                        "    includes.cmpxchg(c.externalId);\n" +
                        "    return c;\n" +
                        "}\n" +
                        "from index 'Companies/ByName' as c\n" +
                        "select incl(c)")
                        .statistics(statsRef)
                        .toList();

                assertThat(companies)
                        .hasSize(1);
                assertThat(statsRef.value.getDurationInMs())
                        .isNotNegative(); // not from cache
                assertThat(statsRef.value.getResultEtag())
                        .isNotEqualTo(resultEtag);

                value1 = session.advanced().clusterTransaction().getCompareExchangeValue(Address.class, companies.get(0).getExternalId());
                assertThat(value1.getValue().getCity())
                        .isEqualTo("Bydgoszcz");
            }
        }
    }

    @Test
    public void compareExchangeValueTrackingInSessionStartsWith() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            List<String> allCompanies = new ArrayList<>();

            SessionOptions sessionOptions = new SessionOptions();
            sessionOptions.setTransactionMode(TransactionMode.CLUSTER_WIDE);

            try (IDocumentSession session = store.openSession(sessionOptions)) {
                for (int i = 0; i < 10; i++) {
                    Company company = new Company();
                    company.setId("companies/" + i);
                    company.setExternalId("companies/hr");
                    company.setName("HR");

                    allCompanies.add(company.getId());
                    session.advanced().clusterTransaction().createCompareExchangeValue(company.getId(), company);
                }

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession(sessionOptions)) {
                Map<String, CompareExchangeValue<Company>> results =
                        session.advanced().clusterTransaction().getCompareExchangeValues(Company.class, "comp");

                assertThat(results)
                        .hasSize(10);
                assertThat(results.values().stream().allMatch(Objects::nonNull))
                        .isTrue();
                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);

                results = session.advanced().clusterTransaction().getCompareExchangeValues(Company.class, allCompanies.toArray(new String[0]));

                assertThat(results)
                        .hasSize(10);
                assertThat(results.values().stream().allMatch(Objects::nonNull));
                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);

                for (String companyId : allCompanies) {
                    CompareExchangeValue<Company> result =
                            session.advanced().clusterTransaction().getCompareExchangeValue(Company.class, companyId);
                    assertThat(result.getValue())
                            .isNotNull();
                    assertThat(session.advanced().getNumberOfRequests())
                            .isEqualTo(1);
                }
            }
        }
    }

    public static class Companies_ByName extends AbstractIndexCreationTask {
        public Companies_ByName() {
            map = "from c in docs.Companies select new { name = c.name }";
        }
    }
}
