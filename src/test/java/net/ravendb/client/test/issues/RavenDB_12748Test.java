package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.indexes.AbstractIndexCreationTask;
import net.ravendb.client.documents.queries.Query;
import net.ravendb.client.documents.queries.facets.FacetResult;
import net.ravendb.client.documents.queries.facets.FacetValue;
import net.ravendb.client.documents.queries.facets.RangeBuilder;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.infrastructure.entities.faceted.Currency;
import net.ravendb.client.infrastructure.entities.faceted.Order;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class RavenDB_12748Test extends RemoteTestBase {

    @Test
    public void canCorrectlyAggregate() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            new Orders_All().execute(store);

            try (IDocumentSession session = store.openSession()) {
                Order order1 = new Order();
                order1.setCurrency(Currency.EUR);
                order1.setProduct("Milk");
                order1.setQuantity(3);
                order1.setTotal(3);

                session.store(order1);

                Order order2 = new Order();
                order2.setCurrency(Currency.NIS);
                order2.setProduct("Milk");
                order2.setQuantity(5);
                order2.setTotal(9);

                session.store(order2);

                Order order3 = new Order();
                order3.setCurrency(Currency.EUR);
                order3.setProduct("iPhone");
                order3.setQuantity(7777);
                order3.setTotal(3333);

                session.store(order3);
                session.saveChanges();
            }

            waitForIndexing(store);

            try (IDocumentSession session = store.openSession()) {
                Map<String, FacetResult> r = session.query(Order.class, Orders_All.class)
                        .aggregateBy(f -> f.byField("region"))
                        .andAggregateBy(f -> f.byField("product").sumOn("total").averageOn("total").sumOn("quantity"))
                        .execute();

                FacetResult facetResult = r.get("region");
                assertThat(facetResult.getValues())
                        .hasSize(1);
                assertThat(facetResult.getValues().get(0).getName())
                        .isNull();
                assertThat(facetResult.getValues().get(0).getCount())
                        .isEqualTo(3);

                facetResult = r.get("product");
                List<FacetValue> totalValues =
                        facetResult
                                .getValues()
                                .stream()
                                .filter(x -> x.getName().equals("total"))
                                .collect(Collectors.toList());

                assertThat(totalValues)
                        .hasSize(2);

                FacetValue milkValue = totalValues.stream().filter(x -> "milk".equals(x.getRange())).findFirst().get();
                FacetValue iphoneValue = totalValues.stream().filter(x -> "iphone".equals(x.getRange())).findFirst().get();

                assertThat(milkValue.getCount())
                        .isEqualTo(2);
                assertThat(iphoneValue.getCount())
                        .isEqualTo(1);
                assertThat(milkValue.getSum())
                        .isEqualTo(12);
                assertThat(iphoneValue.getSum())
                        .isEqualTo(3333);
                assertThat(milkValue.getAverage())
                        .isEqualTo(6);
                assertThat(iphoneValue.getAverage())
                        .isEqualTo(3333);
                assertThat(milkValue.getMax())
                        .isNull();
                assertThat(iphoneValue.getMax())
                        .isNull();
                assertThat(milkValue.getMin())
                        .isNull();
                assertThat(iphoneValue.getMin())
                        .isNull();

                List<FacetValue> quantityValues = facetResult.getValues().stream().filter(x -> "quantity".equals(x.getName())).collect(Collectors.toList());

                milkValue = quantityValues.stream().filter(x -> "milk".equals(x.getRange())).findFirst().get();
                iphoneValue = quantityValues.stream().filter(x -> "iphone".equals(x.getRange())).findFirst().get();

                assertThat(milkValue.getCount())
                        .isEqualTo(2);
                assertThat(iphoneValue.getCount())
                        .isEqualTo(1);
                assertThat(milkValue.getSum())
                        .isEqualTo(8);
                assertThat(iphoneValue.getSum())
                        .isEqualTo(7777);
                assertThat(milkValue.getAverage())
                        .isNull();
                assertThat(iphoneValue.getAverage())
                        .isNull();
                assertThat(milkValue.getMax())
                        .isNull();
                assertThat(iphoneValue.getMax())
                        .isNull();
                assertThat(milkValue.getMin())
                        .isNull();
                assertThat(iphoneValue.getMin())
                        .isNull();
            }

            try (IDocumentSession session = store.openSession()) {
                Map<String, FacetResult> r = session.query(Order.class, Orders_All.class)
                        .aggregateBy(f -> f.byField("region"))
                        .andAggregateBy(f -> f.byField("product").sumOn("total", "T1").averageOn("total", "T1").sumOn("quantity", "Q1"))
                        .execute();

                FacetResult facetResult = r.get("region");
                assertThat(facetResult.getValues())
                        .hasSize(1);
                assertThat(facetResult.getValues().get(0).getName())
                        .isNull();
                assertThat(facetResult.getValues().get(0).getCount())
                        .isEqualTo(3);

                facetResult = r.get("product");
                List<FacetValue> totalValues = facetResult.getValues().stream().filter(x -> "T1".equals(x.getName())).collect(Collectors.toList());

                assertThat(totalValues)
                        .hasSize(2);

                FacetValue milkValue = totalValues.stream().filter(x -> "milk".equals(x.getRange())).findFirst().get();
                FacetValue iphoneValue = totalValues.stream().filter(x -> "iphone".equals(x.getRange())).findFirst().get();

                assertThat(milkValue.getCount())
                        .isEqualTo(2);
                assertThat(iphoneValue.getCount())
                        .isEqualTo(1);
                assertThat(milkValue.getSum())
                        .isEqualTo(12);
                assertThat(iphoneValue.getSum())
                        .isEqualTo(3333);
                assertThat(milkValue.getAverage())
                        .isEqualTo(6);
                assertThat(iphoneValue.getAverage())
                        .isEqualTo(3333);
                assertThat(milkValue.getMax())
                        .isNull();
                assertThat(iphoneValue.getMax())
                        .isNull();
                assertThat(milkValue.getMin())
                        .isNull();
                assertThat(iphoneValue.getMin())
                        .isNull();

                List<FacetValue> quantityValues = facetResult.getValues().stream().filter(x -> "Q1".equals(x.getName())).collect(Collectors.toList());

                milkValue = quantityValues.stream().filter(x -> "milk".equals(x.getRange())).findFirst().get();
                iphoneValue = quantityValues.stream().filter(x -> "iphone".equals(x.getRange())).findFirst().get();

                assertThat(milkValue.getCount())
                        .isEqualTo(2);
                assertThat(iphoneValue.getCount())
                        .isEqualTo(1);
                assertThat(milkValue.getSum())
                        .isEqualTo(8);
                assertThat(iphoneValue.getSum())
                        .isEqualTo(7777);
                assertThat(milkValue.getAverage())
                        .isNull();
                assertThat(iphoneValue.getAverage())
                        .isNull();
                assertThat(milkValue.getMax())
                        .isNull();
                assertThat(iphoneValue.getMax())
                        .isNull();
                assertThat(milkValue.getMin())
                        .isNull();
                assertThat(iphoneValue.getMin())
                        .isNull();
            }

            try (IDocumentSession session = store.openSession()) {
                Map<String, FacetResult> r = session.query(Order.class, Orders_All.class)
                        .aggregateBy(f -> f.byField("region"))
                        .andAggregateBy(f -> f.byField("product")
                                .sumOn("total", "T1")
                                .sumOn("total", "T2")
                                .averageOn("total", "T2")
                                .sumOn("quantity", "Q1"))
                        .execute();

                FacetResult facetResult = r.get("region");
                assertThat(facetResult.getValues())
                        .hasSize(1);
                assertThat(facetResult.getValues().get(0).getName())
                        .isNull();
                assertThat(facetResult.getValues().get(0).getCount())
                        .isEqualTo(3);

                facetResult = r.get("product");
                List<FacetValue> totalValues = facetResult.getValues().stream().filter(x -> "T1".equals(x.getName())).collect(Collectors.toList());

                assertThat(totalValues)
                        .hasSize(2);

                FacetValue milkValue = totalValues.stream().filter(x -> "milk".equals(x.getRange())).findFirst().get();
                FacetValue iphoneValue = totalValues.stream().filter(x -> "iphone".equals(x.getRange())).findFirst().get();

                assertThat(milkValue.getCount())
                        .isEqualTo(2);
                assertThat(iphoneValue.getCount())
                        .isEqualTo(1);
                assertThat(milkValue.getSum())
                        .isEqualTo(12);
                assertThat(iphoneValue.getSum())
                        .isEqualTo(3333);
                assertThat(milkValue.getAverage())
                        .isNull();
                assertThat(iphoneValue.getAverage())
                        .isNull();
                assertThat(milkValue.getMax())
                        .isNull();
                assertThat(iphoneValue.getMax())
                        .isNull();
                assertThat(milkValue.getMin())
                        .isNull();
                assertThat(iphoneValue.getMin())
                        .isNull();

                totalValues = facetResult.getValues().stream().filter(x -> "T2".equals(x.getName())).collect(Collectors.toList());

                assertThat(totalValues)
                        .hasSize(2);

                milkValue = totalValues.stream().filter(x -> "milk".equals(x.getRange())).findFirst().get();
                iphoneValue = totalValues.stream().filter(x -> "iphone".equals(x.getRange())).findFirst().get();

                assertThat(milkValue.getCount())
                        .isEqualTo(2);
                assertThat(iphoneValue.getCount())
                        .isEqualTo(1);
                assertThat(milkValue.getSum())
                        .isEqualTo(12);
                assertThat(iphoneValue.getSum())
                        .isEqualTo(3333);
                assertThat(milkValue.getAverage())
                        .isEqualTo(6);
                assertThat(iphoneValue.getAverage())
                        .isEqualTo(3333);
                assertThat(milkValue.getMax())
                        .isNull();
                assertThat(iphoneValue.getMax())
                        .isNull();
                assertThat(milkValue.getMin())
                        .isNull();
                assertThat(iphoneValue.getMin())
                        .isNull();

                List<FacetValue> quantityValues = facetResult.getValues().stream().filter(x -> "Q1".equals(x.getName())).collect(Collectors.toList());

                milkValue = quantityValues.stream().filter(x -> "milk".equals(x.getRange())).findFirst().get();
                iphoneValue = quantityValues.stream().filter(x -> "iphone".equals(x.getRange())).findFirst().get();

                assertThat(milkValue.getCount())
                        .isEqualTo(2);
                assertThat(iphoneValue.getCount())
                        .isEqualTo(1);
                assertThat(milkValue.getSum())
                        .isEqualTo(8);
                assertThat(iphoneValue.getSum())
                        .isEqualTo(7777);
                assertThat(milkValue.getAverage())
                        .isNull();
                assertThat(iphoneValue.getAverage())
                        .isNull();
                assertThat(milkValue.getMax())
                        .isNull();
                assertThat(iphoneValue.getMax())
                        .isNull();
                assertThat(milkValue.getMin())
                        .isNull();
                assertThat(iphoneValue.getMin())
                        .isNull();
            }
        }
    }

    @Test
    public void canCorrectlyAggregate_Ranges() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            new Orders_All().execute(store);

            try (IDocumentSession session = store.openSession()) {
                Order order1 = new Order();
                order1.setCurrency(Currency.EUR);
                order1.setProduct("Milk");
                order1.setQuantity(3);
                order1.setTotal(3);

                session.store(order1);

                Order order2 = new Order();
                order2.setCurrency(Currency.NIS);
                order2.setProduct("Milk");
                order2.setQuantity(5);
                order2.setTotal(9);

                session.store(order2);

                Order order3 = new Order();
                order3.setCurrency(Currency.EUR);
                order3.setProduct("iPhone");
                order3.setQuantity(7777);
                order3.setTotal(3333);

                session.store(order3);
                session.saveChanges();
            }

            waitForIndexing(store);

            try (IDocumentSession session = store.openSession()) {
                RangeBuilder<Integer> range = RangeBuilder.forPath("total");

                Map<String, FacetResult> r = session.query(Order.class, Query.index("Orders/All"))
                        .aggregateBy(f -> f.byRanges(
                                range.isLessThan(100),
                                range.isGreaterThanOrEqualTo(100).isLessThan(500),
                                range.isGreaterThanOrEqualTo(500).isLessThan(1500),
                                range.isGreaterThanOrEqualTo(1500)
                        )
                                .sumOn("total")
                                .averageOn("total")
                                .sumOn("quantity"))
                        .execute();

                FacetResult facetResult = r.get("total");

                assertThat(facetResult.getValues())
                        .hasSize(8);

                FacetValue range1 = facetResult.getValues().stream().filter(x -> x.getRange().equals("total < 100") && x.getName().equals("total")).findFirst().get();
                FacetValue range2 = facetResult.getValues().stream().filter(x -> x.getRange().equals("total >= 1500") && x.getName().equals("total")).findFirst().get();

                assertThat(range1.getCount())
                        .isEqualTo(2);
                assertThat(range2.getCount())
                        .isEqualTo(1);
                assertThat(range1.getSum())
                        .isEqualTo(12);
                assertThat(range2.getSum())
                        .isEqualTo(3333);
                assertThat(range1.getAverage())
                        .isEqualTo(6);
                assertThat(range2.getAverage())
                        .isEqualTo(3333);
                assertThat(range1.getMax())
                        .isNull();
                assertThat(range2.getMax())
                        .isNull();
                assertThat(range1.getMin())
                        .isNull();
                assertThat(range2.getMin())
                        .isNull();

                range1 = facetResult.getValues().stream().filter(x -> x.getRange().equals("total < 100") && x.getName().equals("quantity")).findFirst().get();
                range2 = facetResult.getValues().stream().filter(x -> x.getRange().equals("total >= 1500") && x.getName().equals("quantity")).findFirst().get();

                assertThat(range1.getCount())
                        .isEqualTo(2);
                assertThat(range2.getCount())
                        .isEqualTo(1);
                assertThat(range1.getSum())
                        .isEqualTo(8);
                assertThat(range2.getSum())
                        .isEqualTo(7777);
                assertThat(range1.getAverage())
                        .isNull();
                assertThat(range2.getAverage())
                        .isNull();
                assertThat(range1.getMax())
                        .isNull();
                assertThat(range2.getMax())
                        .isNull();
                assertThat(range1.getMin())
                        .isNull();
                assertThat(range2.getMin())
                        .isNull();
            }

            try (IDocumentSession session = store.openSession()) {
                RangeBuilder<Integer> range = RangeBuilder.forPath("total");

                Map<String, FacetResult> r = session.query(Order.class, Query.index("Orders/All"))
                        .aggregateBy(f -> f.byRanges(
                                range.isLessThan(100),
                                range.isGreaterThanOrEqualTo(100).isLessThan(500),
                                range.isGreaterThanOrEqualTo(500).isLessThan(1500),
                                range.isGreaterThanOrEqualTo(1500)
                        )
                                .sumOn("total", "T1")
                                .averageOn("total", "T1")
                                .sumOn("quantity", "Q1"))
                        .execute();

                FacetResult facetResult = r.get("total");

                assertThat(facetResult.getValues())
                        .hasSize(8);

                FacetValue range1 = facetResult.getValues().stream().filter(x -> x.getRange().equals("total < 100") && x.getName().equals("T1")).findFirst().get();
                FacetValue range2 = facetResult.getValues().stream().filter(x -> x.getRange().equals("total >= 1500") && x.getName().equals("T1")).findFirst().get();

                assertThat(range1.getCount())
                        .isEqualTo(2);
                assertThat(range2.getCount())
                        .isEqualTo(1);
                assertThat(range1.getSum())
                        .isEqualTo(12);
                assertThat(range2.getSum())
                        .isEqualTo(3333);
                assertThat(range1.getAverage())
                        .isEqualTo(6);
                assertThat(range2.getAverage())
                        .isEqualTo(3333);
                assertThat(range1.getMax())
                        .isNull();
                assertThat(range2.getMax())
                        .isNull();
                assertThat(range1.getMin())
                        .isNull();
                assertThat(range2.getMin())
                        .isNull();

                range1 = facetResult.getValues().stream().filter(x -> x.getRange().equals("total < 100") && x.getName().equals("Q1")).findFirst().get();
                range2 = facetResult.getValues().stream().filter(x -> x.getRange().equals("total >= 1500") && x.getName().equals("Q1")).findFirst().get();

                assertThat(range1.getCount())
                        .isEqualTo(2);
                assertThat(range2.getCount())
                        .isEqualTo(1);
                assertThat(range1.getSum())
                        .isEqualTo(8);
                assertThat(range2.getSum())
                        .isEqualTo(7777);
                assertThat(range1.getAverage())
                        .isNull();
                assertThat(range2.getAverage())
                        .isNull();
                assertThat(range1.getMax())
                        .isNull();
                assertThat(range2.getMax())
                        .isNull();
                assertThat(range1.getMin())
                        .isNull();
                assertThat(range2.getMin())
                        .isNull();
            }

            try (IDocumentSession session = store.openSession()) {
                RangeBuilder<Integer> range = RangeBuilder.forPath("total");

                Map<String, FacetResult> r = session.query(Order.class, Query.index("Orders/All"))
                        .aggregateBy(f -> f.byRanges(
                                range.isLessThan(100),
                                range.isGreaterThanOrEqualTo(100).isLessThan(500),
                                range.isGreaterThanOrEqualTo(500).isLessThan(1500),
                                range.isGreaterThanOrEqualTo(1500)
                        )
                                .sumOn("total", "T1")
                                .sumOn("total", "T2")
                                .averageOn("total", "T2")
                                .sumOn("quantity", "Q1"))
                        .execute();

                FacetResult facetResult = r.get("total");
                assertThat(facetResult.getValues())
                        .hasSize(12);

                FacetValue range1 = facetResult.getValues().stream().filter(x -> x.getRange().equals("total < 100") && x.getName().equals("T1")).findFirst().get();
                FacetValue range2 = facetResult.getValues().stream().filter(x -> x.getRange().equals("total >= 1500") && x.getName().equals("T1")).findFirst().get();

                assertThat(range1.getCount())
                        .isEqualTo(2);
                assertThat(range2.getCount())
                        .isEqualTo(1);
                assertThat(range1.getSum())
                        .isEqualTo(12);
                assertThat(range2.getSum())
                        .isEqualTo(3333);
                assertThat(range1.getAverage())
                        .isNull();
                assertThat(range2.getAverage())
                        .isNull();
                assertThat(range1.getMax())
                        .isNull();
                assertThat(range2.getMax())
                        .isNull();
                assertThat(range1.getMin())
                        .isNull();
                assertThat(range2.getMin())
                        .isNull();

                range1 = facetResult.getValues().stream().filter(x -> x.getRange().equals("total < 100") && x.getName().equals("T2")).findFirst().get();
                range2 = facetResult.getValues().stream().filter(x -> x.getRange().equals("total >= 1500") && x.getName().equals("T2")).findFirst().get();

                assertThat(range1.getCount())
                        .isEqualTo(2);
                assertThat(range2.getCount())
                        .isEqualTo(1);
                assertThat(range1.getSum())
                        .isEqualTo(12);
                assertThat(range2.getSum())
                        .isEqualTo(3333);
                assertThat(range1.getAverage())
                        .isEqualTo(6);
                assertThat(range2.getAverage())
                        .isEqualTo(3333);
                assertThat(range1.getMax())
                        .isNull();
                assertThat(range2.getMax())
                        .isNull();
                assertThat(range1.getMin())
                        .isNull();
                assertThat(range2.getMin())
                        .isNull();

                range1 = facetResult.getValues().stream().filter(x -> x.getRange().equals("total < 100") && x.getName().equals("Q1")).findFirst().get();
                range2 = facetResult.getValues().stream().filter(x -> x.getRange().equals("total >= 1500") && x.getName().equals("Q1")).findFirst().get();

                assertThat(range1.getCount())
                        .isEqualTo(2);
                assertThat(range2.getCount())
                        .isEqualTo(1);
                assertThat(range1.getSum())
                        .isEqualTo(8);
                assertThat(range2.getSum())
                        .isEqualTo(7777);
                assertThat(range1.getAverage())
                        .isNull();
                assertThat(range2.getAverage())
                        .isNull();
                assertThat(range1.getMax())
                        .isNull();
                assertThat(range2.getMax())
                        .isNull();
                assertThat(range1.getMin())
                        .isNull();
                assertThat(range2.getMin())
                        .isNull();
            }
        }
    }

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
}
