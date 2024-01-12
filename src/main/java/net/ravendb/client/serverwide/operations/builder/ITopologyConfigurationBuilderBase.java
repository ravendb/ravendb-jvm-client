package net.ravendb.client.serverwide.operations.builder;

public interface ITopologyConfigurationBuilderBase<TSelf> {
    TSelf addNode(String nodeTag);
    TSelf enableDynamicNodesDistribution();
}
