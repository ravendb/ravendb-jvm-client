package net.ravendb.client.serverwide;

import net.ravendb.client.serverwide.operations.DatabasePromotionStatus;

import java.util.List;
import java.util.Map;

public class DatabaseTopology {

    private List<String> members;
    private List<String> promotables;
    private List<String> rehabs;

    private Map<String, String> predefinedMentors;
    private Map<String, String> demotionReasons;
    private Map<String, DatabasePromotionStatus> promotablesStatus;
    private int replicationFactor;
    private boolean dynamicNodesDistribution;
    private LeaderStamp stamp;
    private String databaseTopologyIdBase64;

    public List<String> getMembers() {
        return members;
    }

    public void setMembers(List<String> members) {
        this.members = members;
    }

    public List<String> getPromotables() {
        return promotables;
    }

    public void setPromotables(List<String> promotables) {
        this.promotables = promotables;
    }

    public List<String> getRehabs() {
        return rehabs;
    }

    public void setRehabs(List<String> rehabs) {
        this.rehabs = rehabs;
    }

    public Map<String, String> getPredefinedMentors() {
        return predefinedMentors;
    }

    public void setPredefinedMentors(Map<String, String> predefinedMentors) {
        this.predefinedMentors = predefinedMentors;
    }

    public Map<String, String> getDemotionReasons() {
        return demotionReasons;
    }

    public void setDemotionReasons(Map<String, String> demotionReasons) {
        this.demotionReasons = demotionReasons;
    }

    public Map<String, DatabasePromotionStatus> getPromotablesStatus() {
        return promotablesStatus;
    }

    public void setPromotablesStatus(Map<String, DatabasePromotionStatus> promotablesStatus) {
        this.promotablesStatus = promotablesStatus;
    }

    public int getReplicationFactor() {
        return replicationFactor;
    }

    public void setReplicationFactor(int replicationFactor) {
        this.replicationFactor = replicationFactor;
    }

    public boolean isDynamicNodesDistribution() {
        return dynamicNodesDistribution;
    }

    public void setDynamicNodesDistribution(boolean dynamicNodesDistribution) {
        this.dynamicNodesDistribution = dynamicNodesDistribution;
    }

    public LeaderStamp getStamp() {
        return stamp;
    }

    public void setStamp(LeaderStamp stamp) {
        this.stamp = stamp;
    }

    public String getDatabaseTopologyIdBase64() {
        return databaseTopologyIdBase64;
    }

    public void setDatabaseTopologyIdBase64(String databaseTopologyIdBase64) {
        this.databaseTopologyIdBase64 = databaseTopologyIdBase64;
    }
}
