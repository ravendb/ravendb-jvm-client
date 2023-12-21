package net.ravendb.client.documents.subscriptions;

import net.ravendb.client.documents.dataArchival.ArchivedDataProcessingBehavior;

public class SubscriptionTryout {
    private String changeVector;
    private String query;
    private ArchivedDataProcessingBehavior archivedDataProcessingBehavior;

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

    public ArchivedDataProcessingBehavior getArchivedDataProcessingBehavior() {
        return archivedDataProcessingBehavior;
    }

    public void setArchivedDataProcessingBehavior(ArchivedDataProcessingBehavior archivedDataProcessingBehavior) {
        this.archivedDataProcessingBehavior = archivedDataProcessingBehavior;
    }
}
