package net.ravendb.client.documents.session;

import net.ravendb.client.documents.CloseableIterator;
import net.ravendb.client.documents.commands.StreamResult;
import net.ravendb.client.documents.session.operations.lazy.IEagerSessionOperations;
import net.ravendb.client.documents.session.operations.lazy.ILazySessionOperations;
import net.ravendb.client.primitives.Reference;

import java.io.OutputStream;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

public interface IAdvancedSessionOperations extends IAdvancedDocumentSessionOperations, IDocumentQueryBuilder {

    /**
     * Access the eager operations
     * @return Eager session operations
     */
    IEagerSessionOperations eagerly();

    /**
     * Access the lazy operations
     * @return Lazy session operations
     */
    ILazySessionOperations lazily();

    /**
     * @return Access the attachments operations
     */
    IAttachmentsSessionOperations attachments();


    /**
     * @return Access the revisions operations
     */
    IRevisionsSessionOperations revisions();

    /**
     * @return Access cluster transaction operations
     */
    IClusterTransactionOperations clusterTransaction();

    /**
     * Updates entity with latest changes from server
     * @param <T> entity class
     * @param entity Entity to refresh
     */
    <T> void refresh(T entity);

    /**
     * Updates entity with latest changes from server
     * @param <T> entity class
     * @param entities Collection of instances of an entity that will be refreshed
     */
    <T> void refresh(List<T> entities);

    /**
     * Query the specified index using provided raw query
     * @param <T> result class
     * @param clazz result class
     * @param query Query
     * @return Raw document query
     */
    <T> IRawDocumentQuery<T> rawQuery(Class<T> clazz, String query);

    /**
     * Issue a graph query based on the raw match query provided
     * @param clazz result class
     * @param query Graph Query
     * @param <T> result class
     * @deprecated Graph API will be removed in next major version of the product.
     * @return Graph query
     */
    <T> IGraphDocumentQuery<T> graphQuery(Class<T> clazz, String query);


    /**
     * Check if document exists
     * @param id document id to check
     * @return true if document exists
     */
    boolean exists(String id);

    /**
     * Loads multiple entities that contain common prefix.
     * @param clazz entity class
     * @param idPrefix prefix for which documents should be returned e.g. "products/"
     * @param <T> entity class
     * @return Matched entities
     */
    <T> T[] loadStartingWith(Class<T> clazz, String idPrefix);

    /**
     * Loads multiple entities that contain common prefix.
     * @param clazz entity class
     * @param idPrefix prefix for which documents should be returned e.g. "products/"
     * @param matches  pipe ('|') separated values for which document IDs (after 'idPrefix') should be matched ('?' any single character, '*' any characters)
     * @param <T> entity class
     * @return Matched entities
     */
    <T> T[] loadStartingWith(Class<T> clazz, String idPrefix, String matches);

    /**
     * Loads multiple entities that contain common prefix.
     * @param clazz entity class
     * @param idPrefix prefix for which documents should be returned e.g. "products/"
     * @param matches  pipe ('|') separated values for which document IDs (after 'idPrefix') should be matched ('?' any single character, '*' any characters)
     * @param start number of documents that should be skipped. By default: 0.
     * @param <T> entity class
     * @return Matched entities
     */
    <T> T[] loadStartingWith(Class<T> clazz, String idPrefix, String matches, int start);

    /**
     * Loads multiple entities that contain common prefix.
     * @param clazz entity class
     * @param idPrefix prefix for which documents should be returned e.g. "products/"
     * @param matches  pipe ('|') separated values for which document IDs (after 'idPrefix') should be matched ('?' any single character, '*' any characters)
     * @param start number of documents that should be skipped. By default: 0.
     * @param pageSize maximum number of documents that will be retrieved. By default: 25.
     * @param <T> entity class
     * @return Matched entities
     */
    <T> T[] loadStartingWith(Class<T> clazz, String idPrefix, String matches, int start, int pageSize);

    /**
     * Loads multiple entities that contain common prefix.
     * @param clazz entity class
     * @param idPrefix prefix for which documents should be returned e.g. "products/"
     * @param matches  pipe ('|') separated values for which document IDs (after 'idPrefix') should be matched ('?' any single character, '*' any characters)
     * @param start number of documents that should be skipped. By default: 0.
     * @param pageSize maximum number of documents that will be retrieved. By default: 25.
     * @param exclude pipe ('|') separated values for which document IDs (after 'idPrefix') should not be matched ('?' any single character, '*' any characters)
     * @param <T> entity class
     * @return Matched entities
     */
    <T> T[] loadStartingWith(Class<T> clazz, String idPrefix, String matches, int start, int pageSize, String exclude);

    /**
     * Loads multiple entities that contain common prefix.
     * @param clazz entity class
     * @param idPrefix prefix for which documents should be returned e.g. "products/"
     * @param matches  pipe ('|') separated values for which document IDs (after 'idPrefix') should be matched ('?' any single character, '*' any characters)
     * @param start number of documents that should be skipped. By default: 0.
     * @param pageSize maximum number of documents that will be retrieved. By default: 25.
     * @param exclude pipe ('|') separated values for which document IDs (after 'idPrefix') should not be matched ('?' any single character, '*' any characters)
     * @param startAfter skip document fetching until given ID is found and return documents after that ID (default: null)
     * @param <T> entity class
     * @return Matched entities
     */
    <T> T[] loadStartingWith(Class<T> clazz, String idPrefix, String matches, int start, int pageSize, String exclude, String startAfter);

    /**
     * Loads multiple entities that contain common prefix into a given stream.
     * @param idPrefix prefix for which documents should be returned e.g. "products/"
     * @param output the stream that will contain the load results
     */
    void loadStartingWithIntoStream(String idPrefix, OutputStream output);

    /**
     * Loads multiple entities that contain common prefix into a given stream.
     * @param idPrefix prefix for which documents should be returned e.g. "products/"
     * @param output the stream that will contain the load results
     * @param matches pipe ('|') separated values for which document IDs (after 'idPrefix') should be matched ('?' any single character, '*' any characters)
     */
    void loadStartingWithIntoStream(String idPrefix, OutputStream output, String matches);

    /**
     * Loads multiple entities that contain common prefix into a given stream.
     * @param idPrefix prefix for which documents should be returned e.g. "products/"
     * @param output the stream that will contain the load results
     * @param matches pipe ('|') separated values for which document IDs (after 'idPrefix') should be matched ('?' any single character, '*' any characters)
     * @param start number of documents that should be skipped. By default: 0.
     */
    void loadStartingWithIntoStream(String idPrefix, OutputStream output, String matches, int start);

    /**
     * Loads multiple entities that contain common prefix into a given stream.
     * @param idPrefix prefix for which documents should be returned e.g. "products/"
     * @param output the stream that will contain the load results
     * @param matches pipe ('|') separated values for which document IDs (after 'idPrefix') should be matched ('?' any single character, '*' any characters)
     * @param start number of documents that should be skipped. By default: 0.
     * @param pageSize maximum number of documents that will be retrieved. By default: 25.
     */
    void loadStartingWithIntoStream(String idPrefix, OutputStream output, String matches, int start, int pageSize);

    /**
     * Loads multiple entities that contain common prefix into a given stream.
     * @param idPrefix prefix for which documents should be returned e.g. "products/"
     * @param output the stream that will contain the load results
     * @param matches pipe ('|') separated values for which document IDs (after 'idPrefix') should be matched ('?' any single character, '*' any characters)
     * @param start number of documents that should be skipped. By default: 0.
     * @param pageSize maximum number of documents that will be retrieved. By default: 25.
     * @param exclude pipe ('|') separated values for which document IDs (after 'idPrefix') should not be matched ('?' any single character, '*' any characters)
     */
    void loadStartingWithIntoStream(String idPrefix, OutputStream output, String matches, int start, int pageSize, String exclude);

    /**
     * Loads multiple entities that contain common prefix into a given stream.
     * @param idPrefix prefix for which documents should be returned e.g. "products/"
     * @param output the stream that will contain the load results
     * @param matches pipe ('|') separated values for which document IDs (after 'idPrefix') should be matched ('?' any single character, '*' any characters)
     * @param start number of documents that should be skipped. By default: 0.
     * @param pageSize maximum number of documents that will be retrieved. By default: 25.
     * @param exclude pipe ('|') separated values for which document IDs (after 'idPrefix') should not be matched ('?' any single character, '*' any characters)
     * @param startAfter skip document fetching until given ID is found and return documents after that ID (default: null)
     */
    void loadStartingWithIntoStream(String idPrefix, OutputStream output, String matches, int start, int pageSize, String exclude, String startAfter);

    /**
     * Loads the specified entities with the specified ids directly into a given stream.
     * @param ids Collection of the Ids of the documents that should be loaded
     * @param output the stream that will contain the load results
     */
    void loadIntoStream(Collection<String> ids, OutputStream output);


    <T, U> void increment(String id, String path, U valueToAdd);

    <T, U> void increment(T entity, String path, U valueToAdd);

    <T, U> void patch(String id, String path, U value);

    <T, U> void patch(T entity, String path, U value);

    <T, U> void patchArray(T entity, String pathToArray, Consumer<JavaScriptArray<U>> arrayAdder);

    <T, U> void patchArray(String id, String pathToArray, Consumer<JavaScriptArray<U>> arrayAdder);

    <T, TKey, TValue> void patchObject(T entity, String pathToObject, Consumer<JavaScriptMap<TKey, TValue>> dictionaryAdder);

    <T, TKey, TValue> void patchObject(String id, String pathToObject, Consumer<JavaScriptMap<TKey, TValue>> dictionaryAdder);

    <T, TU> void addOrPatch(String id, T entity, String pathToObject, TU value);

    <T, TU> void addOrPatchArray(String id, T entity, String pathToObject, Consumer<JavaScriptArray<TU>> arrayAdder);

    <T, TU> void addOrIncrement(String id, T entity, String pathToObject, TU valToAdd);

    /**
     * Stream the results on the query to the client, converting them to
     * Java types along the way.
     *
     * Does NOT track the entities in the session, and will not includes changes there when saveChanges() is called
     * @param query Query to stream results for
     * @param <T> Result class
     * @return results iterator
     */
    <T> CloseableIterator<StreamResult<T>> stream(IDocumentQuery<T> query);

    /**
     * Stream the results on the query to the client, converting them to
     * Java types along the way.
     *
     * Does NOT track the entities in the session, and will not includes changes there when saveChanges() is called
     * @param query Query to stream results for
     * @param streamQueryStats Information about the performed query
     * @param <T> Result class
     * @return results iterator
     */
    <T> CloseableIterator<StreamResult<T>> stream(IDocumentQuery<T> query, Reference<StreamQueryStatistics> streamQueryStats);

    /**
     * Stream the results on the query to the client, converting them to
     * Java types along the way.
     *
     * Does NOT track the entities in the session, and will not includes changes there when saveChanges() is called
     * @param query Query to stream results for
     * @param <T> Result class
     * @return results iterator
     */
    <T> CloseableIterator<StreamResult<T>> stream(IRawDocumentQuery<T> query);

    /**
     * Stream the results on the query to the client, converting them to
     * Java types along the way.
     *
     * Does NOT track the entities in the session, and will not includes changes there when saveChanges() is called
     * @param query Query to stream results for
     * @param streamQueryStats Information about the performed query
     * @param <T> Result class
     * @return results iterator
     */
    <T> CloseableIterator<StreamResult<T>> stream(IRawDocumentQuery<T> query, Reference<StreamQueryStatistics> streamQueryStats);

    /**
     * Stream the results of documents search to the client, converting them to CLR types along the way.
     * Does NOT track the entities in the session, and will not includes changes there when saveChanges() is called
     * @param <T> Result class
     * @param clazz Entity class
     * @param startsWith prefix for which documents should be returned e.g. "products/"
     * @return results iterator
     */
    <T> CloseableIterator<StreamResult<T>> stream(Class<T> clazz, String startsWith);

    /**
     * Stream the results of documents search to the client, converting them to CLR types along the way.
     * Does NOT track the entities in the session, and will not includes changes there when saveChanges() is called
     * @param clazz Result class
     * @param startsWith prefix for which documents should be returned e.g. "products/"
     * @param matches pipe ('|') separated values for which document ID (after 'idPrefix') should be matched ('?' any single character, '*' any characters)
     * @param <T> Result class
     * @return results iterator
     */
    <T> CloseableIterator<StreamResult<T>> stream(Class<T> clazz, String startsWith, String matches);

    /**
     * Stream the results of documents search to the client, converting them to CLR types along the way.
     * Does NOT track the entities in the session, and will not includes changes there when saveChanges() is called
     * @param clazz Entity class
     * @param startsWith prefix for which documents should be returned e.g. "products/"
     * @param matches pipe ('|') separated values for which document ID (after 'idPrefix') should be matched ('?' any single character, '*' any characters)
     * @param start number of documents that should be skipped
     * @param <T> Result class
     * @return results iterator
     */
    <T> CloseableIterator<StreamResult<T>> stream(Class<T> clazz, String startsWith, String matches, int start);

    /**
     * Stream the results of documents search to the client, converting them to CLR types along the way.
     * Does NOT track the entities in the session, and will not includes changes there when saveChanges() is called
     * @param clazz Entity class
     * @param startsWith prefix for which documents should be returned e.g. "products/"
     * @param matches pipe ('|') separated values for which document ID (after 'idPrefix') should be matched ('?' any single character, '*' any characters)
     * @param start number of documents that should be skipped
     * @param pageSize maximum number of documents that will be retrieved
     * @param <T> Result class
     * @return results iterator
     */
    <T> CloseableIterator<StreamResult<T>> stream(Class<T> clazz, String startsWith, String matches, int start, int pageSize);

    /**
     * Stream the results of documents search to the client, converting them to CLR types along the way.
     * Does NOT track the entities in the session, and will not includes changes there when saveChanges() is called
     * @param clazz Entity class
     * @param startsWith prefix for which documents should be returned e.g. "products/"
     * @param matches pipe ('|') separated values for which document ID (after 'idPrefix') should be matched ('?' any single character, '*' any characters)
     * @param start number of documents that should be skipped
     * @param pageSize maximum number of documents that will be retrieved
     * @param startAfter skip document fetching until given ID is found and return documents after that ID (default: null)
     * @param <T> Result class
     * @return results iterator
     */
    <T> CloseableIterator<StreamResult<T>> stream(Class<T> clazz, String startsWith, String matches, int start, int pageSize, String startAfter);

    /**
     * Returns the results of a query directly into stream
     * @param query Query to use
     * @param output Target output stream
     * @param <T> Result class
     */
    <T> void streamInto(IDocumentQuery<T> query, OutputStream output);

    /**
     * Returns the results of a query directly into stream
     * @param query  Query to use
     * @param output Target output stream
     * @param <T> Result class
     */
    <T> void streamInto(IRawDocumentQuery<T> query, OutputStream output);

    /**
     * Loads the specified entity with the specified id and changeVector.
     * If the entity is loaded into the session, the tracked entity will be returned otherwise the entity will be loaded only if it is fresher then the provided changeVector.
     * @param clazz Result class
     * @param id Identifier of a entity that will be conditional loaded.
     * @param changeVector Change vector of a entity that will be conditional loaded.
     * @param <T> Result class
     * @return Entity and change vector
     */
    <T> ConditionalLoadResult<T> conditionalLoad(Class<T> clazz, String id, String changeVector);

}
