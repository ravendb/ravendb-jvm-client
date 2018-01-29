package net.ravendb.client.documents.operations.compareExchange;

public class CompareExchangeValue<T> {
    private String key;
    private long index;
    private T value;

    public CompareExchangeValue(String key, long index, T value) {
        this.key = key;
        this.index = index;
        this.value = value;
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
}
