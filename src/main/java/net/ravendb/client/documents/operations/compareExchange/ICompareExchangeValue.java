package net.ravendb.client.documents.operations.compareExchange;

import net.ravendb.client.documents.session.IMetadataDictionary;

public interface ICompareExchangeValue {
    String getKey();
    long getIndex();
    void setIndex(long index);
    Object getValue();
    IMetadataDictionary getMetadata();
    boolean hasMetadata();
}
