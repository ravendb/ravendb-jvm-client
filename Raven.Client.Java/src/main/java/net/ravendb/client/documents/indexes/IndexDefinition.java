package net.ravendb.client.documents.indexes;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A definition of a RavenIndex
 */
public class IndexDefinition {

    public IndexDefinition() {
        configuration = new IndexConfiguration();
    }

    private Long etag;
    private String name;
    private IndexPriority priority;
    private IndexLockMode lockMode;
    private Map<String, String> additionalSources;
    private Set<String> maps;
    private String reduce;
    private Map<String, IndexFieldOptions> fields;
    private IndexConfiguration configuration;
    private IndexType indexType;
    private boolean testIndex;
    private String outputReduceToCollection;

    /**
     * Index etag (internal).
     */
    public Long getEtag() {
        return etag;
    }

    /**
     * Index etag (internal).
     */
    public void setEtag(Long etag) {
        this.etag = etag;
    }

    /**
     * This is the means by which the outside world refers to this index definition
     */
    public String getName() {
        return name;
    }

    /**
     * This is the means by which the outside world refers to this index definition
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Priority of an index
     */
    public IndexPriority getPriority() {
        return priority;
    }

    /**
     * Priority of an index
     */
    public void setPriority(IndexPriority priority) {
        this.priority = priority;
    }

    /**
     * Index lock mode:
     * - Unlock - all index definition changes acceptable
     * - LockedIgnore - all index definition changes will be ignored, only log entry will be created
     * - LockedError - all index definition changes will raise exception
     */
    public IndexLockMode getLockMode() {
        return lockMode;
    }

    /**
     * Index lock mode:
     * - Unlock - all index definition changes acceptable
     * - LockedIgnore - all index definition changes will be ignored, only log entry will be created
     * - LockedError - all index definition changes will raise exception
     */
    public void setLockMode(IndexLockMode lockMode) {
        this.lockMode = lockMode;
    }

    /**
     * Additional code files to be compiled with this index.
     */
    public Map<String, String> getAdditionalSources() {
        if (additionalSources == null) {
            additionalSources = new HashMap<>();
        }
        return additionalSources;
    }

    /**
     * Additional code files to be compiled with this index.
     */
    public void setAdditionalSources(Map<String, String> additionalSources) {
        this.additionalSources = additionalSources;
    }

    /**
     * All the map functions for this index
     */
    public Set<String> getMaps() {
        if (maps == null) {
            maps = new HashSet<>();
        }
        return maps;
    }

    /**
     * All the map functions for this index
     */
    public void setMaps(Set<String> maps) {
        this.maps = maps;
    }

    /**
     * Index reduce function
     */
    public String getReduce() {
        return reduce;
    }

    /**
     * Index reduce function
     */
    public void setReduce(String reduce) {
        this.reduce = reduce;
    }

    @Override
    public String toString() {
        return name;
    }

    public Map<String, IndexFieldOptions> getFields() {
        if (fields == null) {
            fields = new HashMap<>();
        }
        return fields;
    }

    public void setFields(Map<String, IndexFieldOptions> fields) {
        this.fields = fields;
    }

    public IndexConfiguration getConfiguration() {
        if (configuration == null) {
            configuration = new IndexConfiguration();
        }
        return configuration;
    }

    public void setConfiguration(IndexConfiguration configuration) {
        this.configuration = configuration;
    }

    public IndexType getType() {
        if (indexType == null || indexType == IndexType.NONE) {
            indexType = detectStaticIndexType();
        }

        return indexType;
    }

    public void setType(IndexType indexType) {
        this.indexType = indexType;
    }

    private IndexType detectStaticIndexType() {
        if (reduce == null || StringUtils.isBlank(reduce)){
            return IndexType.MAP;
        }
        return IndexType.MAP_REDUCE;
    }

    /**
     * Whether this is a temporary test only index
     */
    public boolean isTestIndex() {
        return testIndex;
    }

    /**
     * Whether this is a temporary test only index
     */
    public void setTestIndex(boolean testIndex) {
        this.testIndex = testIndex;
    }

    /**
     * If not null than each reduce result will be created as a document in the specified collection name.
     */
    public String getOutputReduceToCollection() {
        return outputReduceToCollection;
    }

    /**
     * If not null than each reduce result will be created as a document in the specified collection name.
     */
    public void setOutputReduceToCollection(String outputReduceToCollection) {
        this.outputReduceToCollection = outputReduceToCollection;
    }
}
