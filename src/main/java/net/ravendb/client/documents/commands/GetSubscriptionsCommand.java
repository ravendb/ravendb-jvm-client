package net.ravendb.client.documents.commands;

import net.ravendb.client.documents.subscriptions.GetSubscriptionsResult;
import net.ravendb.client.documents.subscriptions.SubscriptionState;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.primitives.Reference;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;

import java.io.IOException;

public class GetSubscriptionsCommand extends RavenCommand<SubscriptionState[]> {

    private final int _start;
    private final int _pageSize;

    public GetSubscriptionsCommand(int start, int pageSize) {
        super(SubscriptionState[].class);
        _start = start;
        _pageSize = pageSize;
    }

    @Override
    public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
        url.value = node.getUrl() + "/databases/" + node.getDatabase() + "/subscriptions?start=" + _start + "&pageSize=" + _pageSize;

        return new HttpGet();
    }

    @Override
    public void setResponse(String response, boolean fromCache) throws IOException {
        if (response == null) {
            result = null;
            return;
        }

        result = mapper.readValue(response, GetSubscriptionsResult.class).getResults();
    }

    @Override
    public boolean isReadRequest() {
        return true;
    }
}
