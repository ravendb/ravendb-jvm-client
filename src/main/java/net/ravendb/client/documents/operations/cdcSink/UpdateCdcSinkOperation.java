package net.ravendb.client.documents.operations.cdcSink;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IMaintenanceOperation;
import net.ravendb.client.http.IRaftCommand;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.json.ContentProviderHttpEntity;
import net.ravendb.client.util.RaftIdGenerator;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.core5.http.ContentType;

import java.io.IOException;

public class UpdateCdcSinkOperation implements IMaintenanceOperation<UpdateCdcSinkOperationResult> {

    private final long _taskId;
    private final CdcSinkConfiguration _configuration;

    public UpdateCdcSinkOperation(long taskId, CdcSinkConfiguration configuration) {
        if (configuration == null) {
            throw new IllegalArgumentException("configuration cannot be null");
        }

        _taskId = taskId;
        _configuration = configuration;
    }

    @Override
    public RavenCommand<UpdateCdcSinkOperationResult> getCommand(DocumentConventions conventions) {
        return new UpdateCdcSinkCommand(conventions, _taskId, _configuration);
    }

    private static class UpdateCdcSinkCommand extends RavenCommand<UpdateCdcSinkOperationResult> implements IRaftCommand {
        private final long _taskId;
        private final CdcSinkConfiguration _configuration;
        private final DocumentConventions _conventions;

        public UpdateCdcSinkCommand(DocumentConventions conventions, long taskId, CdcSinkConfiguration configuration) {
            super(UpdateCdcSinkOperationResult.class);

            if (conventions == null) {
                throw new IllegalArgumentException("conventions cannot be null");
            }

            _conventions = conventions;
            _taskId = taskId;
            _configuration = configuration;
        }

        @Override
        public boolean isReadRequest() {
            return false;
        }

        @Override
        public HttpUriRequestBase createRequest(ServerNode node) {
            String url = node.getUrl() + "/databases/" + node.getDatabase() + "/admin/cdc-sink?id=" + _taskId;

            HttpPut request = new HttpPut(url);

            request.setEntity(new ContentProviderHttpEntity(outputStream -> {
                try (JsonGenerator generator = createSafeJsonGenerator(outputStream)) {
                    ObjectNode config = mapper.valueToTree(_configuration);
                    generator.writeTree(config);
                }
            }, ContentType.APPLICATION_JSON, _conventions));

            return request;
        }

        @Override
        public void setResponse(String response, boolean fromCache) throws IOException {
            if (response == null) {
                throwInvalidResponse();
            }

            result = mapper.readValue(response, resultClass);
        }

        @Override
        public String getRaftUniqueRequestId() {
            return RaftIdGenerator.newId();
        }
    }
}
