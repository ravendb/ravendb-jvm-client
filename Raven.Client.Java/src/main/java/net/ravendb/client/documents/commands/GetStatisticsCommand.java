package net.ravendb.client.documents.commands;

import net.ravendb.client.documents.operations.DatabaseStatistics;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.primitives.Reference;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;

import java.io.IOException;

public class GetStatisticsCommand extends RavenCommand<DatabaseStatistics> {

    private String debugTag;

    public GetStatisticsCommand() {
        super(DatabaseStatistics.class);
    }

    public GetStatisticsCommand(String debugTag) {
        super(DatabaseStatistics.class);
        this.debugTag = debugTag;
    }

    @Override
    public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
        url.value = node.getUrl() + "/databases/" + node.getDatabase() + "/stats";
        if (debugTag != null) {
            url.value += "?" + debugTag;
        }

        return new HttpGet();
    }

    @Override
    public void setResponse(String response, boolean fromCache) throws IOException {
        result = mapper.readValue(response, DatabaseStatistics.class);
    }

    @Override
    public boolean isReadRequest() {
        return true;
    }
}
