package net.ravendb.client.documents.commands;

import net.ravendb.client.documents.subscriptions.GetSubscriptionsResult;
import net.ravendb.client.documents.subscriptions.SubscriptionState;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.classic.methods.HttpGet;

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
    public HttpUriRequestBase createRequest(ServerNode node) {
        String url = node.getUrl() + "/databases/" + node.getDatabase() + "/subscriptions?start=" + _start + "&pageSize=" + _pageSize;

        return new HttpGet(url);
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
