package net.ravendb.client.documents.commands;

import net.ravendb.client.http.ServerNode;
import net.ravendb.client.http.VoidRavenCommand;
import net.ravendb.client.primitives.Reference;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpRequestBase;

public class DeleteSubscriptionCommand extends VoidRavenCommand {

    private final String _name;

    public DeleteSubscriptionCommand(String name) {
        _name = name;
    }

    @Override
    public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
        url.value = node.getUrl() + "/databases/" + node.getDatabase() + "/subscriptions?taskName=" + _name;

        return new HttpDelete();
    }
}
