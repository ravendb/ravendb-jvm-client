package net.ravendb.client.documents.session;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class ResponseTimeInformation {

    private Duration totalServerDuration;
    private Duration totalClientDuration;

    private List<ResponseTimeItem> durationBreakdown;

    public void computeServerTotal() {
        totalServerDuration = durationBreakdown.stream().map(x -> x.getDuration()).reduce(Duration.ZERO, (prev, next) -> prev.plus(next));
    }

    public ResponseTimeInformation() {
        totalServerDuration = Duration.ZERO;
        totalClientDuration = Duration.ZERO;
        durationBreakdown = new ArrayList<>();
    }

    public Duration getTotalServerDuration() {
        return totalServerDuration;
    }

    public void setTotalServerDuration(Duration totalServerDuration) {
        this.totalServerDuration = totalServerDuration;
    }

    public Duration getTotalClientDuration() {
        return totalClientDuration;
    }

    public void setTotalClientDuration(Duration totalClientDuration) {
        this.totalClientDuration = totalClientDuration;
    }

    public List<ResponseTimeItem> getDurationBreakdown() {
        return durationBreakdown;
    }

    public void setDurationBreakdown(List<ResponseTimeItem> durationBreakdown) {
        this.durationBreakdown = durationBreakdown;
    }

    public static class ResponseTimeItem {
        private String url;
        private Duration duration;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public Duration getDuration() {
            return duration;
        }

        public void setDuration(Duration duration) {
            this.duration = duration;
        }
    }
}
