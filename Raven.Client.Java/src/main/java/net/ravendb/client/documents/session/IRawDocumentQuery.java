package net.ravendb.client.documents.session;

public interface IRawDocumentQuery<T> extends IQueryBase<T, IRawDocumentQuery<T>>, IDocumentQueryBaseSingle<T>, IEnumerableQuery<T> {

    /* TODO
     /// <summary>
        /// Add a named parameter to the query
        /// </summary>
        IDocumentQuery<T> AddParameter(string name, object value);
     */
}
