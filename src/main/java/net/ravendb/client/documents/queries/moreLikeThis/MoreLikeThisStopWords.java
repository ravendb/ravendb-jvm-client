package net.ravendb.client.documents.queries.moreLikeThis;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class MoreLikeThisStopWords {
    @JsonProperty("Id")
    private String id;

    @JsonProperty("StopWords")
    private List<String> stopWords;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<String> getStopWords() {
        return stopWords;
    }

    public void setStopWords(List<String> stopWords) {
        this.stopWords = stopWords;
    }
}
