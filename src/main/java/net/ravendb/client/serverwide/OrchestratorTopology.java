package net.ravendb.client.serverwide;

public class OrchestratorTopology extends DatabaseTopology {

    public void update(DatabaseTopology topology) {
        setMembers(topology.getMembers());
        setPromotables(topology.getPromotables());
        setRehabs(topology.getRehabs());

        setPredefinedMentors(topology.getPredefinedMentors());
        setDemotionReasons(topology.getDemotionReasons());
        setPromotablesStatus(topology.getPromotablesStatus());

        setStamp(topology.getStamp());
        setPriorityOrder(topology.getPriorityOrder());
        setNodesModifiedAt(topology.getNodesModifiedAt());

        setReplicationFactor(topology.getReplicationFactor());
        setDynamicNodesDistribution(topology.isDynamicNodesDistribution());
    }
}
