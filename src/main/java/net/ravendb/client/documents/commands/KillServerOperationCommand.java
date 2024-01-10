package net.ravendb.client.documents.commands;

import net.ravendb.client.http.ServerNode;
import net.ravendb.client.http.VoidRavenCommand;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;

public class KillServerOperationCommand extends VoidRavenCommand {
    private final long _id;

    public KillServerOperationCommand(long id) {
        _id = id;
    }

    public KillServerOperationCommand(long id, String nodeTag) {
        this(id);

        selectedNodeTag = nodeTag;
    }

    @Override
    public HttpUriRequestBase createRequest(ServerNode node) {
        String url = node.getUrl() + "/admin/operations/kill?id=" + _id;

        return new HttpPost(url);
    }
}
