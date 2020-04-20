package net.ravendb.client.documents.session;

import net.ravendb.client.http.Topology;
import net.ravendb.client.primitives.EventArgs;

public class TopologyUpdatedEventArgs extends EventArgs {
    private Topology _topology;

    public Topology getTopology() {
        return _topology;
    }

    public TopologyUpdatedEventArgs(Topology topology) {
        _topology = topology;
    }
}
