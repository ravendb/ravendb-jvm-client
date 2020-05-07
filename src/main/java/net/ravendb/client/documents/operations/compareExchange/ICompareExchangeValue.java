package net.ravendb.client.documents.operations.compareExchange;

public interface ICompareExchangeValue {
    String getKey();
    long getIndex();
    void setIndex(long index);
    Object getValue();
}
