package net.ravendb.client.documents.operations.etl;

import net.ravendb.client.serverwide.IDatabaseTaskStatus;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class EtlProcessState implements IDatabaseTaskStatus {

    private String configurationName;
    private String transformationName;
    private Map<String, Long> lastProcessedEtagPerDbId;
    private String changeVector;
    private String nodeTag;
    private Set<String> skippedTimeSeriesDocs;
    private Date lastBatchTime;

    public EtlProcessState() {
        lastProcessedEtagPerDbId = new HashMap<>();
        changeVector = null;
    }

    public String getConfigurationName() {
        return configurationName;
    }

    public void setConfigurationName(String configurationName) {
        this.configurationName = configurationName;
    }

    public String getTransformationName() {
        return transformationName;
    }

    public void setTransformationName(String transformationName) {
        this.transformationName = transformationName;
    }

    public Map<String, Long> getLastProcessedEtagPerDbId() {
        return lastProcessedEtagPerDbId;
    }

    public void setLastProcessedEtagPerDbId(Map<String, Long> lastProcessedEtagPerDbId) {
        this.lastProcessedEtagPerDbId = lastProcessedEtagPerDbId;
    }

    public String getChangeVector() {
        return changeVector;
    }

    public void setChangeVector(String changeVector) {
        this.changeVector = changeVector;
    }

    @Override
    public String getNodeTag() {
        return nodeTag;
    }

    @Override
    public void setNodeTag(String nodeTag) {
        this.nodeTag = nodeTag;
    }

    /**
     * In a regular case we load time-series only when the time-series change.
     * When there is time-series without document (that can happen if the time-series was replicated but its document didn't yet)
     * we mark the time-series of the document as skipped so when we load the document we will load all its time-series with it
     * @return documents with skipped time series
     */
    public Set<String> getSkippedTimeSeriesDocs() {
        return skippedTimeSeriesDocs;
    }

    public void setSkippedTimeSeriesDocs(Set<String> skippedTimeSeriesDocs) {
        this.skippedTimeSeriesDocs = skippedTimeSeriesDocs;
    }

    public Date getLastBatchTime() {
        return lastBatchTime;
    }

    public void setLastBatchTime(Date lastBatchTime) {
        this.lastBatchTime = lastBatchTime;
    }

    public long getLastProcessedEtag(String dbId, String nodeTag) {
        Long etag = lastProcessedEtagPerDbId.get(dbId);
        return etag != null ? etag : 0;
    }

    public static String generateItemName(String databaseName, String configurationName, String transformationName) {
        return "values/" + databaseName + "/etl/" + configurationName.toLowerCase() + "/" + transformationName.toLowerCase();
    }
}
