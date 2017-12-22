package net.ravendb.client.serverwide.operations;

import net.ravendb.client.serverwide.DatabaseTopology;

import java.util.List;

public class DatabasePutResult {

    private long raftCommandIndex;
    private String name;
    private DatabaseTopology topology;
    private List<String> nodesAddedTo;

    public long getRaftCommandIndex() {
        return raftCommandIndex;
    }

    public void setRaftCommandIndex(long raftCommandIndex) {
        this.raftCommandIndex = raftCommandIndex;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public DatabaseTopology getTopology() {
        return topology;
    }

    public void setTopology(DatabaseTopology topology) {
        this.topology = topology;
    }

    public List<String> getNodesAddedTo() {
        return nodesAddedTo;
    }

    public void setNodesAddedTo(List<String> nodesAddedTo) {
        this.nodesAddedTo = nodesAddedTo;
    }
}