package net.ravendb.client.documents.commands;

import net.ravendb.client.http.ServerNode;
import net.ravendb.client.http.VoidRavenCommand;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;

public class KillOperationCommand extends VoidRavenCommand {

    private final long _id;

    public KillOperationCommand(long id) {
        _id = id;
    }

    public KillOperationCommand(long id, String nodeTag) {
        this(id);

        this.selectedNodeTag = nodeTag;
    }

    @Override
    public HttpUriRequestBase createRequest(ServerNode node) {
        String url = node.getUrl() + "/databases/" + node.getDatabase() + "/operations/kill?id=" + _id;

        return new HttpPost(url);
    }
}
