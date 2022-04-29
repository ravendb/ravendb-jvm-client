package net.ravendb.client.exceptions.documents.subscriptions;

import java.util.Map;

public class SubscriptionDoesNotBelongToNodeException extends SubscriptionException {
    private String appropriateNode;
    private Map<String, String> reasons;

    private Long registerConnectionDurationInTicks;

    public SubscriptionDoesNotBelongToNodeException(String message) {
        super(message);
    }

    public SubscriptionDoesNotBelongToNodeException(String message, Throwable cause) {
        super(message, cause);
    }

    public String getAppropriateNode() {
        return appropriateNode;
    }

    public void setAppropriateNode(String appropriateNode) {
        this.appropriateNode = appropriateNode;
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
