package net.ravendb.client.documents.session;

import net.ravendb.client.json.MetadataAsDictionary;

import java.util.List;
import java.util.Map;

/**
 * Revisions advanced synchronous session operations
 */
public interface IRevisionsSessionOperations {

    /**
     * Returns all previous document revisions for specified document (with paging) ordered by most recent revisions first.
     */
    <T> List<T> getFor(Class<T> clazz, String id);

    /**
     * Returns all previous document revisions for specified document (with paging) ordered by most recent revisions first.
     */
    <T> List<T> getFor(Class<T> clazz, String id, int start);

    /**
     * Returns all previous document revisions for specified document (with paging) ordered by most recent revisions first.
     */
    <T> List<T> getFor(Class<T> clazz, String id, int start, int pageSize);

    /**
     * Returns all previous document revisions for specified document (with paging) ordered by most recent revisions first.
     */
    List<MetadataAsDictionary> getMetadataFor(String id);

    /**
     * Returns all previous document revisions for specified document (with paging) ordered by most recent revisions first.
     */
    List<MetadataAsDictionary> getMetadataFor(String id, int start);

    /**
     * Returns all previous document revisions for specified document (with paging) ordered by most recent revisions first.
     */
    List<MetadataAsDictionary> getMetadataFor(String id, int start, int pageSize);

    /**
     * Returns a document revision by change vector.
     */
    <T> T get(Class<T> clazz, String changeVector);

    /**
     * Returns document revisions by change vectors.
     */
    <T> Map<String, T> get(Class<T> clazz, String[] changeVectors);
}
