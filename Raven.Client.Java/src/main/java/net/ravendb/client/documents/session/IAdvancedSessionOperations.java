package net.ravendb.client.documents.session;

import net.ravendb.client.documents.session.operations.lazy.IEagerSessionOperations;
import net.ravendb.client.documents.session.operations.lazy.ILazySessionOperations;

public interface IAdvancedSessionOperations  extends IAdvancedDocumentSessionOperations {

    /**
     *  Access the eager operations
     */
    IEagerSessionOperations eagerly();

    /**
     *  Access the lazy operations
     * @return
     */
    ILazySessionOperations lazily();

    /**
     * Updates entity with latest changes from server
     */
    <T> void refresh(T entity);

    /**
     * Query the specified index using provided raw query
     */
    <T>IRawDocumentQuery<T> rawQuery(Class<T> clazz, String query);

    /**
     * Check if document exists
     */
    boolean exists(String id);
    /*

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
        T[] LoadStartingWith<T>(string idPrefix, string matches = null, int start = 0, int pageSize = 25, string exclude = null, string startAfter = null);

        /// <summary>
        ///     Loads multiple entities that contain common prefix into a given stream.
        /// </summary>
        /// <param name="idPrefix">prefix for which documents should be returned e.g. "products/"</param>
        /// <param name="output">the stream that will contain the load results</param>
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
        void LoadStartingWithIntoStream(string idPrefix, Stream output, string matches = null, int start = 0, int pageSize = 25, string exclude = null, string startAfter = null);

        /// <summary>
        ///     Loads the specified entities with the specified ids directly into a given stream.
        /// </summary>
        /// <param name="ids">Enumerable of the Ids of the documents that should be loaded</param>
        /// <param name="output">the stream that will contain the load results</param>
        void LoadIntoStream(IEnumerable<string> ids, Stream output);

          List<T> MoreLikeThis<T, TIndexCreator>(string documentId) where TIndexCreator : AbstractIndexCreationTask, new();

        List<T> MoreLikeThis<T, TIndexCreator>(MoreLikeThisQuery query) where TIndexCreator : AbstractIndexCreationTask, new();

        List<T> MoreLikeThis<T>(string index, string documentId);

        List<T> MoreLikeThis<T>(MoreLikeThisQuery query);

         void Increment<T, U>(T entity, Expression<Func<T, U>> path, U valToAdd);

        void Increment<T, U>(string id, Expression<Func<T, U>> path, U valToAdd);

        void Patch<T, U>(string id, Expression<Func<T, U>> path, U value);

        void Patch<T, U>(T entity, Expression<Func<T, U>> path, U value);

        void Patch<T, U>(T entity, Expression<Func<T, IEnumerable<U>>> path,
            Expression<Func<JavaScriptArray<U>, object>> arrayAdder);

        void Patch<T, U>(string id, Expression<Func<T, IEnumerable<U>>> path,
            Expression<Func<JavaScriptArray<U>, object>> arrayAdder);

             /// <summary>
        ///     Queries the index specified by <typeparamref name="TIndexCreator" /> using lucene syntax.
        /// </summary>
        /// <typeparam name="T">The result of the query</typeparam>
        /// <typeparam name="TIndexCreator">The type of the index creator.</typeparam>
        IDocumentQuery<T> DocumentQuery<T, TIndexCreator>() where TIndexCreator : AbstractIndexCreationTask, new();

        /// <summary>
        ///     Query the specified index using Lucene syntax
        /// </summary>
        /// <typeparam name="T">The result of the query</typeparam>
        /// <param name="indexName">Name of the index (mutually exclusive with collectionName)</param>
        /// <param name="collectionName">Name of the collection (mutually exclusive with indexName)</param>
        /// <param name="isMapReduce">Whether we are querying a map/reduce index (modify how we treat identifier properties)</param>
        IDocumentQuery<T> DocumentQuery<T>(string indexName = null, string collectionName = null, bool isMapReduce = false);
*/

    //tODO: overrides + doc from above!
    <T> IDocumentQuery<T> documentQuery(Class<T> clazz, String indexName, String collectionName, boolean isMapReduce);

    /* TODO
         /// <summary>
        ///     Stream the results on the query to the client, converting them to
        ///     CLR types along the way.
        ///     <para>Does NOT track the entities in the session, and will not includes changes there when SaveChanges() is called</para>
        /// </summary>
        /// <param name="query">Query to stream results for</param>
        IEnumerator<StreamResult<T>> Stream<T>(IQueryable<T> query);

        /// <summary>
        ///     Stream the results on the query to the client, converting them to
        ///     CLR types along the way.
        ///     <para>Does NOT track the entities in the session, and will not includes changes there when SaveChanges() is called</para>
        /// </summary>
        /// <param name="query">Query to stream results for</param>
        /// <param name="streamQueryStats">Information about the performed query</param>
        IEnumerator<StreamResult<T>> Stream<T>(IQueryable<T> query, out StreamQueryStatistics streamQueryStats);

        /// <summary>
        ///     Stream the results on the query to the client, converting them to
        ///     CLR types along the way.
        ///     <para>Does NOT track the entities in the session, and will not includes changes there when SaveChanges() is called</para>
        /// </summary>
        /// <param name="query">Query to stream results for</param>
        IEnumerator<StreamResult<T>> Stream<T>(IDocumentQuery<T> query);

        /// <summary>
        ///     Stream the results on the query to the client, converting them to
        ///     CLR types along the way.
        ///     <para>Does NOT track the entities in the session, and will not includes changes there when SaveChanges() is called</para>
        /// </summary>
        /// <param name="query">Query to stream results for</param>
        IEnumerator<StreamResult<T>> Stream<T>(IRawDocumentQuery<T> query);


        /// <summary>
        ///     Stream the results on the query to the client, converting them to
        ///     CLR types along the way.
        ///     <para>Does NOT track the entities in the session, and will not includes changes there when SaveChanges() is called</para>
        /// </summary>
        /// <param name="query">Query to stream results for</param>
        /// <param name="streamQueryStats">Information about the performed query</param>
        IEnumerator<StreamResult<T>> Stream<T>(IRawDocumentQuery<T> query, out StreamQueryStatistics streamQueryStats);

        /// <summary>
        ///     Stream the results on the query to the client, converting them to
        ///     CLR types along the way.
        ///     <para>Does NOT track the entities in the session, and will not includes changes there when SaveChanges() is called</para>
        /// </summary>
        /// <param name="query">Query to stream results for</param>
        /// <param name="streamQueryStats">Information about the performed query</param>
        IEnumerator<StreamResult<T>> Stream<T>(IDocumentQuery<T> query, out StreamQueryStatistics streamQueryStats);

        /// <summary>
        ///     Stream the results of documents search to the client, converting them to CLR types along the way.
        ///     <para>Does NOT track the entities in the session, and will not includes changes there when SaveChanges() is called</para>
        /// </summary>
        /// <param name="startsWith">prefix for which documents should be returned e.g. "products/"</param>
        /// <param name="matches">
        ///     pipe ('|') separated values for which document ID (after 'idPrefix') should be matched ('?'
        ///     any single character, '*' any characters)
        /// </param>
        /// <param name="start">number of documents that should be skipped</param>
        /// <param name="pageSize">maximum number of documents that will be retrieved</param>
        /// <param name="startAfter">
        ///     skip document fetching until given ID is found and return documents after that ID (default:
        ///     null)
        /// </param>
        IEnumerator<StreamResult<T>> Stream<T>(string startsWith, string matches = null, int start = 0, int pageSize = int.MaxValue, string startAfter = null);

        /// <summary>
        ///     Returns the results of a query directly into stream
        /// </summary>
        void StreamInto<T>(IDocumentQuery<T> query, Stream output);


        /// <summary>
        ///     Returns the results of a query directly into stream
        /// </summary>
        void StreamInto<T>(IRawDocumentQuery<T> query, Stream output);

         /// <summary>
        /// Returns all previous document revisions for specified document (with paging).
        /// </summary>
        List<T> GetRevisionsFor<T>(string id, int start = 0, int pageSize = 25);
     */
}
