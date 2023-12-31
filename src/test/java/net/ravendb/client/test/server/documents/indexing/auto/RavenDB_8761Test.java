package net.ravendb.client.test.server.documents.indexing.auto;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.OrderingType;
import net.ravendb.client.infrastructure.entities.Address;
import net.ravendb.client.infrastructure.entities.Order;
import net.ravendb.client.infrastructure.entities.OrderLine;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static net.ravendb.client.documents.queries.GroupBy.array;
import static net.ravendb.client.documents.queries.GroupBy.field;
import static org.assertj.core.api.Assertions.assertThat;

public class RavenDB_8761Test extends RemoteTestBase {
    @Test
    public void can_group_by_array_values() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            putDocs(store);

            try (IDocumentSession session = store.openSession()) {

                List<ProductCount> productCounts1 = session.advanced().rawQuery(ProductCount.class, "from Orders group by lines[].product\n" +
                        "  order by count()\n" +
                        "  select key() as productName, count() as count")
                        .waitForNonStaleResults()
                        .toList();

                List<ProductCount> productCounts2 = session.advanced().documentQuery(Order.class)
                        .groupBy("lines[].product")
                        .selectKey(null, "productName")
                        .selectCount()
                        .ofType(ProductCount.class)
                        .toList();

                for (List<ProductCount> products : Arrays.asList(productCounts1, productCounts2)) {
                    assertThat(products)
                            .hasSize(2);

                    assertThat(products.get(0).getProductName())
                            .isEqualTo("products/1");
                    assertThat(products.get(0).getCount())
                            .isEqualTo(1);

                    assertThat(products.get(1).getProductName())
                            .isEqualTo("products/2");
                    assertThat(products.get(1).getCount())
                            .isEqualTo(2);
                }

            }

            try (IDocumentSession session = store.openSession()) {

                List<ProductCount> productCounts1 = session.advanced().rawQuery(ProductCount.class, "from Orders\n" +
                        " group by lines[].product, shipTo.country\n" +
                        " order by count() \n" +
                        " select lines[].product as productName, shipTo.country as country, count() as count")
                        .toList();

                List<ProductCount> productCounts2 = session.advanced().documentQuery(Order.class)
                        .groupBy("lines[].product", "shipTo.country")
                        .selectKey("lines[].product", "productName")
                        .selectKey("shipTo.country", "country")
                        .selectCount()
                        .ofType(ProductCount.class)
                        .toList();


                for (List<ProductCount> products : Arrays.asList(productCounts1, productCounts2)) {
                    assertThat(products)
                            .hasSize(2);

                    assertThat(products.get(0).getProductName())
                            .isEqualTo("products/1");
                    assertThat(products.get(0).getCount())
                            .isEqualTo(1);
                    assertThat(products.get(0).getCountry())
                            .isEqualTo("USA");

                    assertThat(products.get(1).getProductName())
                            .isEqualTo("products/2");
                    assertThat(products.get(1).getCount())
                            .isEqualTo(2);
                    assertThat(products.get(1).getCountry())
                            .isEqualTo("USA");
                }
            }

            try (IDocumentSession session = store.openSession()) {
                List<ProductCount> productCounts1 = session.advanced().rawQuery(ProductCount.class, "from Orders\n" +
                        " group by lines[].product, lines[].quantity\n" +
                        " order by lines[].quantity\n" +
                        " select lines[].product as productName, lines[].quantity as quantity, count() as count")
                        .toList();

                List<ProductCount> productCounts2 = session.advanced().documentQuery(Order.class)
                        .groupBy("lines[].product", "lines[].quantity")
                        .selectKey("lines[].product", "productName")
                        .selectKey("lines[].quantity", "quantity")
                        .selectCount()
                        .ofType(ProductCount.class)
                        .toList();

                for (List<ProductCount> products : Arrays.asList(productCounts1, productCounts2)) {
                    assertThat(products)
                            .hasSize(3);

                    assertThat(products.get(0).getProductName())
                            .isEqualTo("products/1");
                    assertThat(products.get(0).getCount())
                            .isEqualTo(1);
                    assertThat(products.get(0).getQuantity())
                            .isEqualTo(1);

                    assertThat(products.get(1).getProductName())
                            .isEqualTo("products/2");
                    assertThat(products.get(1).getCount())
                            .isEqualTo(1);
                    assertThat(products.get(1).getQuantity())
                            .isEqualTo(2);

                    assertThat(products.get(2).getProductName())
                            .isEqualTo("products/2");
                    assertThat(products.get(2).getCount())
                            .isEqualTo(1);
                    assertThat(products.get(2).getQuantity())
                            .isEqualTo(3);
                }
            }
        }
    }

    @Test
    public void can_group_by_array_content() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            putDocs(store);

            try (IDocumentSession session = store.openSession()) {
                OrderLine orderLine1 = new OrderLine();
                orderLine1.setProduct("products/1");
                orderLine1.setQuantity(1);

                OrderLine orderLine2 = new OrderLine();
                orderLine2.setProduct("products/2");
                orderLine2.setQuantity(2);

                Address address = new Address();
                address.setCountry("USA");

                Order order = new Order();
                order.setShipTo(address);
                order.setLines(Arrays.asList(orderLine1, orderLine2));

                session.store(order);
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                List<ProductCount> productCounts1 = session.advanced().rawQuery(ProductCount.class, "from Orders group by array(lines[].product)\n" +
                        " order by count()\n" +
                        " select key() as products, count() as count")
                        .waitForNonStaleResults()
                        .toList();

                List<ProductCount> productCounts2 = session
                        .advanced()
                        .documentQuery(Order.class)
                        .groupBy(array("lines[].product"))
                        .selectKey(null, "products")
                        .selectCount()
                        .orderBy("count", OrderingType.LONG)
                        .ofType(ProductCount.class)
                        .toList();

                for (List<ProductCount> products : Arrays.asList(productCounts1, productCounts2)) {
                    assertThat(products)
                            .hasSize(2);

                    assertThat(products.get(0).getProducts())
                            .hasSize(1)
                            .contains("products/2");
                    assertThat(products.get(0).getCount())
                            .isEqualTo(1);


                    assertThat(products.get(1).getProducts())
                            .hasSize(2)
                            .containsExactly("products/1", "products/2");

                    assertThat(products.get(1).getCount())
                            .isEqualTo(2);
                }
            }

            try (IDocumentSession session = store.openSession()) {
                List<ProductCount> productCounts1 = session.advanced().rawQuery(ProductCount.class, "from Orders\n" +
                        " group by array(lines[].product), shipTo.country\n" +
                        " order by count()\n" +
                        " select lines[].product as products, shipTo.country as country, count() as count")
                        .waitForNonStaleResults()
                        .toList();

                List<ProductCount> productCounts2 = session
                        .advanced()
                        .documentQuery(Order.class)
                        .groupBy(array("lines[].product"), field("shipTo.country"))
                        .selectKey("lines[].product", "products")
                        .selectCount()
                        .orderBy("count", OrderingType.LONG)
                        .ofType(ProductCount.class)
                        .toList();

                for (List<ProductCount> products : Arrays.asList(productCounts1, productCounts2)) {
                    assertThat(products)
                            .hasSize(2);

                    assertThat(products.get(0).getProducts())
                            .hasSize(1)
                            .contains("products/2");
                    assertThat(products.get(0).getCount())
                            .isEqualTo(1);


                    assertThat(products.get(1).getProducts())
                            .hasSize(2)
                            .containsExactly("products/1", "products/2");

                    assertThat(products.get(1).getCount())
                            .isEqualTo(2);
                }
            }

            try (IDocumentSession session = store.openSession()) {
                List<ProductCount> productCounts1 = session.advanced().rawQuery(ProductCount.class, "from Orders\n" +
                        " group by array(lines[].product), array(lines[].quantity)\n" +
                        " order by count()\n" +
                        " select lines[].product as products, lines[].quantity as quantities, count() as count")
                        .waitForNonStaleResults()
                        .toList();

                List<ProductCount> productCounts2 = session
                        .advanced()
                        .documentQuery(Order.class)
                        .groupBy(array("lines[].product"), array("lines[].quantity"))
                        .selectKey("lines[].product", "products")
                        .selectKey("lines[].quantity", "quantities")
                        .selectCount()
                        .orderBy("count", OrderingType.LONG)
                        .ofType(ProductCount.class)
                        .toList();

                for (List<ProductCount> products : Arrays.asList(productCounts1, productCounts2)) {
                    assertThat(products)
                            .hasSize(2);

                    assertThat(products.get(0).getProducts())
                            .hasSize(1)
                            .contains("products/2");
                    assertThat(products.get(0).getCount())
                            .isEqualTo(1);
                    assertThat(products.get(0).getQuantities())
                            .containsExactly(3);


                    assertThat(products.get(1).getProducts())
                            .hasSize(2)
                            .containsExactly("products/1", "products/2");

                    assertThat(products.get(1).getCount())
                            .isEqualTo(2);

                    assertThat(products.get(1).getQuantities())
                            .containsExactly(1, 2);
                }
            }
        }
    }

    public static class ProductCount {
        private String productName;
        private int count;
        private String country;
        private int quantity;
        private List<String> products;
        private List<Integer> quantities;

        public String getProductName() {
            return productName;
        }

        public void setProductName(String productName) {
            this.productName = productName;
        }

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }

        public String getCountry() {
            return country;
        }

        public void setCountry(String country) {
            this.country = country;
        }

        public int getQuantity() {
            return quantity;
        }

        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }

        public List<String> getProducts() {
            return products;
        }

        public void setProducts(List<String> products) {
            this.products = products;
        }

        public List<Integer> getQuantities() {
            return quantities;
        }

        public void setQuantities(List<Integer> quantities) {
            this.quantities = quantities;
        }
    }

    private void putDocs(IDocumentStore store) {
        try (IDocumentSession session = store.openSession()) {
            Order order1 = new Order();

            OrderLine orderLine11 = new OrderLine();
            orderLine11.setProduct("products/1");
            orderLine11.setQuantity(1);

            OrderLine orderLine12 = new OrderLine();
            orderLine12.setProduct("products/2");
            orderLine12.setQuantity(2);

            order1.setLines(Arrays.asList(orderLine11, orderLine12));

            Address address1 = new Address();
            address1.setCountry("USA");

            order1.setShipTo(address1);

            session.store(order1);

            OrderLine orderLine21 = new OrderLine();
            orderLine21.setProduct("products/2");
            orderLine21.setQuantity(3);

            Address address2 = new Address();
            address2.setCountry("USA");
            Order order2 = new Order();
            order2.setLines(Collections.singletonList(orderLine21));
            order2.setShipTo(address2);
            session.store(order2);

            session.saveChanges();
        }
    }

}
