package net.ravendb.client.documents.session;

//TODO:
public interface IDocumentQueryBaseSingle<T> {
    /* TODO
     /// <summary>
        ///     Register the query as a lazy-count query in the session and return a lazy
        ///     instance that will evaluate the query only when needed.
        /// </summary>
        Lazy<int> CountLazily();

        /// <summary>
        ///     Returns first element or throws if sequence is empty.
        /// </summary>
        T First();

        /// <summary>
        ///     Returns first element or default value for type if sequence is empty.
        /// </summary>
        T FirstOrDefault();

        /// <summary>
        ///     Returns first element or throws if sequence is empty or contains more than one element.
        /// </summary>
        T Single();

        /// <summary>
        ///     Returns first element or default value for given type if sequence is empty. Throws if sequence contains more than
        ///     one element.
        /// </summary>
        T SingleOrDefault();

        /// <summary>
        /// Gets the total count of records for this query
        /// </summary>
        /// <returns></returns>
        int Count();

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
