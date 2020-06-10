package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.session.timeSeries.TimeSeriesValue;
import net.ravendb.client.infrastructure.entities.Company;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RavenDB_15129Test extends RemoteTestBase {

    @Test
    public void timeSeriesValue_RequiresDoubleType() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            assertThatThrownBy(() -> {
                store.timeSeries().register(Company.class, MetricValue.class);
            })
                    .hasMessageContaining("Cannot create a mapping for");
        }
    }

    public static class MetricValue {
        @TimeSeriesValue(idx = 0)
        private long durationInMs;
        @TimeSeriesValue(idx = 1)
        private long requestSize;
        @TimeSeriesValue(idx = 2)
        private String sourceIp;

        public long getDurationInMs() {
            return durationInMs;
        }

        public void setDurationInMs(long durationInMs) {
            this.durationInMs = durationInMs;
        }

        public long getRequestSize() {
            return requestSize;
        }

        public void setRequestSize(long requestSize) {
            this.requestSize = requestSize;
        }

        public String getSourceIp() {
            return sourceIp;
        }

        public void setSourceIp(String sourceIp) {
            this.sourceIp = sourceIp;
        }
    }

}
