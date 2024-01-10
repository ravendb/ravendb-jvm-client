package net.ravendb.client.documents.commands;

import net.ravendb.client.http.IRaftCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.http.VoidRavenCommand;
import net.ravendb.client.util.RaftIdGenerator;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;

public class DeleteSubscriptionCommand extends VoidRavenCommand implements IRaftCommand {

    private final String _name;

    public DeleteSubscriptionCommand(String name) {
        _name = name;
    }

    @Override
    public HttpUriRequestBase createRequest(ServerNode node) {
        String url = node.getUrl() + "/databases/" + node.getDatabase() + "/subscriptions?taskName=" + _name;

        return new HttpDelete(url);
    }

    @Override
    public String getRaftUniqueRequestId() {
        return RaftIdGenerator.newId();
    }
}
