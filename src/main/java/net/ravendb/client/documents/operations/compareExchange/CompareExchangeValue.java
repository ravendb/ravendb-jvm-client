package net.ravendb.client.documents.operations.compareExchange;

import net.ravendb.client.documents.session.IMetadataDictionary;
import net.ravendb.client.json.MetadataAsDictionary;

public class CompareExchangeValue<T> implements ICompareExchangeValue {
    /**
     * @see ICompareExchangeValue#getKey()
     */
    private String key;
    /**
     * @see ICompareExchangeValue#getIndex()
     */
    private long index;
    /**
     * @see ICompareExchangeValue#getValue()
     */
    private T value;
    /**
     * The change vector for the compare-exchange value, used for concurrency checks.
     */
    private String changeVector;
    /**
     * @see ICompareExchangeValue#getMetadata()
     */
    private IMetadataDictionary metadataAsDictionary;

    public CompareExchangeValue(String key, long index, T value) {
        this(key, index, value, null);
    }

    public CompareExchangeValue(String key, long index, T value, IMetadataDictionary metadata) {
        this(key, index, value, null, metadata);
    }

    /**
     * Initializes a new instance of the {@link CompareExchangeValue} class with the specified key, index, value, and optional metadata.
     *
     * @param key The unique key of the compare-exchange value.
     * @param index The index used for optimistic concurrency control; used for concurrency checks.
     * @param value The value associated with the specified compare-exchange key.
     * @param metadata Optional metadata associated with the compare-exchange value.
     */
    public CompareExchangeValue(String key, long index, T value, String changeVector, IMetadataDictionary metadata) {
        this.key = key;
        this.index = index;
        this.value = value;
        this.changeVector = changeVector;
        this.metadataAsDictionary = metadata;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public long getIndex() {
        return index;
    }

    public void setIndex(long index) {
        this.index = index;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    public String getChangeVector() {
        return changeVector;
    }

    public void setChangeVector(String changeVector) {
        this.changeVector = changeVector;
    }

    public IMetadataDictionary getMetadata() {
        if (metadataAsDictionary == null) {
            metadataAsDictionary = new MetadataAsDictionary();
        }

        return metadataAsDictionary;
    }

    public boolean hasMetadata() {
        return metadataAsDictionary != null;
    }
}
