package net.ravendb.client.documents.operations.connectionStrings;

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

public class PutConnectionStringOperation<T extends ConnectionString> implements IMaintenanceOperation<PutConnectionStringResult> {

    private final T _connectionString;

    public PutConnectionStringOperation(T connectionString) {
        _connectionString = connectionString;
    }

    @Override
    public RavenCommand<PutConnectionStringResult> getCommand(DocumentConventions conventions) {
        return new PutConnectionStringCommand<>(conventions, _connectionString);
    }

    public static class PutConnectionStringCommand<T> extends RavenCommand<PutConnectionStringResult> implements IRaftCommand {
        private final DocumentConventions _conventions;
        private final T _connectionString;

        public PutConnectionStringCommand(DocumentConventions conventions, T connectionString) {
            super(PutConnectionStringResult.class);

            if (connectionString == null) {
                throw new IllegalArgumentException("ConnectionString cannot be null");
            }

            _conventions = conventions;
            _connectionString = connectionString;
        }

        @Override
        public boolean isReadRequest() {
            return false;
        }

        @Override
        public HttpUriRequestBase createRequest(ServerNode node) {
            String url = node.getUrl() + "/databases/" + node.getDatabase() + "/admin/connection-strings";

            HttpPut request = new HttpPut(url);
            request.setEntity(new ContentProviderHttpEntity(outputStream -> {
                try (JsonGenerator generator = createSafeJsonGenerator(outputStream)) {
                    ObjectNode config = mapper.valueToTree(_connectionString);
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
