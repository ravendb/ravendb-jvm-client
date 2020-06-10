package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.timeSeries.TimeSeriesEntry;
import net.ravendb.client.infrastructure.entities.Company;
import net.ravendb.client.infrastructure.entities.Order;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class RavenDB_15000Test extends RemoteTestBase {

    @Test
    public void canIncludeTimeSeriesWithoutProvidingFromAndToDates_ViaLoad() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                Order order = new Order();
                order.setCompany("companies/1-A");
                session.store(order, "orders/1-A");

                Company company = new Company();
                company.setName("HR");
                session.store(company, "companies/1-A");

                session.timeSeriesFor("orders/1-A", "Heartrate")
                        .append(new Date(), 1);

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                Order order = session.load(Order.class, "orders/1-A",
                        i -> i.includeDocuments("company").includeTimeSeries("Heartrate", null, null));

                // should not go to server
                Company company = session.load(Company.class, order.getCompany());

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);
                assertThat(company.getName())
                        .isEqualTo("HR");

                // should not go to server
                List<TimeSeriesEntry> vals = Arrays.asList(session.timeSeriesFor(order, "Heartrate")
                        .get());

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);
                assertThat(vals)
                        .hasSize(1);
            }
        }
    }

    @Test
    public void canIncludeTimeSeriesWithoutProvidingFromAndToDates_ViaQuery() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                Order order = new Order();
                order.setCompany("companies/1-A");
                session.store(order, "orders/1-A");

                Company company = new Company();
                company.setName("HR");
                session.store(company, "companies/1-A");

                session.timeSeriesFor("orders/1-A", "Heartrate")
                        .append(new Date(), 1);

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                Order order = session.query(Order.class)
                        .include(i -> i.includeDocuments("company").includeTimeSeries("Heartrate"))
                        .first();

                // should not go to server
                Company company = session.load(Company.class, order.getCompany());

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);
                assertThat(company.getName())
                        .isEqualTo("HR");

                // should not go to server
                List<TimeSeriesEntry> vals = Arrays.asList(session.timeSeriesFor(order, "Heartrate")
                        .get());

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);
                assertThat(vals)
                        .hasSize(1);
            }
        }
    }
}
