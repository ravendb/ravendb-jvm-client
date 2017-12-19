package net.ravendb.client.documents.session;

public interface IDocumentQueryBaseSingle<T> {
    //TBD  Lazy<int> CountLazily();

    /**
     * Returns first element or throws if sequence is empty.
     */
    T first();

    /**
     * Returns first element or default value for type if sequence is empty.
     */
    T firstOrDefault();

    /**
     * Returns first element or throws if sequence is empty or contains more than one element.
     */
    T single();

    /**
     * Returns first element or default value for given type if sequence is empty. Throws if sequence contains more than
     * one element.
     */
    T singleOrDefault();

    /**
     * Gets the total count of records for this query
     */
    int count();

    //TBD Lazy<IEnumerable<T>> Lazily(Action<IEnumerable<T>> onEval);
}
