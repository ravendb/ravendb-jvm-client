package net.ravendb.client.documents.operations.replication;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IMaintenanceOperation;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.primitives.Reference;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;

import java.io.IOException;

public class GetReplicationHubAccessOperation implements IMaintenanceOperation<DetailedReplicationHubAccess[]> {

    private final String _hubName;
    private final int _start;
    private final int _pageSize;

    public GetReplicationHubAccessOperation(String hubName) {
        this(hubName, 0, 25);
    }

    public GetReplicationHubAccessOperation(String hubName, int start) {
        this(hubName, start, 25);
    }

    public GetReplicationHubAccessOperation(String hubName, int start, int pageSize) {
        _hubName = hubName;
        _start = start;
        _pageSize = pageSize;
    }

    @Override
    public RavenCommand<DetailedReplicationHubAccess[]> getCommand(DocumentConventions conventions) {
        return new GetReplicationHubAccessCommand(_hubName, _start, _pageSize);
    }

    private static class GetReplicationHubAccessCommand extends RavenCommand<DetailedReplicationHubAccess[]> {
        private final String _hubName;
        private final int _start;
        private final int _pageSize;

        public GetReplicationHubAccessCommand(String hubName, int start, int pageSize) {
            super(DetailedReplicationHubAccess[].class);

            if (StringUtils.isBlank(hubName)) {
                throw new IllegalArgumentException("Value cannot be null or whitespace.");
            }

            _hubName = hubName;
            _start = start;
            _pageSize = pageSize;
        }

        @Override
        public boolean isReadRequest() {
            return true;
        }

        @Override
        public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
            url.value = node.getUrl() + "/databases/" + node.getDatabase()
                    + "/admin/tasks/pull-replication/hub/access?name=" + urlEncode(_hubName)
                    + "&start=" + _start
                    + "&pageSize=" + _pageSize;

            return new HttpGet();
        }

        @Override
        public void setResponse(String response, boolean fromCache) throws IOException {
            if (response == null) {
                throwInvalidResponse();
            }

            result = mapper.readValue(response, ReplicationHubAccessResult.class).getResults();
        }
    }
}
