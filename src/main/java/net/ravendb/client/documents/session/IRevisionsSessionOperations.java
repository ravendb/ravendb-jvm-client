package net.ravendb.client.documents.session;

import net.ravendb.client.json.MetadataAsDictionary;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Revisions advanced synchronous session operations
 */
public interface IRevisionsSessionOperations {

    /**
     * Returns all previous document revisions for specified document (with paging) ordered by most recent revisions first.
     *
     * @param clazz entity class
     * @param id Identifier of a entity that will be loaded.
     * @param <T> Entity class
     * @return List of revisions
     */
    <T> List<T> getFor(Class<T> clazz, String id);

    /**
     * Returns all previous document revisions for specified document (with paging) ordered by most recent revisions first.
     *
     * @param clazz entity class
     * @param id Identifier of a entity that will be loaded.
     * @param start Range start
     * @param <T> Entity class
     * @return List of revisions
     */
    <T> List<T> getFor(Class<T> clazz, String id, int start);

    /**
     * Returns all previous document revisions for specified document (with paging) ordered by most recent revisions first.
     *
     * @param clazz entity class
     * @param id Identifier of a entity that will be loaded.
     * @param start Range start
     * @param pageSize maximum number of documents that will be retrieved
     * @param <T> Entity class
     * @return List of revisions
     */
    <T> List<T> getFor(Class<T> clazz, String id, int start, int pageSize);

    /**
     * Returns all previous document revisions for specified document (with paging) ordered by most recent revisions first.
     * @param id Identifier of a entity
     * @return List of revisions metadata
     */
    List<MetadataAsDictionary> getMetadataFor(String id);

    /**
     * Returns all previous document revisions for specified document (with paging) ordered by most recent revisions first.
     * @param id Identifier of a entity
     * @param start Range start
     * @return List of revisions metadata
     */
    List<MetadataAsDictionary> getMetadataFor(String id, int start);

    /**
     * Returns all previous document revisions for specified document (with paging) ordered by most recent revisions first.
     * @param id Identifier of a entity
     * @param start Range start
     * @param pageSize  maximum number of metadata that will be retrieved
     * @return List of revisions metadata
     */
    List<MetadataAsDictionary> getMetadataFor(String id, int start, int pageSize);

    /**
     * Returns a document revision by change vector.
     * @param clazz Entity class
     * @param changeVector Revision's changeVector
     * @param <T> Entity class
     * @return Revision with given change vector
     */
    <T> T get(Class<T> clazz, String changeVector);

    /**
     * Returns document revisions by change vectors.
     * @param clazz Entity class
     * @param changeVectors Revision's change vectors
     * @param <T> Entity class
     * @return Revisions matching given change vectors
     */
    <T> Map<String, T> get(Class<T> clazz, String[] changeVectors);

    /**
     * Returns the first revision for this document that happens before or at the specified date
     * @param clazz Entity class
     * @param id Identifier of a entity
     * @param date Date to use
     * @param <T> Entity class
     * @return Revision changed before specified date
     */
    <T> T get(Class<T> clazz, String id, Date date);
}
