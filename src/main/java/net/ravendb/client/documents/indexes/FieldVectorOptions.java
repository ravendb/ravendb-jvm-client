package net.ravendb.client.documents.indexes;

import net.ravendb.client.documents.queries.vectorSearch.VectorEmbeddingType;

import java.util.Objects;

/**
 * Vector field options (C# Raven.Client.Documents.Indexes.Vector.VectorOptions)
 */
public class FieldVectorOptions {
    private Integer dimensions;
    private VectorEmbeddingType sourceEmbeddingType = VectorEmbeddingType.SINGLE;
    private VectorEmbeddingType destinationEmbeddingType = VectorEmbeddingType.SINGLE;
    private Integer numberOfEdges;
    private Integer numberOfCandidatesForIndexing;

    public FieldVectorOptions() {
    }

    public FieldVectorOptions(FieldVectorOptions options) {
        dimensions = options.getDimensions();
        sourceEmbeddingType = options.getSourceEmbeddingType();
        destinationEmbeddingType = options.getDestinationEmbeddingType();
        numberOfCandidatesForIndexing = options.getNumberOfCandidatesForIndexing();
        numberOfEdges = options.getNumberOfEdges();
    }

    void validate() {
        if (destinationEmbeddingType == VectorEmbeddingType.TEXT) {
            throw new IllegalStateException("Destination embedding type cannot be Text.");
        }
        if (dimensions != null && dimensions <= 0) {
            throw new IllegalStateException("Number of vector dimensions has to be positive.");
        }
        if (sourceEmbeddingType == VectorEmbeddingType.TEXT && dimensions != null) {
            throw new IllegalStateException("Dimensions are set internally by the embedder.");
        }
        if (sourceEmbeddingType == VectorEmbeddingType.INT8 && destinationEmbeddingType != VectorEmbeddingType.INT8) {
            throw new IllegalStateException("Quantization cannot be performed on already quantized vector.");
        }
        if (sourceEmbeddingType == VectorEmbeddingType.BINARY && destinationEmbeddingType != VectorEmbeddingType.BINARY) {
            throw new IllegalStateException("Quantization cannot be performed on already quantized vector.");
        }
        if (numberOfEdges != null && numberOfEdges <= 0) {
            throw new IllegalStateException("Number of edges has to be positive.");
        }
        if (numberOfCandidatesForIndexing != null && numberOfCandidatesForIndexing <= 0) {
            throw new IllegalStateException("Number of candidate nodes has to be positive.");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FieldVectorOptions that = (FieldVectorOptions) o;
        return Objects.equals(dimensions, that.dimensions)
                && sourceEmbeddingType == that.sourceEmbeddingType
                && destinationEmbeddingType == that.destinationEmbeddingType
                && Objects.equals(numberOfEdges, that.numberOfEdges)
                && Objects.equals(numberOfCandidatesForIndexing, that.numberOfCandidatesForIndexing);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dimensions, sourceEmbeddingType, destinationEmbeddingType, numberOfEdges, numberOfCandidatesForIndexing);
    }

    public Integer getDimensions() {
        return dimensions;
    }

    public void setDimensions(Integer dimensions) {
        this.dimensions = dimensions;
    }

    public VectorEmbeddingType getSourceEmbeddingType() {
        return sourceEmbeddingType;
    }

    public void setSourceEmbeddingType(VectorEmbeddingType sourceEmbeddingType) {
        this.sourceEmbeddingType = sourceEmbeddingType;
    }

    public VectorEmbeddingType getDestinationEmbeddingType() {
        return destinationEmbeddingType;
    }

    public void setDestinationEmbeddingType(VectorEmbeddingType destinationEmbeddingType) {
        this.destinationEmbeddingType = destinationEmbeddingType;
    }

    public Integer getNumberOfEdges() {
        return numberOfEdges;
    }

    public void setNumberOfEdges(Integer numberOfEdges) {
        this.numberOfEdges = numberOfEdges;
    }

    public Integer getNumberOfCandidatesForIndexing() {
        return numberOfCandidatesForIndexing;
    }

    public void setNumberOfCandidatesForIndexing(Integer numberOfCandidatesForIndexing) {
        this.numberOfCandidatesForIndexing = numberOfCandidatesForIndexing;
    }
}
