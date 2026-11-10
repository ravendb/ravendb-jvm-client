package net.ravendb.client.documents.subscriptions;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class SubscriptionUpdateOptions extends SubscriptionCreationOptions {
    private Long id;
    private boolean createNew;
    private boolean pinToMentorNodeWasSet;
    private boolean disabledWasSet;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public boolean isCreateNew() {
        return createNew;
    }

    public void setCreateNew(boolean createNew) {
        this.createNew = createNew;
    }

    @Override
    public void setPinToMentorNode(boolean pinToMentorNode) {
        super.setPinToMentorNode(pinToMentorNode);
        pinToMentorNodeWasSet = true;
    }

    /**
     * @return true if pinToMentorNode was explicitly set, only then it is sent to the server on update
     */
    @JsonIgnore
    public boolean isPinToMentorNodeWasSet() {
        return pinToMentorNodeWasSet;
    }

    @Override
    public void setDisabled(boolean disabled) {
        super.setDisabled(disabled);
        disabledWasSet = true;
    }

    /**
     * @return true if disabled was explicitly set, only then it is sent to the server on update
     */
    @JsonIgnore
    public boolean isDisabledWasSet() {
        return disabledWasSet;
    }
}
