package net.ravendb.client.documents.session.operations.lazy;

/**
 * Specify interface for lazy operation for the session
 */
public interface ILazySessionOperations {
    /* TBD:
     /// <summary>
        ///     Begin a load while including the specified path
        /// </summary>
        /// <param name="path">Path in documents in which server should look for a 'referenced' documents.</param>
        ILazyLoaderWithInclude<object> Include(string path);

        /// <summary>
        ///     Begin a load while including the specified path
        /// </summary>
        /// <param name="path">Path in documents in which server should look for a 'referenced' documents.</param>
        ILazyLoaderWithInclude<TResult> Include<TResult>(Expression<Func<TResult, string>> path);

        /// <summary>
        ///     Begin a load while including the specified path
        /// </summary>
        /// <param name="path">Path in documents in which server should look for a 'referenced' documents.</param>
        ILazyLoaderWithInclude<TResult> Include<TResult>(Expression<Func<TResult, IEnumerable<string>>> path);

        /// <summary>
        ///     Loads the specified entities with the specified ids.
        /// </summary>
        /// <param name="ids">Enumerable of Ids that should be loaded</param>
        Lazy<Dictionary<string, TResult>> Load<TResult>(IEnumerable<string> ids);

        /// <summary>
        ///     Loads the specified entities with the specified ids and a function to call when it is evaluated
        /// </summary>
        /// <param name="ids">Enumerable of Ids that should be loaded</param>
        /// <param name="onEval">Action to be executed on evaluation.</param>
        Lazy<Dictionary<string, TResult>> Load<TResult>(IEnumerable<string> ids, Action<Dictionary<string, TResult>> onEval);

        /// <summary>
        ///     Loads the specified entity with the specified id.
        /// </summary>
        /// <param name="id">Identifier of a entity that will be loaded.</param>
        Lazy<TResult> Load<TResult>(string id);

        /// <summary>
        ///     Loads the specified entity with the specified id and a function to call when it is evaluated
        /// </summary>
        /// <param name="id">Identifier of a entity that will be loaded.</param>
        /// <param name="onEval">Action to be executed on evaluation.</param>
        Lazy<TResult> Load<TResult>(string id, Action<TResult> onEval);

        /// <summary>
        ///     Loads multiple entities that contain common prefix.
        /// </summary>
        /// <param name="idPrefix">prefix for which documents should be returned e.g. "products/"</param>
        /// <param name="matches">
        ///     pipe ('|') separated values for which document IDs (after 'idPrefix') should be matched ('?'
        ///     any single character, '*' any characters)
        /// </param>
        /// <param name="start">number of documents that should be skipped. By default: 0.</param>
        /// <param name="pageSize">maximum number of documents that will be retrieved. By default: 25.</param>
        /// <param name="exclude">
        ///     pipe ('|') separated values for which document IDs (after 'idPrefix') should not be matched
        ///     ('?' any single character, '*' any characters)
        /// </param>
        /// <param name="startAfter">
        ///     skip document fetching until given ID is found and return documents after that ID (default:
        ///     null)
        /// </param>
        Lazy<Dictionary<string, TResult>> LoadStartingWith<TResult>(string idPrefix, string matches = null, int start = 0, int pageSize = 25, string exclude = null, string startAfter = null);

        Lazy<List<TResult>> MoreLikeThis<TResult>(MoreLikeThisQuery query);
     */
}
