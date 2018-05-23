package net.ravendb.client.documents.operations.revisions;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class RevisionsConfiguration {

    @JsonProperty("Default")
    private RevisionsCollectionConfiguration defaultConfig;

    private Map<String, RevisionsCollectionConfiguration> collections;


    public RevisionsCollectionConfiguration getDefaultConfig() {
        return defaultConfig;
    }

    public void setDefaultConfig(RevisionsCollectionConfiguration defaultConfig) {
        this.defaultConfig = defaultConfig;
    }

    public Map<String, RevisionsCollectionConfiguration> getCollections() {
        return collections;
    }

    public void setCollections(Map<String, RevisionsCollectionConfiguration> collections) {
        this.collections = collections;
    }
}
