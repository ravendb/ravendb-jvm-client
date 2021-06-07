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
     */
    <T> Lazy<List<T>> getFor(Class<T> clazz, String id);

    /**
     * Returns all previous document revisions for specified document (with paging) ordered by most recent revision first.
     */
    <T> Lazy<List<T>> getFor(Class<T> clazz, String id, int start);

    /**
     * Returns all previous document revisions for specified document (with paging) ordered by most recent revision first.
     */
    <T> Lazy<List<T>> getFor(Class<T> clazz, String id, int start, int pageSize);

    /**
     * Returns all previous document revisions metadata for specified document (with paging).
     */
    <T> Lazy<List<MetadataAsDictionary>> getMetadataFor(String id);

    /**
     * Returns all previous document revisions metadata for specified document (with paging).
     */
    <T> Lazy<List<MetadataAsDictionary>> getMetadataFor(String id, int start);

    /**
     * Returns all previous document revisions metadata for specified document (with paging).
     */
    <T> Lazy<List<MetadataAsDictionary>> getMetadataFor(String id, int start, int pageSize);

    /**
     * Returns a document revision by change vector.
     */
    <T> Lazy<T> get(Class<T> clazz, String changeVector);

    /**
     * Returns document revisions by change vectors.
     */
    <T> Lazy<Map<String, T>> get(Class<T> clazz, String[] changeVectors);

    /**
     * Returns the first revision for this document that happens before or at the specified date.
     */
    <T> Lazy<T> get(Class<T> clazz, String id, Date date);
}
