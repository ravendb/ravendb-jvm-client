package net.ravendb.client.documents.commands;

import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.classic.methods.HttpGet;

import java.io.IOException;

public class GetConflictsCommand extends RavenCommand<GetConflictsResult> {

    private final String _id;

    public GetConflictsCommand(String id) {
        super(GetConflictsResult.class);
        _id = id;
    }

    @Override
    public boolean isReadRequest() {
        return true;
    }

    @Override
    public HttpUriRequestBase createRequest(ServerNode node) {
        String url = node.getUrl() + "/databases/"
                + node.getDatabase() + "/replication/conflicts?docId="
                + urlEncode(_id);

        return new HttpGet(url);
    }

    @Override
    public void setResponse(String response, boolean fromCache) throws IOException {
        if (response == null) {
            throwInvalidResponse();
        }

        result = mapper.readValue(response, resultClass);
    }

}