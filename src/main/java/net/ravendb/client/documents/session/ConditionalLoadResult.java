package net.ravendb.client.documents.session;

public class ConditionalLoadResult<T> {
    private T entity;
    private String changeVector;

    private ConditionalLoadResult() {
    }

    public T getEntity() {
        return entity;
    }

    public String getChangeVector() {
        return changeVector;
    }

    public static <T> ConditionalLoadResult<T> create(T entity, String changeVector) {
        ConditionalLoadResult<T> result = new ConditionalLoadResult<>();
        result.entity = entity;
        result.changeVector = changeVector;
        return result;
    }
}
