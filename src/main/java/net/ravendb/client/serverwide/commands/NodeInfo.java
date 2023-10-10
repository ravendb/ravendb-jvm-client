package net.ravendb.client.serverwide.commands;

import net.ravendb.client.http.ServerNode;
import net.ravendb.client.serverwide.operations.BuildNumber;

public class NodeInfo {
    private String nodeTag;
    private String topologyId;
    private String certificate;
    private String clusterStatus;
    private int numberOfCores;
    private double installedMemoryInGb;
    private double usableMemoryInGb;
    private BuildNumber buildInfo;
    private ServerNode.Role serverRole;
    private boolean hasFixedPort;
    private int serverSchemaVersion;

    public String getNodeTag() {
        return nodeTag;
    }

    public void setNodeTag(String nodeTag) {
        this.nodeTag = nodeTag;
    }

    public String getTopologyId() {
        return topologyId;
    }

    public void setTopologyId(String topologyId) {
        this.topologyId = topologyId;
    }

    public String getCertificate() {
        return certificate;
    }

    public void setCertificate(String certificate) {
        this.certificate = certificate;
    }

    public String getClusterStatus() {
        return clusterStatus;
    }

    public void setClusterStatus(String clusterStatus) {
        this.clusterStatus = clusterStatus;
    }

    public int getNumberOfCores() {
        return numberOfCores;
    }

    public void setNumberOfCores(int numberOfCores) {
        this.numberOfCores = numberOfCores;
    }

    public double getInstalledMemoryInGb() {
        return installedMemoryInGb;
    }

    public void setInstalledMemoryInGb(double installedMemoryInGb) {
        this.installedMemoryInGb = installedMemoryInGb;
    }

    public double getUsableMemoryInGb() {
        return usableMemoryInGb;
    }

    public void setUsableMemoryInGb(double usableMemoryInGb) {
        this.usableMemoryInGb = usableMemoryInGb;
    }

    public BuildNumber getBuildInfo() {
        return buildInfo;
    }

    public void setBuildInfo(BuildNumber buildInfo) {
        this.buildInfo = buildInfo;
    }

    public ServerNode.Role getServerRole() {
        return serverRole;
    }

    public void setServerRole(ServerNode.Role serverRole) {
        this.serverRole = serverRole;
    }

    public boolean isHasFixedPort() {
        return hasFixedPort;
    }

    public void setHasFixedPort(boolean hasFixedPort) {
        this.hasFixedPort = hasFixedPort;
    }

    public int getServerSchemaVersion() {
        return serverSchemaVersion;
    }

    public void setServerSchemaVersion(int serverSchemaVersion) {
        this.serverSchemaVersion = serverSchemaVersion;
    }
}
