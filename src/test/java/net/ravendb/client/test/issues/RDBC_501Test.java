package net.ravendb.client.test.issues;

import net.ravendb.client.RavenTestHelper;
import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.queries.timeSeries.TimeSeriesAggregationResult;
import net.ravendb.client.documents.queries.timeSeries.TypedTimeSeriesAggregationResult;
import net.ravendb.client.documents.queries.timeSeries.TypedTimeSeriesRangeAggregation;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.ISessionDocumentTypedTimeSeries;
import net.ravendb.client.documents.session.timeSeries.TimeSeriesValue;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

public class RDBC_501Test extends RemoteTestBase {

    @Test
    public void shouldProperlyMapTypedEntries() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            Date baseline = RavenTestHelper.utcToday();

            try (IDocumentSession session = store.openSession()) {
                MarkerSymbol symbol = new MarkerSymbol();
                session.store(symbol, "markerSymbols/1-A");

                SymbolPrice price1 = new SymbolPrice();
                price1.low = 1;
                price1.high = 10;
                price1.open = 4;
                price1.close = 7;

                SymbolPrice price2 = new SymbolPrice();
                price2.low = 21;
                price2.high = 210;
                price2.open = 24;
                price2.close = 27;

                SymbolPrice price3 = new SymbolPrice();
                price3.low = 321;
                price3.high = 310;
                price3.open = 34;
                price3.close = 37;

                ISessionDocumentTypedTimeSeries<SymbolPrice> tsf = session.timeSeriesFor(SymbolPrice.class, symbol, "history");

                tsf.append(DateUtils.addHours(baseline, 1), price1);
                tsf.append(DateUtils.addHours(baseline, 2), price2);
                tsf.append(DateUtils.addDays(baseline, 2), price3);

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                TimeSeriesAggregationResult aggregatedHistoryQueryResult = session.query(MarkerSymbol.class)
                        .selectTimeSeries(TimeSeriesAggregationResult.class, b -> b.raw("from history\n" +
                                "          group by '1 days'\n" +
                                "          select first(), last(), min(), max()"))
                        .first();

                assertThat(aggregatedHistoryQueryResult.getResults())
                        .hasSize(2);

                TypedTimeSeriesAggregationResult<SymbolPrice> typed = aggregatedHistoryQueryResult.asTypedResult(SymbolPrice.class);

                assertThat(typed.getResults())
                        .hasSize(2);

                TypedTimeSeriesRangeAggregation<SymbolPrice> firstResult = typed.getResults()[0];
                assertThat(firstResult.getMin().getOpen())
                        .isEqualTo(4);
                assertThat(firstResult.getMin().getClose())
                        .isEqualTo(7);
                assertThat(firstResult.getMin().getLow())
                        .isEqualTo(1);
                assertThat(firstResult.getMin().getHigh())
                        .isEqualTo(10);

                assertThat(firstResult.getFirst().getOpen())
                        .isEqualTo(4);
                assertThat(firstResult.getFirst().getClose())
                        .isEqualTo(7);
                assertThat(firstResult.getFirst().getLow())
                        .isEqualTo(1);
                assertThat(firstResult.getFirst().getHigh())
                        .isEqualTo(10);

                TypedTimeSeriesRangeAggregation<SymbolPrice> secondResult = typed.getResults()[1];

                assertThat(secondResult.getMin().getOpen())
                        .isEqualTo(34);
                assertThat(secondResult.getMin().getClose())
                        .isEqualTo(37);
                assertThat(secondResult.getMin().getLow())
                        .isEqualTo(321);
                assertThat(secondResult.getMin().getHigh())
                        .isEqualTo(310);
            }
        }
    }

    public static class SymbolPrice {
        @TimeSeriesValue(idx = 0)
        private double open;
        @TimeSeriesValue(idx = 1)
        private double close;
        @TimeSeriesValue(idx = 2)
        private double high;
        @TimeSeriesValue(idx = 3)
        private double low;

        public double getOpen() {
            return open;
        }

        public void setOpen(double open) {
            this.open = open;
        }

        public double getClose() {
            return close;
        }

        public void setClose(double close) {
            this.close = close;
        }

        public double getHigh() {
            return high;
        }

        public void setHigh(double high) {
            this.high = high;
        }

        public double getLow() {
            return low;
        }

        public void setLow(double low) {
            this.low = low;
        }
    }

    public static class MarkerSymbol {
        private String id;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }
    }
}
