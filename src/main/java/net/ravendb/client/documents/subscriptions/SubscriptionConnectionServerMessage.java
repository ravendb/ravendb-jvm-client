package net.ravendb.client.documents.subscriptions;

import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.primitives.UseSharpEnum;

import java.util.Map;

class SubscriptionConnectionServerMessage {

    @UseSharpEnum
    public enum MessageType {
        NONE,
        CONNECTION_STATUS,
        END_OF_BATCH,
        DATA,
        INCLUDES,
        COUNTER_INCLUDES,
        TIME_SERIES_INCLUDES,
        CONFIRM,
        ERROR
    }

    @UseSharpEnum
    public enum ConnectionStatus {
        NONE,
        ACCEPTED,
        IN_USE,
        CLOSED,
        NOT_FOUND,
        REDIRECT,
        FORBIDDEN_READ_ONLY,
        FORBIDDEN,
        INVALID,
        CONCURRENCY_RECONNECT
    }

    public static class SubscriptionRedirectData {
        private String currentTag;
        private String redirectedTag;
        private Map<String, String> reasons;
        private Long registerConnectionDurationInTicks;

        public String getCurrentTag() {
            return currentTag;
        }

        public void setCurrentTag(String currentTag) {
            this.currentTag = currentTag;
        }

        public String getRedirectedTag() {
            return redirectedTag;
        }

        public void setRedirectedTag(String redirectedTag) {
            this.redirectedTag = redirectedTag;
        }

        public Map<String, String> getReasons() {
            return reasons;
        }

        public void setReasons(Map<String, String> reasons) {
            this.reasons = reasons;
        }

        public Long getRegisterConnectionDurationInTicks() {
            return registerConnectionDurationInTicks;
        }

        public void setRegisterConnectionDurationInTicks(Long registerConnectionDurationInTicks) {
            this.registerConnectionDurationInTicks = registerConnectionDurationInTicks;
        }
    }

    private MessageType type;
    private ConnectionStatus status;
    private ObjectNode data;
    private ObjectNode includes;
    private ObjectNode counterIncludes;
    private Map<String, String[]> includedCounterNames;
    private ObjectNode timeSeriesIncludes;
    private String exception;
    private String message;

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public ConnectionStatus getStatus() {
        return status;
    }

    public void setStatus(ConnectionStatus status) {
        this.status = status;
    }

    public ObjectNode getData() {
        return data;
    }

    public void setData(ObjectNode data) {
        this.data = data;
    }

    public ObjectNode getIncludes() {
        return includes;
    }

    public void setIncludes(ObjectNode includes) {
        this.includes = includes;
    }

    public ObjectNode getCounterIncludes() {
        return counterIncludes;
    }

    public void setCounterIncludes(ObjectNode counterIncludes) {
        this.counterIncludes = counterIncludes;
    }

    public Map<String, String[]> getIncludedCounterNames() {
        return includedCounterNames;
    }

    public void setIncludedCounterNames(Map<String, String[]> includedCounterNames) {
        this.includedCounterNames = includedCounterNames;
    }

    public ObjectNode getTimeSeriesIncludes() {
        return timeSeriesIncludes;
    }

    public void setTimeSeriesIncludes(ObjectNode timeSeriesIncludes) {
        this.timeSeriesIncludes = timeSeriesIncludes;
    }

    public String getException() {
        return exception;
    }

    public void setException(String exception) {
        this.exception = exception;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
