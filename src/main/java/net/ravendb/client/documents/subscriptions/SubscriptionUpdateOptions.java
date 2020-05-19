package net.ravendb.client.documents.subscriptions;

public class SubscriptionUpdateOptions extends SubscriptionCreationOptions {
    private Long id;
    private boolean createNew;

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
