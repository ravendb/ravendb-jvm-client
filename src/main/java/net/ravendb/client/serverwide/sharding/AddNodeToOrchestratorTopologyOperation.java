package net.ravendb.client.serverwide.sharding;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.http.IRaftCommand;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.serverwide.OrchestratorTopology;
import net.ravendb.client.serverwide.operations.IServerOperation;
import net.ravendb.client.util.RaftIdGenerator;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;

import java.io.IOException;

public class AddNodeToOrchestratorTopologyOperation implements IServerOperation<AddNodeToOrchestratorTopologyOperation.ModifyOrchestratorTopologyResult> {

    private final String _databaseName;
    private final String _node;

    public AddNodeToOrchestratorTopologyOperation(String databaseName) {
        this(databaseName, null);
    }

    public AddNodeToOrchestratorTopologyOperation(String databaseName, String node) {
        _databaseName = databaseName;
        _node = node;
    }

    @Override
    public RavenCommand<ModifyOrchestratorTopologyResult> getCommand(DocumentConventions conventions) {
        return new AddNodeToOrchestratorTopologyCommand(_databaseName, _node);
    }

    private static class AddNodeToOrchestratorTopologyCommand extends RavenCommand<ModifyOrchestratorTopologyResult> implements IRaftCommand {
        private final String _databaseName;
        private final String _node;

        public AddNodeToOrchestratorTopologyCommand(String databaseName, String node) {
            super(ModifyOrchestratorTopologyResult.class);

            _databaseName = databaseName;
            _node = node;
        }

        @Override
        public HttpUriRequestBase createRequest(ServerNode node) {
            String url = node.getUrl() + "/admin/databases/orchestrator?name=" + urlEncode(_databaseName);

            if (StringUtils.isNotEmpty(_node)) {
                url += "&node=" + urlEncode(_node);
            }

            return new HttpPut(url);
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

    public static class ModifyOrchestratorTopologyResult {
        private String name;
        private OrchestratorTopology orchestratorTopology;
        private long raftCommandIndex;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public OrchestratorTopology getOrchestratorTopology() {
            return orchestratorTopology;
        }

        public void setOrchestratorTopology(OrchestratorTopology orchestratorTopology) {
            this.orchestratorTopology = orchestratorTopology;
        }

        public long getRaftCommandIndex() {
            return raftCommandIndex;
        }

        public void setRaftCommandIndex(long raftCommandIndex) {
            this.raftCommandIndex = raftCommandIndex;
        }
    }
}
