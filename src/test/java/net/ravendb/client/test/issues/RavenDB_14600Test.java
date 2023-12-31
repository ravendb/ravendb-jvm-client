package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.indexes.AbstractIndexCreationTask;
import net.ravendb.client.documents.queries.facets.FacetResult;
import net.ravendb.client.documents.queries.facets.FacetValue;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.infrastructure.orders.Employee;
import net.ravendb.client.infrastructure.orders.Order;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class RavenDB_14600Test extends RemoteTestBase {

    @Override
    protected void customizeStore(DocumentStore store) {
        store.getConventions().setDisableTopologyUpdates(true);
    }

    @Test
    public void canIncludeFacetResult() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                Employee employee = new Employee();
                employee.setFirstName("Test");
                session.store(employee, "employees/1");

                Order order = new Order();
                order.setEmployee("employees/1");
                order.setCompany("companies/1-A");

                session.store(order, "orders/1");
                session.saveChanges();
            }

            store.executeIndex(new MyIndex());

            waitForIndexing(store);

            try (IDocumentSession session = store.openSession()) {
                Map<String, FacetResult> facets = session.query(Order.class, MyIndex.class)
                        .include("employee")
                        .whereEquals("company", "companies/1-A")
                        .aggregateBy(x -> x.byField("employee"))
                        .execute();

                assertThat(facets.get("employee").getValues())
                        .isNotNull();

                for (FacetValue f : facets.get("employee").getValues()) {
                    Object e = session.load(Object.class, f.getRange());
                    String cv = session.advanced().getChangeVectorFor(e);
                    assertThat(cv)
                            .isNotNull();
                }

                assertThat(session.advanced().getNumberOfRequests())
                        .isOne();
            }
        }
    }

    public static class MyIndex extends AbstractIndexCreationTask {
        public MyIndex() {
            map = "from o in docs.Orders select new { o.employee, o.company }";
        }
    }
}
