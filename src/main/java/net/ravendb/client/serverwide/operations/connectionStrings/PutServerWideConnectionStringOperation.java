package net.ravendb.client.serverwide.operations.connectionStrings;

import com.fasterxml.jackson.core.JsonGenerator;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.http.IRaftCommand;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.json.ContentProviderHttpEntity;
import net.ravendb.client.serverwide.operations.IServerOperation;
import net.ravendb.client.util.RaftIdGenerator;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.core5.http.ContentType;

import java.io.IOException;

/**
 * Operation to create or update a server-wide connection string. The connection string will be
 * automatically propagated to all databases in the cluster (unless explicitly excluded via
 * {@link ServerWideConnectionString#getExcludedDatabases()}).
 */
public class PutServerWideConnectionStringOperation implements IServerOperation<PutServerWideConnectionStringResult> {

    private final ServerWideConnectionString _connectionString;

    /**
     * @param connectionString the server-wide connection string to create or update
     */
    public PutServerWideConnectionStringOperation(ServerWideConnectionString connectionString) {
        if (connectionString == null) {
            throw new IllegalArgumentException("connectionString cannot be null");
        }

        if (connectionString.getConnectionString() == null) {
            throw new IllegalArgumentException("ConnectionString must not be null.");
        }

        _connectionString = connectionString;
    }

    @Override
    public RavenCommand<PutServerWideConnectionStringResult> getCommand(DocumentConventions conventions) {
        return new PutServerWideConnectionStringCommand(conventions, _connectionString);
    }

    private static class PutServerWideConnectionStringCommand extends RavenCommand<PutServerWideConnectionStringResult> implements IRaftCommand {
        private final DocumentConventions _conventions;
        private final ServerWideConnectionString _connectionString;

        public PutServerWideConnectionStringCommand(DocumentConventions conventions, ServerWideConnectionString connectionString) {
            super(PutServerWideConnectionStringResult.class);

            if (conventions == null) {
                throw new IllegalArgumentException("conventions cannot be null");
            }

            _conventions = conventions;
            _connectionString = connectionString;
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
        public HttpUriRequestBase createRequest(ServerNode node) {
            String url = node.getUrl() + "/admin/configuration/server-wide/connection-strings";

            HttpPut request = new HttpPut(url);

            request.setEntity(new ContentProviderHttpEntity(outputStream -> {
                try (JsonGenerator generator = createSafeJsonGenerator(outputStream)) {
                    generator.writeTree(_connectionString.toJson(mapper));
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
    }
}
