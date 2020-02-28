package net.ravendb.client.documents.session;

import net.ravendb.client.documents.CloseableIterator;
import net.ravendb.client.documents.commands.StreamResult;
import net.ravendb.client.documents.indexes.AbstractIndexCreationTask;
import net.ravendb.client.documents.session.operations.lazy.IEagerSessionOperations;
import net.ravendb.client.documents.session.operations.lazy.ILazySessionOperations;
import net.ravendb.client.primitives.Reference;

import java.io.OutputStream;
import java.util.Collection;
import java.util.function.Consumer;

public interface IDocumentQueryBuilder {

    <T, TIndex extends AbstractIndexCreationTask> IDocumentQuery<T> documentQuery(Class<T> clazz, Class<TIndex> indexClazz);

    /**
     * Query the specified index
     * @param <T> Class of query result
     * @param clazz The result of the query
     * @param indexName Name of the index (mutually exclusive with collectionName)
     * @param collectionName Name of the collection (mutually exclusive with indexName)
     * @param isMapReduce Whether we are querying a map/reduce index (modify how we treat identifier properties)
     * @return Document query
     */
    <T> IDocumentQuery<T> documentQuery(Class<T> clazz, String indexName, String collectionName, boolean isMapReduce);

    /**
     * Query the specified index
     * @param <T> Class of query result
     * @param clazz The result of the query
     * @return Document query
     */
    <T> IDocumentQuery<T> documentQuery(Class<T> clazz);

}
