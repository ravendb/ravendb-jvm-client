package net.ravendb.client.documents.commands;

import net.ravendb.client.http.ServerNode;
import net.ravendb.client.http.VoidRavenCommand;
import net.ravendb.client.primitives.Reference;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;

public class KillOperationCommand extends VoidRavenCommand {

    private final long _id;

    public KillOperationCommand(long id) {
        _id = id;
    }

    @Override
    public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
        url.value = node.getUrl() + "/databases/" + node.getDatabase() + "/operations/kill?id=" + _id;

        return new HttpPost();
    }
}
