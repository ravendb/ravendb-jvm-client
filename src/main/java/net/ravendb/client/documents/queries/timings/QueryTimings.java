package net.ravendb.client.documents.queries.timings;

import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.documents.queries.QueryResult;

import java.util.Map;

public class QueryTimings {

    private long durationInMs;
    private Map<String, QueryTimings> timings;
    private ObjectNode queryPlan;

    public long getDurationInMs() {
        return durationInMs;
    }

    public void setDurationInMs(long durationInMs) {
        this.durationInMs = durationInMs;
    }

    public Map<String, QueryTimings> getTimings() {
        return timings;
    }

    public void setTimings(Map<String, QueryTimings> timings) {
        this.timings = timings;
    }

    public ObjectNode getQueryPlan() {
        return queryPlan;
    }

    public void setQueryPlan(ObjectNode queryPlan) {
        this.queryPlan = queryPlan;
    }

    public void update(QueryResult queryResult) {
        durationInMs = 0;
        timings = null;

        if (queryResult.getTimings() == null) {
            return;
        }

        durationInMs = queryResult.getTimings().getDurationInMs();
        timings = queryResult.getTimings().getTimings();
    }
}
