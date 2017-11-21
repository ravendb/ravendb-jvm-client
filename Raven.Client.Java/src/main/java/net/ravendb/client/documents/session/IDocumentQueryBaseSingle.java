package net.ravendb.client.documents.session;

public interface IDocumentQueryBaseSingle<T> {
    /* TODO
     /// <summary>
        ///     Register the query as a lazy-count query in the session and return a lazy
        ///     instance that will evaluate the query only when needed.
        /// </summary>
        Lazy<int> CountLazily();*/

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
    /* TODO

        /// <summary>
        ///     Register the query as a lazy query in the session and return a lazy
        ///     instance that will evaluate the query only when needed
        /// </summary>
        Lazy<IEnumerable<T>> Lazily();

        /// <summary>
        ///     Register the query as a lazy query in the session and return a lazy
        ///     instance that will evaluate the query only when needed.
        ///     Also provide a function to execute when the value is evaluated
        /// </summary>
        Lazy<IEnumerable<T>> Lazily(Action<IEnumerable<T>> onEval);
     */
}
