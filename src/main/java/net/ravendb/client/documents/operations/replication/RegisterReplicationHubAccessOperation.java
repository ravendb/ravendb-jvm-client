package net.ravendb.client.documents.operations.replication;

import com.fasterxml.jackson.core.JsonGenerator;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IVoidMaintenanceOperation;
import net.ravendb.client.exceptions.ReplicationHubNotFoundException;
import net.ravendb.client.http.IRaftCommand;
import net.ravendb.client.http.RavenCommandResponseType;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.http.VoidRavenCommand;
import net.ravendb.client.json.ContentProviderHttpEntity;
import net.ravendb.client.primitives.Reference;
import net.ravendb.client.util.RaftIdGenerator;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;

import java.io.IOException;
import java.io.InputStream;

public class RegisterReplicationHubAccessOperation implements IVoidMaintenanceOperation {
    private final String _hubName;
    private final ReplicationHubAccess _access;

    public RegisterReplicationHubAccessOperation(String hubName, ReplicationHubAccess access) {
        if (StringUtils.isBlank(hubName)) {
            throw new IllegalArgumentException("HubName cannot be null or whitespace.");
        }

        if (access == null) {
            throw new IllegalArgumentException("Access cannot be null");
        }

        _hubName = hubName;
        _access = access;
    }

    @Override
    public VoidRavenCommand getCommand(DocumentConventions conventions) {
        return new RegisterReplicationHubAccessCommand(_hubName, _access);
    }

    private static class RegisterReplicationHubAccessCommand extends VoidRavenCommand implements IRaftCommand {
        private final String _hubName;
        private final ReplicationHubAccess _access;

        public RegisterReplicationHubAccessCommand(String hubName, ReplicationHubAccess access) {
            if (StringUtils.isBlank(hubName)) {
                throw new IllegalArgumentException("HubName cannot be null or whitespace.");
            }

            if (access == null) {
                throw new IllegalArgumentException("Access cannot be null");
            }

            _hubName = hubName;
            _access = access;

            responseType = RavenCommandResponseType.RAW;
        }

        @Override
        public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
            url.value = node.getUrl() + "/databases/" + node.getDatabase() + "/admin/tasks/pull-replication/hub/access?name=" + urlEncode(_hubName);

            HttpPut request = new HttpPut();
            request.setEntity(new ContentProviderHttpEntity(outputStream -> {
                try (JsonGenerator generator = createSafeJsonGenerator(outputStream)) {
                    generator.getCodec().writeValue(generator, _access);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }, ContentType.APPLICATION_JSON));

            return request;
        }

        @Override
        public void setResponseRaw(CloseableHttpResponse response, InputStream stream) {
            try (CloseableHttpResponse httpResponse = response) {
                if (response.getStatusLine().getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                    throw new ReplicationHubNotFoundException("The replication hub " + _hubName + " was not found on the database. Did you forget to define it first?");
                }
            } catch (IOException e) {
                throwInvalidResponse(e);
            }
        }

        @Override
        public String getRaftUniqueRequestId() {
            return RaftIdGenerator.newId();
        }
    }
}
