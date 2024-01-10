package net.ravendb.client.serverwide.operations.ongoingTasks;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.ResultsResponse;
import net.ravendb.client.documents.operations.ongoingTasks.OngoingTaskType;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.primitives.SharpEnum;
import net.ravendb.client.serverwide.operations.IServerOperation;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;

import java.io.IOException;

public class GetServerWideExternalReplicationOperation implements IServerOperation<ServerWideExternalReplication> {
    private final String _name;

    public GetServerWideExternalReplicationOperation(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Name cannot be null");
        }

        _name = name;
    }

    @Override
    public RavenCommand<ServerWideExternalReplication> getCommand(DocumentConventions conventions) {
        return new GetServerWideExternalReplicationCommand(_name);
    }

    private static class GetServerWideExternalReplicationCommand extends RavenCommand<ServerWideExternalReplication> {
        private final String _name;

        public GetServerWideExternalReplicationCommand(String name) {
            super(ServerWideExternalReplication.class);

            if (name == null) {
                throw new IllegalArgumentException("Name cannot be null");
            }

            _name = name;
        }

        @Override
        public boolean isReadRequest() {
            return true;
        }

        @Override
        public HttpUriRequestBase createRequest(ServerNode node) {
            String url = node.getUrl() + "/admin/configuration/server-wide/tasks?type="
                    + SharpEnum.value(OngoingTaskType.REPLICATION) + "&name=" + urlEncode(_name);

            return new HttpGet(url);
        }

        @Override
        public void setResponse(String response, boolean fromCache) throws IOException {
            if (response == null) {
                return;
            }

            ServerWideExternalReplication[] results = mapper.readValue(response, ResultsResponse.GetServerWideExternalReplicationsResponse.class).getResults();
            if (response.isEmpty()) {
                return;
            }

            if (results.length > 1) {
                throwInvalidResponse();
            }

            result = results[0];
        }
    }
}
