package net.ravendb.client.documents.session;

import net.ravendb.client.documents.Lazy;
import net.ravendb.client.json.MetadataAsDictionary;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Revisions advanced synchronous Lazy session operations
 */
public interface ILazyRevisionsOperations {

    /**
     * Returns all previous document revisions for specified document ordered by most recent revision first.
     * @param clazz entity class
     * @param id Identifier of a entity
     * @param <T> entity class
     * @return Lazy revisions list
     */
    <T> Lazy<List<T>> getFor(Class<T> clazz, String id);

    /**
     * Returns all previous document revisions for specified document (with paging) ordered by most recent revision first.
     * @param clazz entity class
     * @param id Identifier of a entity
     * @param start start
     * @param <T> entity class
     * @return Lazy revisions list
     */
    <T> Lazy<List<T>> getFor(Class<T> clazz, String id, int start);

    /**
     * Returns all previous document revisions for specified document (with paging) ordered by most recent revision first.
     * @param clazz entity class
     * @param id Identifier of a entity
     * @param start start
     * @param pageSize page size
     * @param <T> entity class
     * @return Lazy revisions list
     */
    <T> Lazy<List<T>> getFor(Class<T> clazz, String id, int start, int pageSize);

    /**
     * Returns all previous document revisions metadata for specified document (with paging).
     * @param id Identifier of a entity
     * @return list of revisions metadata
     */
    Lazy<List<MetadataAsDictionary>> getMetadataFor(String id);

    /**
     * Returns all previous document revisions metadata for specified document (with paging).
     * @param id Identifier of a entity
     * @param start start
     * @return list of revisions metadata
     */
    Lazy<List<MetadataAsDictionary>> getMetadataFor(String id, int start);

    /**
     * Returns all previous document revisions metadata for specified document (with paging).
     * @param id Identifier of a entity
     * @param start start
     * @param pageSize page size
     * @return list of revisions metadata
     */
    Lazy<List<MetadataAsDictionary>> getMetadataFor(String id, int start, int pageSize);

    /**
     * Returns a document revision by change vector.
     * @param clazz entity class
     * @param changeVector Change vector
     * @param <T> entity class
     * @return list of revisions metadata
     */
    <T> Lazy<T> get(Class<T> clazz, String changeVector);

    /**
     * Returns document revisions by change vectors.
     * @param clazz entity class
     * @param changeVectors Change vectors to load
     * @param <T> entity class
     * @return Lazy map of revisions
     */
    <T> Lazy<Map<String, T>> get(Class<T> clazz, String[] changeVectors);

    /**
     * Returns the first revision for this document that happens before or at the specified date.
     * @param clazz entity class
     * @param <T> entity class
     * @param id Identifier of a entity that will be loaded.
     * @param date Date to load
     * @return Lazy revision
     */
    <T> Lazy<T> get(Class<T> clazz, String id, Date date);
}
