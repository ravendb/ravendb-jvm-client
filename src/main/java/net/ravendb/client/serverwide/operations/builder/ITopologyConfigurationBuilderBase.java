package net.ravendb.client.serverwide.operations.builder;

import net.ravendb.client.serverwide.operations.builder.ITopologyConfigurationBuilder;

public interface ITopologyConfigurationBuilderBase<TSelf> {
    TSelf addNode(String nodeTag);
    TSelf enableDynamicNodesDistribution();
}
