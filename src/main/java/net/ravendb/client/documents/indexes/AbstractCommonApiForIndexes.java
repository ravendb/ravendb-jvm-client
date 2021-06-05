package net.ravendb.client.documents.indexes;

import java.util.Map;
import java.util.Set;

public abstract class AbstractCommonApiForIndexes {

    private Map<String, String> additionalSources;
    private Set<AdditionalAssembly> additionalAssemblies;
    private IndexConfiguration configuration;

    protected AbstractCommonApiForIndexes() {
        configuration = new IndexConfiguration();
    }

    /**
     * Gets a value indicating whether this instance is map reduce index definition
     * @return true if index is map reduce
     */
    public boolean isMapReduce() {
        return false;
    }

    /**
     * Generates index name from type name replacing all _ with /
     * @return index name
     */
    public String getIndexName() {
        return getClass().getSimpleName().replaceAll("_", "/");
    }

    public Map<String, String> getAdditionalSources() {
        return additionalSources;
    }

    public void setAdditionalSources(Map<String, String> additionalSources) {
        this.additionalSources = additionalSources;
    }

    public Set<AdditionalAssembly> getAdditionalAssemblies() {
        return additionalAssemblies;
    }

    public void setAdditionalAssemblies(Set<AdditionalAssembly> additionalAssemblies) {
        this.additionalAssemblies = additionalAssemblies;
    }

    public IndexConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(IndexConfiguration configuration) {
        this.configuration = configuration;
    }
}
