package net.ravendb.client.documents.commands;

import net.ravendb.client.documents.subscriptions.SubscriptionState;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.classic.methods.HttpGet;

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
    public HttpUriRequestBase createRequest(ServerNode node) {
        String url = node.getUrl() + "/databases/"
                + node.getDatabase() + "/subscriptions/state?name="
                + urlEncode(_subscriptionName);

        return new HttpGet(url);
    }

    @Override
    public void setResponse(String response, boolean fromCache) throws IOException {
        result = mapper.readValue(response, SubscriptionState.class);
    }
}
