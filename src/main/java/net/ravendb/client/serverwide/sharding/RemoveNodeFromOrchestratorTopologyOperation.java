package net.ravendb.client.serverwide.sharding;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.http.IRaftCommand;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.primitives.Reference;
import net.ravendb.client.serverwide.operations.IServerOperation;
import net.ravendb.client.util.RaftIdGenerator;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpRequestBase;

import java.io.IOException;

public class RemoveNodeFromOrchestratorTopologyOperation implements IServerOperation<AddNodeToOrchestratorTopologyOperation.ModifyOrchestratorTopologyResult> {
    private final String _databaseName;
    private final String _node;

    public RemoveNodeFromOrchestratorTopologyOperation(String databaseName, String node) {
        _node = node;
        _databaseName = databaseName;
    }

    @Override
    public RavenCommand<AddNodeToOrchestratorTopologyOperation.ModifyOrchestratorTopologyResult> getCommand(DocumentConventions conventions) {
        return new RemoveNodeFromOrchestratorTopologyCommand(_databaseName, _node);
    }

    private static class RemoveNodeFromOrchestratorTopologyCommand extends RavenCommand<AddNodeToOrchestratorTopologyOperation.ModifyOrchestratorTopologyResult> implements IRaftCommand {
        private final String _databaseName;
        private final String _node;

        public RemoveNodeFromOrchestratorTopologyCommand(String databaseName, String node) {
            super(AddNodeToOrchestratorTopologyOperation.ModifyOrchestratorTopologyResult.class);

            if (StringUtils.isEmpty(databaseName)) {
                throw new IllegalArgumentException("DatabaseName cannot be null or empty");
            }
            if (StringUtils.isEmpty(node)) {
                throw new IllegalArgumentException("Node cannot be null or empty");
            }

            _databaseName = databaseName;
            _node = node;
        }

        @Override
        public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
            url.value = node.getUrl() + "/admin/databases/orchestrator?name=" + urlEncode(_databaseName) + "&node=" + urlEncode(_node);

            return new HttpDelete();
        }

        @Override
        public void setResponse(String response, boolean fromCache) throws IOException {
            if (response == null) {
                throwInvalidResponse();
            }

            result = mapper.readValue(response, resultClass);
        }

        @Override
        public boolean isReadRequest() {
            return false;
        }

        @Override
        public String getRaftUniqueRequestId() {
            return RaftIdGenerator.newId();
        }
    }
}
