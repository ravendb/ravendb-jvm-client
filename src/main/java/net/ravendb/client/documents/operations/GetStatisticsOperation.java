package net.ravendb.client.documents.operations;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.primitives.Reference;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;

import java.io.IOException;

public class GetStatisticsOperation implements IMaintenanceOperation<DatabaseStatistics> {

    private final String _debugTag;

    public GetStatisticsOperation() {
        _debugTag = null;
    }

    public GetStatisticsOperation(String debugTag) {
        this._debugTag = debugTag;
    }

    @Override
    public RavenCommand<DatabaseStatistics> getCommand(DocumentConventions conventions) {
        return new GetStatisticsCommand(_debugTag);
    }

    public static class GetStatisticsCommand extends RavenCommand<DatabaseStatistics> {

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

}
