package net.ravendb.client.documents.operations.etl;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IMaintenanceOperation;
import net.ravendb.client.documents.operations.connectionStrings.ConnectionString;
import net.ravendb.client.http.IRaftCommand;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.json.ContentProviderHttpEntity;
import net.ravendb.client.util.RaftIdGenerator;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.core5.http.ContentType;

import java.io.IOException;

public class AddEtlOperation<T extends ConnectionString> implements IMaintenanceOperation<AddEtlOperationResult> {

    private final EtlConfiguration<T> _configuration;

    public AddEtlOperation(EtlConfiguration<T> configuration) {
        _configuration = configuration;
    }

    @Override
    public RavenCommand<AddEtlOperationResult> getCommand(DocumentConventions conventions) {
        return new AddEtlCommand<>(conventions, _configuration);
    }

    private static class AddEtlCommand<T extends ConnectionString> extends RavenCommand<AddEtlOperationResult> implements IRaftCommand {
        private final EtlConfiguration<T> _configuration;
        private final DocumentConventions _conventions;

        public AddEtlCommand(DocumentConventions conventions, EtlConfiguration<T> configuration) {
            super(AddEtlOperationResult.class);
            _configuration = configuration;
            _conventions = conventions;
        }

        @Override
        public boolean isReadRequest() {
            return false;
        }

        @Override
        public HttpUriRequestBase createRequest(ServerNode node) {
            String url = node.getUrl() + "/databases/" + node.getDatabase() + "/admin/etl";

            HttpPut request = new HttpPut(url);
            request.setEntity(new ContentProviderHttpEntity(outputStream -> {
                try (JsonGenerator generator = createSafeJsonGenerator(outputStream)) {
                    ObjectNode config = mapper.valueToTree(_configuration);
                    generator.writeTree(config);
                } catch (IOException e) {
                    throw new RuntimeException(e);
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
