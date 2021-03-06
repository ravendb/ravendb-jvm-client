package net.ravendb.client.documents.operations;

public class DetailedDatabaseStatistics extends DatabaseStatistics {

    private long countOfIdentities;
    private long countOfCompareExchange;
    private long countOfCompareExchangeTombstones;

    /**
     * @return Total number of identities in database.
     */
    public long getCountOfIdentities() {
        return countOfIdentities;
    }

    /**
     * @param countOfIdentities Total number of identities in database.
     */
    public void setCountOfIdentities(long countOfIdentities) {
        this.countOfIdentities = countOfIdentities;
    }

    /**
     * @return Total number of compare-exchange values in database.
     */
    public long getCountOfCompareExchange() {
        return countOfCompareExchange;
    }

    /**
     * @param countOfCompareExchange Total number of compare-exchange values in database.
     */
    public void setCountOfCompareExchange(long countOfCompareExchange) {
        this.countOfCompareExchange = countOfCompareExchange;
    }

    /**
     * @return Total number of compare-exchange tombstones values in database.
     */
    public long getCountOfCompareExchangeTombstones() {
        return countOfCompareExchangeTombstones;
    }

    /**
     * @param countOfCompareExchangeTombstones Total number of compare-exchange tombstones values in database.
     */
    public void setCountOfCompareExchangeTombstones(long countOfCompareExchangeTombstones) {
        this.countOfCompareExchangeTombstones = countOfCompareExchangeTombstones;
    }
}
