package net.ravendb.client.documents.operations.compareExchange;

import net.ravendb.client.documents.session.IMetadataDictionary;

public interface ICompareExchangeValue {
    /**
     * The unique key of the compare request entry.
     */
    String getKey();
    /**
     * Current index of the entry, used for concurrency checks.
     */
    long getIndex();
    void setIndex(long index);

    /**
     * The value of the entry.
     */
    Object getValue();

    /**
     * Access to the metadata of the compare exchange value.
     */
    IMetadataDictionary getMetadata();
    boolean hasMetadata();
}
