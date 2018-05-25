package net.ravendb.client.documents.subscriptions;

public class GetSubscriptionsResult {
    private SubscriptionState[] results;

    public SubscriptionState[] getResults() {
        return results;
    }

    public void setResults(SubscriptionState[] results) {
        this.results = results;
    }

}
