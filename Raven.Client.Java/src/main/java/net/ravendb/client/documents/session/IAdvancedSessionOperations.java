package net.ravendb.client.documents.session;

import net.ravendb.client.documents.indexes.AbstractIndexCreationTask;
import net.ravendb.client.documents.session.operations.lazy.IEagerSessionOperations;
import net.ravendb.client.documents.session.operations.lazy.ILazySessionOperations;

public interface IAdvancedSessionOperations extends IAdvancedDocumentSessionOperations {

    /**
     *  Access the eager operations
     */
    //TBD IEagerSessionOperations eagerly();

    /**
     *  Access the lazy operations
     * @return
     */
    //TBD ILazySessionOperations lazily();

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
    /* TODO

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
*/

    //TBD void LoadIntoStream(IEnumerable<string> ids, Stream output);
    //TBD List<T> MoreLikeThis<T, TIndexCreator>(string documentId) where TIndexCreator : AbstractIndexCreationTask, new();
    //TBD List<T> MoreLikeThis<T, TIndexCreator>(MoreLikeThisQuery query) where TIndexCreator : AbstractIndexCreationTask, new();
    //TBD List<T> MoreLikeThis<T>(string index, string documentId);
    //TBD List<T> MoreLikeThis<T>(MoreLikeThisQuery query);
    //TBD patch API void Increment<T, U>(T entity, Expression<Func<T, U>> path, U valToAdd);
    //TBD patch API void Increment<T, U>(string id, Expression<Func<T, U>> path, U valToAdd);
    //TBD patch API void Patch<T, U>(string id, Expression<Func<T, U>> path, U value);
    //TBD patch API void Patch<T, U>(T entity, Expression<Func<T, U>> path, U value);
    //TBD patch API void Patch<T, U>(T entity, Expression<Func<T, IEnumerable<U>>> path, Expression<Func<JavaScriptArray<U>, object>> arrayAdder);
    //TBD patch API void Patch<T, U>(string id, Expression<Func<T, IEnumerable<U>>> path, Expression<Func<JavaScriptArray<U>, object>> arrayAdder);

    <T, TIndex extends AbstractIndexCreationTask> IDocumentQuery<T> documentQuery(Class<T> clazz, Class<TIndex> indexClazz);

    /**
     * Query the specified index using Lucene syntax
     * @param clazz The result of the query
     * @param indexName Name of the index (mutually exclusive with collectionName)
     * @param collectionName Name of the collection (mutually exclusive with indexName)
     * @param isMapReduce Whether we are querying a map/reduce index (modify how we treat identifier properties)
     */
    <T> IDocumentQuery<T> documentQuery(Class<T> clazz, String indexName, String collectionName, boolean isMapReduce);


    // TBD stream IEnumerator<StreamResult<T>> Stream<T>(IQueryable<T> query);
    // TBD stream IEnumerator<StreamResult<T>> Stream<T>(IQueryable<T> query, out StreamQueryStatistics streamQueryStats);
    // TBD stream IEnumerator<StreamResult<T>> Stream<T>(IDocumentQuery<T> query);
    // TBD stream IEnumerator<StreamResult<T>> Stream<T>(IRawDocumentQuery<T> query);
    // TBD stream IEnumerator<StreamResult<T>> Stream<T>(IRawDocumentQuery<T> query, out StreamQueryStatistics streamQueryStats);
    // TBD stream IEnumerator<StreamResult<T>> Stream<T>(IDocumentQuery<T> query, out StreamQueryStatistics streamQueryStats);
    // TBD stream IEnumerator<StreamResult<T>> Stream<T>(string startsWith, string matches = null, int start = 0, int pageSize = int.MaxValue, string startAfter = null);
    // TBD stream void StreamInto<T>(IDocumentQuery<T> query, Stream output);
    // TBD stream void StreamInto<T>(IRawDocumentQuery<T> query, Stream output);
    // TBD revisions List<T> GetRevisionsFor<T>(string id, int start = 0, int pageSize = 25);
}
