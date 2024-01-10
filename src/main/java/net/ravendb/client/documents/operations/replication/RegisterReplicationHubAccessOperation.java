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
import net.ravendb.client.util.RaftIdGenerator;
import net.ravendb.client.util.UrlUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpStatus;

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
        return new RegisterReplicationHubAccessCommand(conventions, _hubName, _access);
    }

    private static class RegisterReplicationHubAccessCommand extends VoidRavenCommand implements IRaftCommand {
        private final DocumentConventions _conventions;
        private final String _hubName;
        private final ReplicationHubAccess _access;

        public RegisterReplicationHubAccessCommand(DocumentConventions conventions, String hubName, ReplicationHubAccess access) {
            if (StringUtils.isBlank(hubName)) {
                throw new IllegalArgumentException("HubName cannot be null or whitespace.");
            }

            if (access == null) {
                throw new IllegalArgumentException("Access cannot be null");
            }

            _conventions = conventions;
            _hubName = hubName;
            _access = access;

            responseType = RavenCommandResponseType.RAW;
        }

        @Override
        public HttpUriRequestBase createRequest(ServerNode node) {
            String url = node.getUrl() + "/databases/" + node.getDatabase() + "/admin/tasks/pull-replication/hub/access?name=" + UrlUtils.escapeDataString(_hubName);

            HttpPut request = new HttpPut(url);
            request.setEntity(new ContentProviderHttpEntity(outputStream -> {
                try (JsonGenerator generator = createSafeJsonGenerator(outputStream)) {
                    generator.getCodec().writeValue(generator, _access);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }, ContentType.APPLICATION_JSON, _conventions));

            return request;
        }

        @Override
        public void setResponseRaw(ClassicHttpResponse response, InputStream stream) {
            try (ClassicHttpResponse httpResponse = response) {
                if (response.getCode() == HttpStatus.SC_NOT_FOUND) {
                    throw new ReplicationHubNotFoundException("The replication hub " + _hubName
                            + " was not found on the database. Did you forget to define it first?");
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
