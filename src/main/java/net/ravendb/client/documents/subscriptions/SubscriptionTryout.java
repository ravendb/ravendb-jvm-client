package net.ravendb.client.documents.subscriptions;

public class SubscriptionTryout {
    private String changeVector;
    private String query;

    public String getChangeVector() {
        return changeVector;
    }

    public void setChangeVector(String changeVector) {
        this.changeVector = changeVector;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }
}
