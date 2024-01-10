package net.ravendb.client.documents.subscriptions;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.Map;

public class BatchFromServer {
    private List<SubscriptionConnectionServerMessage> messages;
    private List<ObjectNode> includes;
    private List<CounterIncludeItem> counterIncludes;
    private List<ObjectNode> timeSeriesIncludes;

    public List<SubscriptionConnectionServerMessage> getMessages() {
        return messages;
    }

    public void setMessages(List<SubscriptionConnectionServerMessage> messages) {
        this.messages = messages;
    }

    public List<ObjectNode> getIncludes() {
        return includes;
    }

    public void setIncludes(List<ObjectNode> includes) {
        this.includes = includes;
    }

    public List<CounterIncludeItem> getCounterIncludes() {
        return counterIncludes;
    }

    public void setCounterIncludes(List<CounterIncludeItem> counterIncludes) {
        this.counterIncludes = counterIncludes;
    }

    public List<ObjectNode> getTimeSeriesIncludes() {
        return timeSeriesIncludes;
    }

    public void setTimeSeriesIncludes(List<ObjectNode> timeSeriesIncludes) {
        this.timeSeriesIncludes = timeSeriesIncludes;
    }

    public static class CounterIncludeItem {
        private final ObjectNode includes;
        private final Map<String, String[]> counterIncludes;

        public CounterIncludeItem(ObjectNode includes, Map<String, String[]> counterIncludes) {
            this.includes = includes;
            this.counterIncludes = counterIncludes;
        }

        public ObjectNode getIncludes() {
            return includes;
        }

        public Map<String, String[]> getCounterIncludes() {
            return counterIncludes;
        }
    }
}
