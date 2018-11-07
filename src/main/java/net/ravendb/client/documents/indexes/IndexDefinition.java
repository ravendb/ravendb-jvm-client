package net.ravendb.client.documents.indexes;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A definition of a RavenIndex
 */
public class IndexDefinition {

    public IndexDefinition() {
        configuration = new IndexConfiguration();
    }

    private String name;
    private IndexPriority priority;
    private IndexLockMode lockMode;
    private Map<String, String> additionalSources;
    private Set<String> maps;
    private String reduce;
    private Map<String, IndexFieldOptions> fields;
    private IndexConfiguration configuration;
    private IndexType indexType;
    private String outputReduceToCollection;

    /**
     * This is the means by which the outside world refers to this index definition
     * @return index name
     */
    public String getName() {
        return name;
    }

    /**
     * This is the means by which the outside world refers to this index definition
     * @param name sets the value
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Priority of an index
     * @return index priority
     */
    public IndexPriority getPriority() {
        return priority;
    }

    /**
     * Priority of an index
     * @param priority Sets the value
     */
    public void setPriority(IndexPriority priority) {
        this.priority = priority;
    }

    /**
     * Index lock mode:
     * - Unlock - all index definition changes acceptable
     * - LockedIgnore - all index definition changes will be ignored, only log entry will be created
     * - LockedError - all index definition changes will raise exception
     * @return index lock mode
     */
    public IndexLockMode getLockMode() {
        return lockMode;
    }

    /**
     * Index lock mode:
     * - Unlock - all index definition changes acceptable
     * - LockedIgnore - all index definition changes will be ignored, only log entry will be created
     * - LockedError - all index definition changes will raise exception
     * @param lockMode sets the value
     */
    public void setLockMode(IndexLockMode lockMode) {
        this.lockMode = lockMode;
    }

    /**
     * Additional code files to be compiled with this index.
     * @return additional sources
     */
    public Map<String, String> getAdditionalSources() {
        if (additionalSources == null) {
            additionalSources = new HashMap<>();
        }
        return additionalSources;
    }

    /**
     * Additional code files to be compiled with this index.
     * @param additionalSources Sets the value
     */
    public void setAdditionalSources(Map<String, String> additionalSources) {
        this.additionalSources = additionalSources;
    }

    /**
     * All the map functions for this index
     * @return index maps
     */
    public Set<String> getMaps() {
        if (maps == null) {
            maps = new HashSet<>();
        }
        return maps;
    }

    /**
     * All the map functions for this index
     * @param maps Sets the value
     */
    public void setMaps(Set<String> maps) {
        this.maps = maps;
    }

    /**
     * Index reduce function
     * @return reduce function
     */
    public String getReduce() {
        return reduce;
    }

    /**
     * Index reduce function
     * @param reduce Sets the reduce function
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

        if (maps.isEmpty()) {
            throw new IllegalArgumentException("Index definitions contains no Maps");
        }

        String firstMap = maps.iterator().next();

        firstMap = firstMap.replaceAll("(?:/\\*(?:[^*]|(?:\\*+[^*/]))*\\*+/)|(?://.*)","");
        firstMap = firstMap.trim();

        if (firstMap.startsWith("from") || firstMap.startsWith("docs")) {
            // C# indexes must start with "from" for query synatx or
            // "docs" for method syntax
            if (reduce == null || StringUtils.isBlank(reduce)){
                return IndexType.MAP;
            }
            return IndexType.MAP_REDUCE;
        }

        if (StringUtils.isBlank(getReduce())) {
            return IndexType.JAVA_SCRIPT_MAP;
        }

        return IndexType.JAVA_SCRIPT_MAP_REDUCE;
    }

    /**
     * If not null than each reduce result will be created as a document in the specified collection name.
     * @return true if index outputs should be saved to collection
     */
    public String getOutputReduceToCollection() {
        return outputReduceToCollection;
    }

    /**
     * If not null than each reduce result will be created as a document in the specified collection name.
     * @param outputReduceToCollection Sets the value
     */
    public void setOutputReduceToCollection(String outputReduceToCollection) {
        this.outputReduceToCollection = outputReduceToCollection;
    }
}
