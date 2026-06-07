package net.ravendb.client.test.client.queries;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.queries.IndexQuery;
import net.ravendb.client.documents.queries.RavenDocumentQuery;
import net.ravendb.client.documents.session.IDocumentQuery;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.infrastructure.EnableOnServer;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

public class NowTodayQueryTest extends RemoteTestBase {

    public static class Order {
        private String id;
        private Date createdAt;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public Date getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(Date createdAt) {
            this.createdAt = createdAt;
        }
    }

    @Test
    public void nowGeneratesNowFunctionInRql() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                IDocumentQuery<Order> query = session.advanced().documentQuery(Order.class)
                        .whereLessThan("createdAt", RavenDocumentQuery.now());

                assertThat(query.toString())
                        .contains("createdAt < now()");
            }
        }
    }

    @Test
    public void nowWithOffsetGeneratesNowFunctionWithParameterInRql() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                IDocumentQuery<Order> query = session.advanced().documentQuery(Order.class)
                        .whereGreaterThan("createdAt", RavenDocumentQuery.now("-30d"));

                assertThat(query.toString())
                        .contains("createdAt > now($p0)");

                IndexQuery indexQuery = query.getIndexQuery();
                assertThat(indexQuery.getQueryParameters().get("p0"))
                        .isEqualTo("-30d");
            }
        }
    }

    @Test
    public void todayGeneratesTodayFunctionInRql() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                IDocumentQuery<Order> query = session.advanced().documentQuery(Order.class)
                        .whereGreaterThanOrEqual("createdAt", RavenDocumentQuery.today())
                        .whereLessThanOrEqual("modifiedAt", RavenDocumentQuery.today());

                assertThat(query.toString())
                        .contains("createdAt >= today()")
                        .contains("modifiedAt <= today()");
            }
        }
    }

    @Test
    @EnableOnServer(thresholdVersion = "7.2.2")
    public void canFilterByServerSideNow() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                Order pastOrder = new Order();
                pastOrder.setCreatedAt(new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000L));
                session.store(pastOrder, "orders/past");

                Order futureOrder = new Order();
                futureOrder.setCreatedAt(new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000L));
                session.store(futureOrder, "orders/future");

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                assertThat(session.advanced().documentQuery(Order.class)
                        .whereLessThan("createdAt", RavenDocumentQuery.now())
                        .toList())
                        .hasSize(1);
            }
        }
    }
}
