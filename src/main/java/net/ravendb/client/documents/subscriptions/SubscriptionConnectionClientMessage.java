package net.ravendb.client.documents.subscriptions;

import net.ravendb.client.primitives.UseSharpEnum;

class SubscriptionConnectionClientMessage {

    @UseSharpEnum
    public enum MessageType {
        NONE,
        ACKNOWLEDGE,
        DISPOSED_NOTIFICATION
    }

    private MessageType type;
    private String changeVector;

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public String getChangeVector() {
        return changeVector;
    }

    public void setChangeVector(String changeVector) {
        this.changeVector = changeVector;
    }
}
