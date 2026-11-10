package net.ravendb.client.documents.indexes;

import net.ravendb.client.documents.queries.vectorSearch.VectorEmbeddingType;

/**
 * Fluent builder of vector field options, used by AbstractGenericIndexCreationTask.vector(...)
 */
public final class VectorOptionsFactory {

    private final FieldVectorOptions vectorOptions;

    VectorOptionsFactory() {
        this.vectorOptions = new FieldVectorOptions();
    }

    FieldVectorOptions getVectorOptions() {
        return vectorOptions;
    }

    /**
     * Defines the embedding type of the source data. Int8 and Binary sources keep their quantization;
     * other source types default the destination to Single.
     * @param sourceType source embedding type
     * @return this factory
     */
    public VectorOptionsFactory sourceEmbedding(VectorEmbeddingType sourceType) {
        vectorOptions.setSourceEmbeddingType(sourceType);
        if (sourceType == VectorEmbeddingType.INT8) {
            vectorOptions.setDestinationEmbeddingType(VectorEmbeddingType.INT8);
        } else if (sourceType == VectorEmbeddingType.BINARY) {
            vectorOptions.setDestinationEmbeddingType(VectorEmbeddingType.BINARY);
        } else {
            vectorOptions.setDestinationEmbeddingType(VectorEmbeddingType.SINGLE);
        }
        return this;
    }

    /**
     * Defines the quantization of the indexed embedding.
     * @param destinationType destination embedding type
     * @return this factory
     */
    public VectorOptionsFactory destinationEmbedding(VectorEmbeddingType destinationType) {
        VectorEmbeddingType source = vectorOptions.getSourceEmbeddingType();
        if ((source == VectorEmbeddingType.INT8 || source == VectorEmbeddingType.BINARY) && source != destinationType) {
            throw new IllegalStateException("Cannot change the quantization of the already quantizied vector.");
        }
        vectorOptions.setDestinationEmbeddingType(destinationType);
        return this;
    }

    /**
     * @param dimensions number of vector dimensions (null: taken from the first indexed vector)
     * @return this factory
     */
    public VectorOptionsFactory dimensions(Integer dimensions) {
        vectorOptions.setDimensions(dimensions);
        return this;
    }

    /**
     * @param numberOfCandidates number of candidates used while building the HNSW graph
     * @return this factory
     */
    public VectorOptionsFactory numberOfCandidates(int numberOfCandidates) {
        vectorOptions.setNumberOfCandidatesForIndexing(numberOfCandidates);
        return this;
    }

    /**
     * @param numberOfEdges maximum number of edges of an HNSW graph node
     * @return this factory
     */
    public VectorOptionsFactory numberOfEdges(int numberOfEdges) {
        vectorOptions.setNumberOfEdges(numberOfEdges);
        return this;
    }
}
