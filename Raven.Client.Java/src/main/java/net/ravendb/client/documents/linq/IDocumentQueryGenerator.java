package net.ravendb.client.documents.linq;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.session.IDocumentQuery;

/**
 * Generate a new document query
 */
public interface IDocumentQueryGenerator {
    /**
     * Gets the conventions associated with this query
     */
    DocumentConventions getConventions();


    /**
     * Create a new query for T
     */
    <T> IDocumentQuery<T> documentQuery(Class<T> clazz, String indexName, String collectionName, boolean isMapReduce);

}
