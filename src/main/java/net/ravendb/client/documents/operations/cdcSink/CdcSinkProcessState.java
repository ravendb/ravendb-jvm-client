package net.ravendb.client.documents.operations.cdcSink;

public class CdcSinkProcessState {

    private String nodeTag;
    private String configurationName;

    public String getNodeTag() {
        return nodeTag;
    }

    public void setNodeTag(String nodeTag) {
        this.nodeTag = nodeTag;
    }

    public String getConfigurationName() {
        return configurationName;
    }

    public void setConfigurationName(String configurationName) {
        this.configurationName = configurationName;
    }

    public static String generateItemName(String databaseName, String configurationName) {
        return "values/" + databaseName + "/cdcsink/" + configurationName;
    }
}
