package net.ravendb.client.documents.session;

import net.ravendb.client.http.Topology;
import net.ravendb.client.primitives.EventArgs;

public class TopologyUpdatedEventArgs extends EventArgs {
    private Topology _topology;
    private String _reason;

    public Topology getTopology() {
        return _topology;
    }

    public TopologyUpdatedEventArgs(Topology topology, String reason) {
        _topology = topology;
        _reason = reason;
    }

    public String getReason() {
        return _reason;
    }

    public void setReason(String reason) {
        this._reason = reason;
    }
}
