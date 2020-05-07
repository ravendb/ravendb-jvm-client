package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.operations.GetStatisticsOperation;
import net.ravendb.client.documents.operations.compareExchange.CompareExchangeValue;
import net.ravendb.client.documents.operations.configuration.ClientConfiguration;
import net.ravendb.client.documents.operations.configuration.PutClientConfigurationOperation;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.SessionOptions;
import net.ravendb.client.documents.session.TransactionMode;
import net.ravendb.client.exceptions.RavenException;
import net.ravendb.client.infrastructure.entities.Company;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

public class RavenDB_13456Test extends RemoteTestBase {
    @Test
    public void canChangeIdentityPartsSeparator() throws Exception {

        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                Company company1 = new Company();
                session.store(company1);

                assertThat(company1.getId())
                        .startsWith("companies/1-A");

                Company company2 = new Company();
                session.store(company2);

                assertThat(company2.getId())
                        .startsWith("companies/2-A");
            }

            try (IDocumentSession session = store.openSession()) {
                Company company1 = new Company();
                session.store(company1, "companies/");

                Company company2 = new Company();
                session.store(company2, "companies|");

                session.saveChanges();

                assertThat(company1.getId())
                        .startsWith("companies/000000000");
                assertThat(company2.getId())
                        .isEqualTo("companies/1");
            }

            SessionOptions sessionOptions = new SessionOptions();
            sessionOptions.setTransactionMode(TransactionMode.CLUSTER_WIDE);

            try (IDocumentSession session = store.openSession(sessionOptions)) {
                session.advanced().clusterTransaction().createCompareExchangeValue("company:", new Company());
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession(sessionOptions)) {
                session.advanced().clusterTransaction().createCompareExchangeValue("company|", new Company());

                assertThatThrownBy(session::saveChanges)
                        .isInstanceOf(RavenException.class)
                        .hasMessageContaining("Document id company| cannot end with '|' or '/' as part of cluster transaction");
            }

            try (IDocumentSession session = store.openSession(sessionOptions)) {
                session.advanced().clusterTransaction().createCompareExchangeValue("company/", new Company());

                assertThatThrownBy(session::saveChanges)
                        .isInstanceOf(RavenException.class)
                        .hasMessageContaining("Document id company/ cannot end with '|' or '/' as part of cluster transaction");
            }

            ClientConfiguration clientConfiguration = new ClientConfiguration();
            clientConfiguration.setIdentityPartsSeparator(':');

            store.maintenance().send(new PutClientConfigurationOperation(clientConfiguration));

            store.maintenance().send(new GetStatisticsOperation()); // forcing client configuration update

            try (IDocumentSession session = store.openSession()) {
                Company company1 = new Company();
                session.store(company1);

                assertThat(company1.getId())
                        .startsWith("companies:3-A");

                Company company2 = new Company();
                session.store(company2);

                assertThat(company2.getId())
                        .startsWith("companies:4-A");
            }

            try (IDocumentSession session = store.openSession()) {
                Company company1 = new Company();
                session.store(company1, "companies:");

                Company company2 = new Company();
                session.store(company2, "companies|");

                session.saveChanges();

                assertThat(company1.getId())
                        .startsWith("companies:000000000");

                assertThat(company2.getId())
                        .isEqualTo("companies:2");
            }

            try (IDocumentSession session = store.openSession(sessionOptions)) {
                session.advanced().clusterTransaction().createCompareExchangeValue("company:", new Company());

                assertThatThrownBy(session::saveChanges)
                        .isInstanceOf(RavenException.class)
                        .hasMessageContaining("Document id company: cannot end with '|' or ':' as part of cluster transaction");
            }

            try (IDocumentSession session = store.openSession(sessionOptions)) {
                session.advanced().clusterTransaction().createCompareExchangeValue("company|", new Company());

                assertThatThrownBy(session::saveChanges)
                        .isInstanceOf(RavenException.class)
                        .hasMessageContaining("Document id company| cannot end with '|' or ':' as part of cluster transaction");
            }

            try (IDocumentSession session = store.openSession(sessionOptions)) {
                session.advanced().clusterTransaction().createCompareExchangeValue("company/", new Company());

                session.saveChanges();
            }

            ClientConfiguration secondClientConfiguration = new ClientConfiguration();
            secondClientConfiguration.setIdentityPartsSeparator(null);
            store.maintenance().send(new PutClientConfigurationOperation(secondClientConfiguration));

            store.maintenance().send(new GetStatisticsOperation()); // forcing client configuration update

            try (IDocumentSession session = store.openSession()) {
                Company company1 = new Company();
                session.store(company1);

                assertThat(company1.getId())
                        .startsWith("companies/5-A");

                Company company2 = new Company();
                session.store(company2);

                assertThat(company2.getId())
                        .startsWith("companies/6-A");
            }

            try (IDocumentSession session = store.openSession()) {
                Company company1 = new Company();
                session.store(company1, "companies/");

                Company company2 = new Company();
                session.store(company2, "companies|");

                session.saveChanges();

                assertThat(company1.getId())
                        .startsWith("companies/000000000");
                assertThat(company2.getId())
                        .isEqualTo("companies/3");
            }

            try (IDocumentSession session = store.openSession(sessionOptions)) {
                CompareExchangeValue<Company> company = session.advanced().clusterTransaction().getCompareExchangeValue(Company.class, "company:");
                company.getValue().setName("HR");

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession(sessionOptions)) {
                session.advanced().clusterTransaction().createCompareExchangeValue("company|", new Company());

                assertThatThrownBy(session::saveChanges)
                        .isInstanceOf(RavenException.class)
                        .hasMessageContaining("Document id company| cannot end with '|' or '/' as part of cluster transaction");
            }

            try (IDocumentSession session = store.openSession(sessionOptions)) {
                session.advanced().clusterTransaction().createCompareExchangeValue("company/", new Company());

                assertThatThrownBy(session::saveChanges)
                        .isInstanceOf(RavenException.class)
                        .hasMessageContaining("Document id company/ cannot end with '|' or '/' as part of cluster transaction");
            }
        }
    }
}
