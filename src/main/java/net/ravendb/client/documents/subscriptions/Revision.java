package net.ravendb.client.documents.subscriptions;

public class Revision<T> {
    private T previous;
    private T current;

    public T getPrevious() {
        return previous;
    }

    public void setPrevious(T previous) {
        this.previous = previous;
    }

    public T getCurrent() {
        return current;
    }

    public void setCurrent(T current) {
        this.current = current;
    }
}
