package net.ravendb.client.documents;

import java.util.function.Supplier;

public class Lazy<T> {
    private final Supplier<T> valueFactory;
    private volatile boolean valueCreated = false;
    private T value;

    public Lazy(Supplier<T> valueFactory) {
        this.valueFactory = valueFactory;
    }

    public boolean isValueCreated() {
        return valueCreated;
    }

    public T getValue() {
        if (valueCreated) {
            return value;
        }
        synchronized (this) {
            if (!valueCreated) {
                value = valueFactory.get();
                valueCreated = true;
            }
        }

        return value;
    }
}
