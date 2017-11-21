package net.ravendb.client.documents.session;

public interface IRawDocumentQuery<T> extends IQueryBase<T, IRawDocumentQuery<T>>, IDocumentQueryBaseSingle<T>, IEnumerableQuery<T> {

    /**
     * Add a named parameter to the query
     */
    IRawDocumentQuery<T> addParameter(String name, Object value);
}
