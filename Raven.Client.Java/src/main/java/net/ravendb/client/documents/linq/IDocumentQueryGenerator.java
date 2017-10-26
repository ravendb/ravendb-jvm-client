package net.ravendb.client.documents.linq;

import net.ravendb.client.documents.conventions.DocumentConventions;

/**
 * Generate a new document query
 */
public interface IDocumentQueryGenerator {
    /**
     * Gets the conventions associated with this query
     */
    DocumentConventions getConventions();


    /**
     * Create a new query for
     */
    //TODO: <T> IDocumentQuery<T> documentQuery(Class<T> clazz, String indexName, boolean isMapReduce);

    /**
     * Generates a query inspector
     */
    //TODO: <T> RavenQueryInspector<T> createRavenQueryInspector();
}
