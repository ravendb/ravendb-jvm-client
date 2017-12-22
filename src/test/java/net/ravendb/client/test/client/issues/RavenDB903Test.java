package net.ravendb.client.test.client.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.indexes.AbstractIndexCreationTask;
import net.ravendb.client.documents.indexes.FieldIndexing;
import net.ravendb.client.documents.session.IDocumentQuery;
import net.ravendb.client.documents.session.IDocumentSession;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

public class RavenDB903Test extends RemoteTestBase {

    public static class Product {
        private String name;
        private String description;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }

    @Test
    public void test1() throws Exception {
        doTest(session -> session.advanced().documentQuery(Product.class, TestIndex.class)
                .search("description", "Hello")
                .intersect()
                .whereEquals("name", "Bar"));
    }

    @Test
    public void test2() throws Exception {
        doTest(session -> session.advanced().documentQuery(Product.class, TestIndex.class)
                .whereEquals("name", "Bar")
                .intersect()
                .search("description", "Hello"));
    }

    private void doTest(Function<IDocumentSession, IDocumentQuery<Product>> queryFunction) throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            store.executeIndex(new TestIndex());

            try (IDocumentSession session = store.openSession()) {
                Product product1 = new Product();
                product1.setName("Foo");
                product1.setDescription("Hello World");

                Product product2 = new Product();
                product2.setName("Bar");
                product2.setDescription("Hello World");

                Product product3 = new Product();
                product3.setName("Bar");
                product3.setDescription("Goodbye World");

                session.store(product1);
                session.store(product2);
                session.store(product3);
                session.saveChanges();
            }

            waitForIndexing(store);

            try (IDocumentSession session = store.openSession()) {
                IDocumentQuery<Product> query = queryFunction.apply(session);

                List<Product> products = query.toList();
                assertThat(products)
                        .hasSize(1);
            }
        }
    }

    public static class TestIndex extends AbstractIndexCreationTask {
        public TestIndex() {
            map = "from product in docs.Products select new { product.name, product.description }";

            index("description", FieldIndexing.SEARCH);
        }
    }
}
