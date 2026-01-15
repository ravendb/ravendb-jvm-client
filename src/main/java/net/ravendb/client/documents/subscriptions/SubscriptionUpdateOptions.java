package net.ravendb.client.documents.subscriptions;

public class SubscriptionUpdateOptions extends SubscriptionCreationOptions {
    private Long id;
    private boolean createNew;
    private boolean pinToMentorNode;
    private boolean pinToMentorWasSet;
    private boolean disabled;
    private boolean disabledWasSet;

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
        this.setDisabledWasSet(true);
    }

    public boolean isDisabledWasSet() {
        return disabledWasSet;
    }

    public void setDisabledWasSet(boolean disabledWasSet) {
        this.disabledWasSet = disabledWasSet;
    }

    public boolean isPinToMentorWasSet() {
        return pinToMentorWasSet;
    }

    public void setPinToMentorWasSet(boolean pinToMentorWasSet) {
        this.pinToMentorWasSet = pinToMentorWasSet;
    }

    @Override
    public boolean isPinToMentorNode() {
        return pinToMentorNode;
    }

    @Override
    public void setPinToMentorNode(boolean pinToMentorNode) {
        this.pinToMentorNode = pinToMentorNode;
        this.setPinToMentorWasSet(true);
    }

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
}
