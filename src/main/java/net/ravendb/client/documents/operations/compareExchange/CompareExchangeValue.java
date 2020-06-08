package net.ravendb.client.documents.operations.compareExchange;

import net.ravendb.client.documents.session.IMetadataDictionary;
import net.ravendb.client.json.MetadataAsDictionary;

public class CompareExchangeValue<T> implements ICompareExchangeValue {
    private String key;
    private long index;
    private T value;
    private IMetadataDictionary metadataAsDictionary;

    public CompareExchangeValue(String key, long index, T value) {
        this(key, index, value, null);
    }

    public CompareExchangeValue(String key, long index, T value, IMetadataDictionary metadata) {
        this.key = key;
        this.index = index;
        this.value = value;
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
