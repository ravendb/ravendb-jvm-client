package net.ravendb.client.documents.subscriptions;

import net.ravendb.client.documents.session.loaders.ISubscriptionIncludeBuilder;

import java.util.function.Consumer;

public class SubscriptionCreationOptions {

    private String name;
    private String query;
    private Consumer<ISubscriptionIncludeBuilder> includes;
    private String changeVector;
    private String mentorNode;

    private boolean pinToMentorNode;
    private boolean disabled;

    public Consumer<ISubscriptionIncludeBuilder> getIncludes() {
        return includes;
    }

    public void setIncludes(Consumer<ISubscriptionIncludeBuilder> includes) {
        this.includes = includes;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getChangeVector() {
        return changeVector;
    }

    public void setChangeVector(String changeVector) {
        this.changeVector = changeVector;
    }

    public String getMentorNode() {
        return mentorNode;
    }

    public boolean isPinToMentorNode() {
        return pinToMentorNode;
    }

    public void setPinToMentorNode(boolean pinToMentorNode) {
        this.pinToMentorNode = pinToMentorNode;
    }

    public void setMentorNode(String mentorNode) {
        this.mentorNode = mentorNode;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }
}
