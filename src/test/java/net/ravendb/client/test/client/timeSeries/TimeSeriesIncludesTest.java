package net.ravendb.client.test.client.timeSeries;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.ISessionDocumentTimeSeries;
import net.ravendb.client.documents.session.timeSeries.TimeSeriesEntry;
import net.ravendb.client.infrastructure.entities.Company;
import net.ravendb.client.infrastructure.entities.Order;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.Test;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class TimeSeriesIncludesTest extends RemoteTestBase {

    @Test
    public void sessionLoadWithIncludeTimeSeries() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            Date baseLine = DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH);

            try (IDocumentSession session = store.openSession()) {
                Company company = new Company();
                company.setName("HR");
                session.store(company, "companies/1-A");

                Order order = new Order();
                order.setCompany("companies/1-A");
                session.store(order, "orders/1-A");

                ISessionDocumentTimeSeries tsf = session.timeSeriesFor("orders/1-A", "Heartrate");
                tsf.append(baseLine, 67, "watches/apple");
                tsf.append(DateUtils.addMinutes(baseLine, 5), 64, "watches/apple");
                tsf.append(DateUtils.addMinutes(baseLine, 10), 65, "watches/fitbit");

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                Order order = session.load(Order.class, "orders/1-A",
                        i -> i.includeDocuments("company")
                                .includeTimeSeries("Heartrate", null, null));

                Company company = session.load(Company.class, order.getCompany());
                assertThat(company.getName())
                        .isEqualTo("HR");

                // should not go to server
                List<TimeSeriesEntry> values = session.timeSeriesFor(order, "Heartrate")
                        .get(null, null);

                assertThat(values)
                        .hasSize(3);

                assertThat(values.get(0).getValues())
                        .hasSize(1);
                assertThat(values.get(0).getValues()[0])
                        .isEqualTo(67);
                assertThat(values.get(0).getTag())
                        .isEqualTo("watches/apple");
                assertThat(values.get(0).getTimestamp())
                        .isEqualTo(baseLine);

                assertThat(values.get(1).getValues())
                        .hasSize(1);
                assertThat(values.get(1).getValues()[0])
                        .isEqualTo(64);
                assertThat(values.get(1).getTag())
                        .isEqualTo("watches/apple");
                assertThat(values.get(1).getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 5));

                assertThat(values.get(2).getValues())
                        .hasSize(1);
                assertThat(values.get(2).getValues()[0])
                        .isEqualTo(65);
                assertThat(values.get(2).getTag())
                        .isEqualTo("watches/fitbit");
                assertThat(values.get(2).getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 10));

                assertThat(session.advanced().getNumberOfRequests())
                        .isOne();
            }
        }
    }

    /* TODO copy other tests from latest version

     */
    public static class User {
        private String name;
        private String worksAt;
        private String id;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getWorksAt() {
            return worksAt;
        }

        public void setWorksAt(String worksAt) {
            this.worksAt = worksAt;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }
    }
}
