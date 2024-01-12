package net.ravendb.client.documents.operations.replication;

import com.fasterxml.jackson.core.JsonGenerator;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IMaintenanceOperation;
import net.ravendb.client.http.IRaftCommand;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.json.ContentProviderHttpEntity;
import net.ravendb.client.serverwide.operations.ModifyOngoingTaskResult;
import net.ravendb.client.util.RaftIdGenerator;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.core5.http.ContentType;

import java.io.IOException;

public class PutPullReplicationAsHubOperation implements IMaintenanceOperation<ModifyOngoingTaskResult> {
    private final PullReplicationDefinition _pullReplicationDefinition;

    public PutPullReplicationAsHubOperation(String name) {
        if (StringUtils.isEmpty(name)) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }

        _pullReplicationDefinition = new PullReplicationDefinition(name);
    }

    public PutPullReplicationAsHubOperation(PullReplicationDefinition pullReplicationDefinition) {
        if (StringUtils.isEmpty(pullReplicationDefinition.getName())) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }

        _pullReplicationDefinition = pullReplicationDefinition;
    }

    @Override
    public RavenCommand<ModifyOngoingTaskResult> getCommand(DocumentConventions conventions) {
        return new UpdatePullReplicationDefinitionCommand(conventions, _pullReplicationDefinition);
    }

    private static class UpdatePullReplicationDefinitionCommand extends RavenCommand<ModifyOngoingTaskResult> implements IRaftCommand {
        private final PullReplicationDefinition _pullReplicationDefinition;
        private final DocumentConventions _conventions;

        public UpdatePullReplicationDefinitionCommand(DocumentConventions conventions, PullReplicationDefinition pullReplicationDefinition) {
            super(ModifyOngoingTaskResult.class);
            _pullReplicationDefinition = pullReplicationDefinition;
            _conventions = conventions;
        }

        @Override
        public HttpUriRequestBase createRequest(ServerNode node) {
            String url = node.getUrl() + "/databases/" + node.getDatabase() + "/admin/tasks/pull-replication/hub";

            HttpPut request = new HttpPut(url);
            request.setEntity(new ContentProviderHttpEntity(outputStream -> {
                try (JsonGenerator generator = createSafeJsonGenerator(outputStream)) {
                    generator.getCodec().writeValue(generator, _pullReplicationDefinition);
                }
            }, ContentType.APPLICATION_JSON, _conventions));

            return request;
        }

        @Override
        public void setResponse(String response, boolean fromCache) throws IOException {
            if (response == null) {
                throwInvalidResponse();
            }

            result = mapper.readValue(response, ModifyOngoingTaskResult.class);
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