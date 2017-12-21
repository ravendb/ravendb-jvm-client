package net.ravendb.client.serverwide;

import java.util.Map;

public class ConflictSolver {

    private Map<String, ScriptResolver> resolveByCollection;
    private boolean resolveToLatest;


    public Map<String, ScriptResolver> getResolveByCollection() {
        return resolveByCollection;
    }

    public void setResolveByCollection(Map<String, ScriptResolver> resolveByCollection) {
        this.resolveByCollection = resolveByCollection;
    }

    public boolean isResolveToLatest() {
        return resolveToLatest;
    }

    public void setResolveToLatest(boolean resolveToLatest) {
        this.resolveToLatest = resolveToLatest;
    }
}
