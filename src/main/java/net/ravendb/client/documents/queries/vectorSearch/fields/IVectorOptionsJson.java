package net.ravendb.client.documents.queries.vectorSearch.fields;

import net.ravendb.client.documents.queries.vectorSearch.VectorEmbeddingType;

public class IVectorOptionsJson {
    private VectorEmbeddingType sourceEmbeddingType;
    private VectorEmbeddingType destinationEmbeddingType;
    private Integer dimensions;
    private Integer numberOfEdges;
    private Integer numberOfCandidates;
    private Double similarity;
    private Boolean isExact;
    private String sourceFieldName;

    // Getters and setters

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

    public Integer getDimensions() {
        return dimensions;
    }

    public void setDimensions(Integer dimensions) {
        this.dimensions = dimensions;
    }

    public Integer getNumberOfEdges() {
        return numberOfEdges;
    }

    public void setNumberOfEdges(Integer numberOfEdges) {
        this.numberOfEdges = numberOfEdges;
    }

    public Integer getNumberOfCandidates() {
        return numberOfCandidates;
    }

    public void setNumberOfCandidates(Integer numberOfCandidates) {
        this.numberOfCandidates = numberOfCandidates;
    }

    public Double getSimilarity() {
        return similarity;
    }

    public void setSimilarity(Double similarity) {
        this.similarity = similarity;
    }

    public Boolean getIsExact() {
        return isExact;
    }

    public void setIsExact(Boolean isExact) {
        this.isExact = isExact;
    }

    public String getSourceFieldName() {
        return sourceFieldName;
    }

    public void setSourceFieldName(String sourceFieldName) {
        this.sourceFieldName = sourceFieldName;
    }
}
