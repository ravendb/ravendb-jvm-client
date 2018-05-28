package net.ravendb.client.documents.commands;

import net.ravendb.client.documents.subscriptions.SubscriptionState;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.primitives.Reference;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;

import java.io.IOException;

public class GetSubscriptionStateCommand extends RavenCommand<SubscriptionState> {

    private final String _subscriptionName;

    public GetSubscriptionStateCommand(String subscriptionName) {
        super(SubscriptionState.class);
        _subscriptionName = subscriptionName;
    }

    @Override
    public boolean isReadRequest() {
        return true;
    }

    @Override
    public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
        url.value = node.getUrl() + "/databases/" + node.getDatabase() + "/subscriptions/state?name=" + _subscriptionName;

        return new HttpGet();
    }

    @Override
    public void setResponse(String response, boolean fromCache) throws IOException {
        result = mapper.readValue(response, SubscriptionState.class);
    }
}
