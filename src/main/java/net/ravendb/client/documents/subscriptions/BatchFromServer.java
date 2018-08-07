package net.ravendb.client.documents.subscriptions;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;

public class BatchFromServer {
    private List<SubscriptionConnectionServerMessage> messages;
    private List<ObjectNode> includes;

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
}
