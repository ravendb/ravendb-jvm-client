package net.ravendb.client.documents.session;

import net.ravendb.client.documents.RavenVector;

import java.util.Collection;
import java.util.Map;

public interface IVectorEmbeddingFieldValueFactory {

    /**
     * Defines a queried embedding.
     * @param embedding Array containing embedding values
     */
    <T extends Number> void byEmbedding(T[] embedding);

    /**
     * Defines queried embeddings.
     * @param embeddings Array containing embeddings values
     */
    <T extends Number> void byEmbeddings(T[][] embeddings);

    /**
     * Defines queried embedding in base64 format.
     * @param base64Embedding Embedding encoded as base64 string
     */
    void byBase64(String base64Embedding);

    /**
     * Defines queried embeddings in base64 format.
     * @param base64Embeddings Embeddings encoded as base64 strings
     */
    void byBase64(Collection<String> base64Embeddings);

    /**
     * Defines a queried embedding.
     * @param embedding Collection containing embedding values
     * @param <T> embedding element type
     */
    <T extends Number> void byEmbedding(Collection<T> embedding);

    /**
     * Defines queried embeddings (C# ByEmbedding(IEnumerable&lt;IEnumerable&lt;T&gt;&gt;)).
     * @param embeddings Collection of embeddings
     * @param <T> embedding element type
     */
    <T extends Number> void byEmbeddings(Collection<? extends Collection<T>> embeddings);

    /**
     * Defines queried embedding.
     * @param embedding RavenVector containing embedding values.
     * @param <T> embedding element type
     */
    <T extends Number> void byEmbedding(RavenVector<T> embedding);

    /**
     * Defines queried embedding using a RavenVector wrapper.
     * @param embedding Map with "@vector" key containing a RavenVector
     * @param <T> embedding element type
     * @deprecated Map-wrapped vectors are not part of the C# API. Use {@link #byEmbedding(RavenVector)} instead.
     */
    @Deprecated
    <T extends Number> void byEmbedding(Map<String, IRavenVector<T>> embedding);

    /**
     * Query by the embedding(s) indexed from the specified document for the quried field.
     * @param documentId The unique identifier of the document to be processed.
     */
    void forDocument(String documentId);
}
