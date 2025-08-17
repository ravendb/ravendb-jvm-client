package net.ravendb.client.documents.session;

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
     * Defines queried embedding using a RavenVector wrapper.
     * @param embedding Map with "@vector" key containing a RavenVector
     */
    <T extends Number> void byEmbedding(Map<String, IRavenVector<T>> embedding);
}
