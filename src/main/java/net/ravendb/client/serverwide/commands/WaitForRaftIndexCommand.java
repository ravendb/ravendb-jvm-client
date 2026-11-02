package net.ravendb.client.serverwide.commands;

import net.ravendb.client.http.ServerNode;
import net.ravendb.client.http.VoidRavenCommand;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;

public class WaitForRaftIndexCommand extends VoidRavenCommand {

    private final long _index;

    public WaitForRaftIndexCommand(long index) {
        _index = index;
    }

    public WaitForRaftIndexCommand(long index, String nodeTag) {
        this(index);

        this.selectedNodeTag = nodeTag;
    }

    @Override
    public HttpUriRequestBase createRequest(ServerNode node) {
        String url = node.getUrl() + "/rachis/waitfor?index=" + _index;

        return new HttpGet(url);
    }

    @Override
    public boolean isReadRequest() {
        return true;
    }
}
