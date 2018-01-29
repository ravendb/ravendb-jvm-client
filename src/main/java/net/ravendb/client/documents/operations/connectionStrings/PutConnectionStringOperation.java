package net.ravendb.client.documents.operations.connectionStrings;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IMaintenanceOperation;
import net.ravendb.client.documents.session.EntityToJson;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.json.ContentProviderHttpEntity;
import net.ravendb.client.primitives.Reference;
import net.ravendb.client.serverwide.operations.connectionStrings.PutConnectionStringResult;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;

import java.io.IOException;

public class PutConnectionStringOperation<T extends ConnectionString> implements IMaintenanceOperation<PutConnectionStringResult> {

    private final T _connectionString;

    public PutConnectionStringOperation(T connectionString) {
        _connectionString = connectionString;
    }

    @Override
    public RavenCommand<PutConnectionStringResult> getCommand(DocumentConventions conventions) {
        return new PutConnectionStringCommand<>(_connectionString);
    }

    public static class PutConnectionStringCommand<T> extends RavenCommand<PutConnectionStringResult> {
        private final T _connectionString;

        public PutConnectionStringCommand(T connectionString) {
            super(PutConnectionStringResult.class);
            _connectionString = connectionString;
        }

        @Override
        public boolean isReadRequest() {
            return false;
        }

        @Override
        public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
            url.value = node.getUrl() + "/databases/" + node.getDatabase() + "/admin/connection-strings";

            HttpPut request = new HttpPut();
            request.setEntity(new ContentProviderHttpEntity(outputStream -> {
                try (JsonGenerator generator = mapper.getFactory().createGenerator(outputStream)) {
                    ObjectNode config = EntityToJson.convertEntityToJson(_connectionString, DocumentConventions.defaultConventions);
                    generator.writeTree(config);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }, ContentType.APPLICATION_JSON));
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
