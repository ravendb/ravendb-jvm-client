package net.ravendb.client.util;

import net.ravendb.client.documents.queries.vectorSearch.VectorEmbeddingType;
import net.ravendb.client.documents.session.IRavenVector;
import net.ravendb.client.documents.session.tokens.VectorSearchToken;

import java.util.HashMap;
import java.util.Map;

public class VectorSearchUtil {
    public static String vectorSearchConfigurationToMethodName(VectorEmbeddingType source, VectorEmbeddingType dest) {
        if (source == VectorEmbeddingType.SINGLE && dest == VectorEmbeddingType.SINGLE) {
            return "";
        }
        if (source == VectorEmbeddingType.SINGLE && dest == VectorEmbeddingType.INT8) {
            return VectorSearchToken.EMBEDDING_SINGLE_INT8;
        }
        if (source == VectorEmbeddingType.SINGLE && dest == VectorEmbeddingType.BINARY) {
            return VectorSearchToken.EMBEDDING_SINGLE_INT1;
        }
        if (source == VectorEmbeddingType.TEXT && dest == VectorEmbeddingType.SINGLE) {
            return VectorSearchToken.EMBEDDING_TEXT;
        }
        if (source == VectorEmbeddingType.TEXT && dest == VectorEmbeddingType.INT8) {
            return VectorSearchToken.EMBEDDING_TEXT_INT8;
        }
        if (source == VectorEmbeddingType.TEXT && dest == VectorEmbeddingType.BINARY) {
            return VectorSearchToken.EMBEDDING_TEXT_INT1;
        }
        if (source == VectorEmbeddingType.INT8 && dest == VectorEmbeddingType.INT8) {
            return VectorSearchToken.EMBEDDING_INT8;
        }
        if (source == VectorEmbeddingType.BINARY && dest == VectorEmbeddingType.BINARY) {
            return VectorSearchToken.EMBEDDING_INT1;
        }

        throw new IllegalStateException(
                String.format("Invalid embedding configuration. SourceEmbedding: %s, DestinationEmbedding: %s",
                        source, dest));
    }

    public static <T extends Number> Map<String, IRavenVector<T>> ravenVector(IRavenVector<T> vector) {
        Map<String, IRavenVector<T>> wrapper = new HashMap<>();
        wrapper.put("@vector", vector);
        return wrapper;
    }
}
