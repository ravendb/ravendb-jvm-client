package net.ravendb.client.documents.commands;

import net.ravendb.client.http.ServerNode;
import net.ravendb.client.http.VoidRavenCommand;
import net.ravendb.client.primitives.Reference;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;

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
    public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
        url.value = node.getUrl() + "/admin/operations/kill?id=" + _id;

        return new HttpPost();
    }
}
