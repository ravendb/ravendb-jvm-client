package net.ravendb.client.test.faceted;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.indexes.AbstractIndexCreationTask;
import net.ravendb.client.documents.queries.facets.FacetResult;
import net.ravendb.client.documents.queries.facets.FacetValue;
import net.ravendb.client.documents.queries.facets.RangeBuilder;
import net.ravendb.client.documents.session.IDocumentSession;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("ConstantConditions")
public class AggregationTest extends RemoteTestBase {

    public static class Orders_All extends AbstractIndexCreationTask {
        public Orders_All() {
            map = "docs.Orders.Select(order => new { order.currency,\n" +
                    "                          order.product,\n" +
                    "                          order.total,\n" +
                    "                          order.quantity,\n" +
                    "                          order.region,\n" +
                    "                          order.at,\n" +
                    "                          order.tax })";
        }
    }

    public static class Order {
        private Currency currency;
        private String product;
        private double total;
        private int region;

        public Currency getCurrency() {
            return currency;
        }

        public void setCurrency(Currency currency) {
            this.currency = currency;
        }

        public String getProduct() {
            return product;
        }

        public void setProduct(String product) {
            this.product = product;
        }

        public double getTotal() {
            return total;
        }

        public void setTotal(double total) {
            this.total = total;
        }

        public int getRegion() {
            return region;
        }

        public void setRegion(int region) {
            this.region = region;
        }
    }

    public enum Currency {
        EUR,
        PLN,
        NIS
    }

    @Test
    public void canCorrectlyAggregate_Double() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            new Orders_All().execute(store);

            try (IDocumentSession session = store.openSession()) {
                Order obj = new Order();
                obj.setCurrency(Currency.EUR);
                obj.setProduct("Milk");
                obj.setTotal(1.1);
                obj.setRegion(1);

                Order obj2 = new Order();
                obj2.setCurrency(Currency.EUR);
                obj2.setProduct("Milk");
                obj2.setTotal(1);
                obj2.setRegion(1);

                session.store(obj);
                session.store(obj2);

                session.saveChanges();
            }

            waitForIndexing(store);

            try (IDocumentSession session = store.openSession()) {
                Map<String, FacetResult> result = session
                        .query(Order.class, Orders_All.class)
                        .aggregateBy(f -> f.byField("region")
                                .maxOn("total")
                                .minOn("total"))
                        .execute();

                FacetResult facetResult = result.get("region");
                assertThat(facetResult.getValues().get(0).getCount())
                        .isEqualTo(2);

                assertThat(facetResult.getValues().get(0).getMin())
                        .isEqualTo(1);
                assertThat(facetResult.getValues().get(0).getMax())
                        .isEqualTo(1.1);
                assertThat(facetResult.getValues().stream().filter(x -> "1".equals(x.getRange())).count())
                        .isEqualTo(1);

            }
        }
    }

    @Test
    public void canCorrectlyAggregate_MultipleItems() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            new Orders_All().execute(store);

            try (IDocumentSession session = store.openSession()) {
                Order obj = new Order();
                obj.setCurrency(Currency.EUR);
                obj.setProduct("Milk");
                obj.setTotal(3);

                Order obj2 = new Order();
                obj2.setCurrency(Currency.NIS);
                obj2.setProduct("Milk");
                obj2.setTotal(9);

                Order obj3 = new Order();
                obj3.setCurrency(Currency.EUR);
                obj3.setProduct("iPhone");
                obj3.setTotal(3333);

                session.store(obj);
                session.store(obj2);
                session.store(obj3);

                session.saveChanges();
            }

            waitForIndexing(store);

            try (IDocumentSession session = store.openSession()) {
                Map<String, FacetResult> r = session
                        .query(Order.class, Orders_All.class)
                        .aggregateBy(x -> x.byField("product").sumOn("total"))
                        .andAggregateBy(x -> x.byField("currency").sumOn("total"))
                        .execute();

                FacetResult facetResult = r.get("product");
                assertThat(facetResult.getValues().size())
                        .isEqualTo(2);

                assertThat(facetResult.getValues().stream().filter(x -> "milk".equals(x.getRange())).findFirst().get().getSum())
                        .isEqualTo(12);

                assertThat(facetResult.getValues().stream().filter(x -> "iphone".equals(x.getRange())).findFirst().get().getSum())
                        .isEqualTo(3333);

                facetResult = r.get("currency");
                assertThat(facetResult.getValues().size())
                        .isEqualTo(2);

                assertThat(facetResult.getValues().stream().filter(x -> "eur".equals(x.getRange())).findFirst().get().getSum())
                        .isEqualTo(3336);

                assertThat(facetResult.getValues().stream().filter(x -> "nis".equals(x.getRange())).findFirst().get().getSum())
                        .isEqualTo(9);
            }
        }
    }

    @Test
    public void canCorrectlyAggregate_MultipleAggregations() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            new Orders_All().execute(store);

            try (IDocumentSession session = store.openSession()) {
                Order obj = new Order();
                obj.setCurrency(Currency.EUR);
                obj.setProduct("Milk");
                obj.setTotal(3);

                Order obj2 = new Order();
                obj2.setCurrency(Currency.NIS);
                obj2.setProduct("Milk");
                obj2.setTotal(9);

                Order obj3 = new Order();
                obj3.setCurrency(Currency.EUR);
                obj3.setProduct("iPhone");
                obj3.setTotal(3333);

                session.store(obj);
                session.store(obj2);
                session.store(obj3);

                session.saveChanges();
            }

            waitForIndexing(store);

            try (IDocumentSession session = store.openSession()) {
                Map<String, FacetResult> r = session
                        .query(Order.class, Orders_All.class)
                        .aggregateBy(f -> f.byField("product").maxOn("total").minOn("total"))
                        .execute();

                FacetResult facetResult = r.get("product");
                assertThat(facetResult.getValues().size())
                        .isEqualTo(2);

                assertThat(facetResult.getValues().stream().filter(x -> "milk".equals(x.getRange())).findFirst().get().getMax())
                        .isEqualTo(9);

                assertThat(facetResult.getValues().stream().filter(x -> "milk".equals(x.getRange())).findFirst().get().getMin())
                        .isEqualTo(3);

                assertThat(facetResult.getValues().stream().filter(x -> "iphone".equals(x.getRange())).findFirst().get().getMax())
                        .isEqualTo(3333);

                assertThat(facetResult.getValues().stream().filter(x -> "iphone".equals(x.getRange())).findFirst().get().getMin())
                        .isEqualTo(3333);
            }
        }
    }

    @Test
    public void canCorrectlyAggregate_DisplayName() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            new Orders_All().execute(store);

            try (IDocumentSession session = store.openSession()) {
                Order obj = new Order();
                obj.setCurrency(Currency.EUR);
                obj.setProduct("Milk");
                obj.setTotal(3);

                Order obj2 = new Order();
                obj2.setCurrency(Currency.NIS);
                obj2.setProduct("Milk");
                obj2.setTotal(9);

                Order obj3 = new Order();
                obj3.setCurrency(Currency.EUR);
                obj3.setProduct("iPhone");
                obj3.setTotal(3333);

                session.store(obj);
                session.store(obj2);
                session.store(obj3);

                session.saveChanges();
            }

            waitForIndexing(store);

            try (IDocumentSession session = store.openSession()) {
                Map<String, FacetResult> r = session
                        .query(Order.class, Orders_All.class)
                        .aggregateBy(f -> f.byField("product").withDisplayName("productMax").maxOn("total"))
                        .andAggregateBy(f -> f.byField("product").withDisplayName("productMin"))
                        .execute();


                assertThat(r.size())
                        .isEqualTo(2);

                assertThat(r.get("productMax"))
                        .isNotNull();

                assertThat(r.get("productMin"))
                        .isNotNull();

                assertThat(r.get("productMax").getValues().get(0).getMax())
                        .isEqualTo(3333);

                assertThat(r.get("productMin").getValues().get(1).getCount())
                        .isEqualTo(2);
            }
        }
    }

    @Test
    public void canCorrectlyAggregate_Ranges() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {

            new Orders_All().execute(store);

            try (IDocumentSession session = store.openSession()) {
                Order obj = new Order();
                obj.setCurrency(Currency.EUR);
                obj.setProduct("Milk");
                obj.setTotal(3);

                Order obj2 = new Order();
                obj2.setCurrency(Currency.NIS);
                obj2.setProduct("Milk");
                obj2.setTotal(9);

                Order obj3 = new Order();
                obj3.setCurrency(Currency.EUR);
                obj3.setProduct("iPhone");
                obj3.setTotal(3333);

                session.store(obj);
                session.store(obj2);
                session.store(obj3);

                session.saveChanges();
            }

            waitForIndexing(store);

            try (IDocumentSession session = store.openSession()) {
                RangeBuilder<Integer> range = RangeBuilder.forPath("total");
                Map<String, FacetResult> r = session
                        .query(Order.class, Orders_All.class)
                        .aggregateBy(f -> f.byField("product").sumOn("total"))
                        .andAggregateBy(f -> f.byRanges(
                                range.isLessThan(100),
                                range.isGreaterThanOrEqualTo(100).isLessThan(500),
                                range.isGreaterThanOrEqualTo(500).isLessThan(1500),
                                range.isGreaterThanOrEqualTo(1500)
                        ).sumOn("total"))
                        .execute();

                FacetResult facetResult = r.get("product");
                assertThat(facetResult.getValues().size())
                        .isEqualTo(2);

                assertThat(facetResult.getValues().stream().filter(x -> "milk".equals(x.getRange())).findFirst().get().getSum())
                        .isEqualTo(12);
                assertThat(facetResult.getValues().stream().filter(x -> "iphone".equals(x.getRange())).findFirst().get().getSum())
                        .isEqualTo(3333);

                facetResult = r.get("total");
                assertThat(facetResult.getValues().size())
                        .isEqualTo(4);


                assertThat(facetResult.getValues().stream().filter(x -> "total < 100".equals(x.getRange())).findFirst().get().getSum())
                        .isEqualTo(12);
                assertThat(facetResult.getValues().stream().filter(x -> "total >= 1500".equals(x.getRange())).findFirst().get().getSum())
                        .isEqualTo(3333);
            }
        }
    }

    @Test
    public void canCorrectlyAggregate_DateTimeDataType_WithRangeCounts() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            new ItemsOrders_All().execute(store);

            try (IDocumentSession session = store.openSession()) {
                ItemsOrder item1 = new ItemsOrder();
                item1.setItems(Arrays.asList("first", "second"));
                item1.setAt(new Date());

                ItemsOrder item2 = new ItemsOrder();
                item2.setItems(Arrays.asList("first", "second"));
                item2.setAt(DateUtils.addDays(new Date(), -1));

                ItemsOrder item3 = new ItemsOrder();
                item3.setItems(Arrays.asList("first", "second"));
                item3.setAt(new Date());

                ItemsOrder item4 = new ItemsOrder();
                item4.setItems(Collections.singletonList("first"));
                item4.setAt(new Date());

                session.store(item1);
                session.store(item2);
                session.store(item3);
                session.store(item4);
                session.saveChanges();
            }

            List<Object> items = Collections.singletonList("second");

            Date minValue = DateUtils.setYears(new Date(), 1980);

            Date end0 = DateUtils.addDays(new Date(), -2);
            Date end1 = DateUtils.addDays(new Date(), -1);
            Date end2 = new Date();

            waitForIndexing(store);

            RangeBuilder<Date> builder = RangeBuilder.forPath("at");
            try (IDocumentSession session = store.openSession()) {
                Map<String, FacetResult> r = session.query(ItemsOrder.class, ItemsOrders_All.class)
                        .whereGreaterThanOrEqual("at", end0)
                        .aggregateBy(f -> f.byRanges(
                                builder.isGreaterThanOrEqualTo(minValue), // all - 4
                                builder.isGreaterThanOrEqualTo(end0).isLessThan(end1), // 0
                                builder.isGreaterThanOrEqualTo(end1).isLessThan(end2))) // 1
                        .execute();

                List<FacetValue> facetResults = r.get("at").getValues();

                assertThat(facetResults.get(0).getCount())
                        .isEqualTo(4);

                assertThat(facetResults.get(1).getCount())
                        .isEqualTo(1);

                assertThat(facetResults.get(2).getCount())
                        .isEqualTo(3);
            }
        }
    }
    public static class ItemsOrder {
        private List<String> items;
        private Date at;

        public List<String> getItems() {
            return items;
        }

        public void setItems(List<String> items) {
            this.items = items;
        }

        public Date getAt() {
            return at;
        }

        public void setAt(Date at) {
            this.at = at;
        }
    }

    public static class ItemsOrders_All extends AbstractIndexCreationTask {
        public ItemsOrders_All() {
            map = "docs.ItemsOrders.Select(order => new { order.at,\n" +
                    "                          order.items })";
        }
    }
}
