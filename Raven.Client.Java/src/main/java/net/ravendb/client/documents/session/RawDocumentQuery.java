package net.ravendb.client.documents.session;

public class RawDocumentQuery<T> extends AbstractDocumentQuery<T, RawDocumentQuery<T>> implements IRawDocumentQuery<T> {

    public RawDocumentQuery(Class<T> clazz, InMemoryDocumentSessionOperations session, String rawQuery) {
        super(clazz, session, null, null, false, null, null, null);
        this.queryRaw = rawQuery;
    }

    public IRawDocumentQuery<T> skip(int count) {
        _skip(count);
        return this;
    }

    @Override
    public IRawDocumentQuery<T> take(int count) {
        _take(count);
        return this;
    }

    @Override
    public RawDocumentQuery<T> waitForNonStaleResults() {
        _waitForNonStaleResults();
        return this;
    }

    //TODO: other interface specific methods from DocumentQuery
}
