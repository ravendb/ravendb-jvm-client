package net.ravendb.client.serverwide.sharding;

public class OrchestratorConfiguration {

    private OrchestratorTopology topology;

    public OrchestratorTopology getTopology() {
        return topology;
    }

    public void setTopology(OrchestratorTopology topology) {
        this.topology = topology;
    }
}
