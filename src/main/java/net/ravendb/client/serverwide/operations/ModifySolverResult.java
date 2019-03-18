package net.ravendb.client.serverwide.operations;

import net.ravendb.client.serverwide.ConflictSolver;

public class ModifySolverResult {
    private String key;
    private Long raftCommandIndex;
    private ConflictSolver solver;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Long getRaftCommandIndex() {
        return raftCommandIndex;
    }

    public void setRaftCommandIndex(Long raftCommandIndex) {
        this.raftCommandIndex = raftCommandIndex;
    }

    public ConflictSolver getSolver() {
        return solver;
    }

    public void setSolver(ConflictSolver solver) {
        this.solver = solver;
    }
}
