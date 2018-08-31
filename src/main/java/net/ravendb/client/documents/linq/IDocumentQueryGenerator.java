package net.ravendb.client.documents.linq;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.session.IDocumentQuery;
import net.ravendb.client.documents.session.InMemoryDocumentSessionOperations;

/**
 * Generate a new document query
 */
public interface IDocumentQueryGenerator {
    /**
     * Gets the conventions associated with this query
     * @return document conventions
     */
    DocumentConventions getConventions();

    InMemoryDocumentSessionOperations getSession();

    /**
     * Create a new query for T
     * @param <T> entity class
     * @param clazz Target class
     * @param indexName Index name to use
     * @param collectionName Collection name to use
     * @param isMapReduce is map reduce query?
     * @return Document query
     */
    <T> IDocumentQuery<T> documentQuery(Class<T> clazz, String indexName, String collectionName, boolean isMapReduce);

}
