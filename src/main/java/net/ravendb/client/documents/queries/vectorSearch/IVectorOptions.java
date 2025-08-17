package net.ravendb.client.documents.queries.vectorSearch;

public class IVectorOptions {
    private Integer numberOfCandidates;
    private Double similarity;
    private Boolean isExact;

    // Getters and setters
    public Integer getNumberOfCandidates() { return numberOfCandidates; }
    public void setNumberOfCandidates(Integer numberOfCandidates) { this.numberOfCandidates = numberOfCandidates; }

    public Double getSimilarity() { return similarity; }
    public void setSimilarity(Double similarity) { this.similarity = similarity; }

    public Boolean getIsExact() { return isExact; }
    public void setIsExact(Boolean isExact) { this.isExact = isExact; }
}
