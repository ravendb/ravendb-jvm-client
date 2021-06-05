package net.ravendb.client.documents.indexes;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AbstractJavaScriptIndexCreationTask extends AbstractIndexCreationTaskBase<IndexDefinition> {

    private final IndexDefinition _definition = new IndexDefinition();

    protected AbstractJavaScriptIndexCreationTask() {
    }

    public Set<String> getMaps() {
        return _definition.getMaps();
    }

    public void setMaps(Set<String> maps) {
        _definition.setMaps(maps);
    }

    public Map<String, IndexFieldOptions> getFields() {
        return _definition.getFields();
    }

    public void setFields(Map<String, IndexFieldOptions> fields) {
        _definition.setFields(fields);
    }

    protected String getReduce() {
        return _definition.getReduce();
    }

    protected void setReduce(String reduce) {
        _definition.setReduce(reduce);
    }

    @Override
    public boolean isMapReduce() {
        return getReduce() != null;
    }

    /**
     * @return If not null than each reduce result will be created as a document in the specified collection name.
     */
    protected String getOutputReduceToCollection() {
        return _definition.getOutputReduceToCollection();
    }

    /**
     * @param outputReduceToCollection If not null than each reduce result will be created as a document in the specified collection name.
     */
    protected void setOutputReduceToCollection(String outputReduceToCollection) {
        _definition.setOutputReduceToCollection(outputReduceToCollection);
    }

    /**
     * @return Defines a collection name for reference documents created based on provided pattern
     */
    protected String getPatternReferencesCollectionName() {
        return _definition.getPatternReferencesCollectionName();
    }

    /**
     * @param patternReferencesCollectionName Defines a collection name for reference documents created based on provided pattern
     */
    protected void setPatternReferencesCollectionName(String patternReferencesCollectionName) {
        _definition.setPatternReferencesCollectionName(patternReferencesCollectionName);
    }

    /**
     * @return Defines a collection name for reference documents created based on provided pattern
     */
    protected String getPatternForOutputReduceToCollectionReferences() {
        return _definition.getPatternForOutputReduceToCollectionReferences();
    }

    /**
     * @param patternForOutputReduceToCollectionReferences Defines a collection name for reference documents created based on provided pattern
     */
    protected void setPatternForOutputReduceToCollectionReferences(String patternForOutputReduceToCollectionReferences) {
        _definition.setPatternForOutputReduceToCollectionReferences(patternForOutputReduceToCollectionReferences);
    }

    @Override
    public IndexDefinition createIndexDefinition() {
        _definition.setName(getIndexName());
        _definition.setType(isMapReduce() ? IndexType.JAVA_SCRIPT_MAP_REDUCE : IndexType.JAVA_SCRIPT_MAP);
        if (getAdditionalSources() != null) {
            _definition.setAdditionalSources(getAdditionalSources());
        } else {
            _definition.setAdditionalSources(new HashMap<>());
        }
        if (getAdditionalAssemblies() != null) {
            _definition.setAdditionalAssemblies(getAdditionalAssemblies());
        } else {
            _definition.setAdditionalAssemblies(new HashSet<AdditionalAssembly>());
        }
        _definition.setConfiguration(getConfiguration());
        _definition.setLockMode(lockMode);
        _definition.setPriority(priority);
        _definition.setState(state);
        _definition.setDeploymentMode(deploymentMode);
        return _definition;
    }
}
