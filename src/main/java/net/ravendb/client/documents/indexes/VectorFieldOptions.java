package net.ravendb.client.documents.indexes;

import net.ravendb.client.documents.queries.vectorSearch.VectorEmbeddingType;

public class VectorFieldOptions {
    private Integer dimensions;
    private VectorEmbeddingType sourceEmbeddingType;
    private VectorEmbeddingType destinationEmbeddingType;
    private Integer numberOfEdges;
    private Integer numberOfCandidatesForIndexing;

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
