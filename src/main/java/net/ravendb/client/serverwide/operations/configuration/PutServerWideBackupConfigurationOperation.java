package net.ravendb.client.serverwide.operations.configuration;

import com.fasterxml.jackson.core.JsonGenerator;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.http.IRaftCommand;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.json.ContentProviderHttpEntity;
import net.ravendb.client.primitives.Reference;
import net.ravendb.client.serverwide.operations.IServerOperation;
import net.ravendb.client.serverwide.operations.ongoingTasks.ServerWideTaskResponse;
import net.ravendb.client.util.RaftIdGenerator;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;

import java.io.IOException;

public class PutServerWideBackupConfigurationOperation implements IServerOperation<PutServerWideBackupConfigurationOperation.PutServerWideBackupConfigurationResponse> {
    private final ServerWideBackupConfiguration _configuration;

    public PutServerWideBackupConfigurationOperation(ServerWideBackupConfiguration configuration) {
        if (configuration == null) {
            throw new IllegalArgumentException("Configuration cannot be null");
        }

        _configuration = configuration;
    }

    @Override
    public RavenCommand<PutServerWideBackupConfigurationResponse> getCommand(DocumentConventions conventions) {
        return new PutServerWideBackupConfigurationCommand(conventions, _configuration);
    }

    private static class PutServerWideBackupConfigurationCommand extends RavenCommand<PutServerWideBackupConfigurationResponse> implements IRaftCommand {
        private final ServerWideBackupConfiguration _configuration;
        private final DocumentConventions _conventions;

        public PutServerWideBackupConfigurationCommand(DocumentConventions conventions, ServerWideBackupConfiguration configuration) {
            super(PutServerWideBackupConfigurationResponse.class);

            if (configuration == null) {
                throw new IllegalArgumentException("Configuration cannot be null");
            }

            _conventions = conventions;
            _configuration = configuration;
        }

        @Override
        public boolean isReadRequest() {
            return false;
        }

        @Override
        public String getRaftUniqueRequestId() {
            return RaftIdGenerator.newId();
        }

        @Override
        public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
            url.value = node.getUrl() + "/admin/configuration/server-wide/backup";

            HttpPut request = new HttpPut();
            request.setEntity(new ContentProviderHttpEntity(outputStream -> {
                try (JsonGenerator generator = createSafeJsonGenerator(outputStream)) {
                    generator.getCodec().writeValue(generator, _configuration);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }, ContentType.APPLICATION_JSON, _conventions));

            return request;
        }

        @Override
        public void setResponse(String response, boolean fromCache) throws IOException {
            result = mapper.readValue(response, PutServerWideBackupConfigurationResponse.class);
        }
    }

    public static class PutServerWideBackupConfigurationResponse extends ServerWideTaskResponse {

    }
}