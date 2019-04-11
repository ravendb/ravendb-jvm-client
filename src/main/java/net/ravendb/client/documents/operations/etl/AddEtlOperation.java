package net.ravendb.client.documents.operations.etl;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IMaintenanceOperation;
import net.ravendb.client.documents.operations.connectionStrings.ConnectionString;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.json.ContentProviderHttpEntity;
import net.ravendb.client.primitives.Reference;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;

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

    private static class AddEtlCommand<T extends ConnectionString> extends RavenCommand<AddEtlOperationResult> {
        private final DocumentConventions _conventions;
        private final EtlConfiguration<T> _configuration;

        public AddEtlCommand(DocumentConventions conventions, EtlConfiguration<T> configuration) {
            super(AddEtlOperationResult.class);
            _conventions = conventions;
            _configuration = configuration;
        }

        @Override
        public boolean isReadRequest() {
            return false;
        }

        @Override
        public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
            url.value = node.getUrl() + "/databases/" + node.getDatabase() + "/admin/etl";

            HttpPut request = new HttpPut();
            request.setEntity(new ContentProviderHttpEntity(outputStream -> {
                try (JsonGenerator generator = mapper.getFactory().createGenerator(outputStream)) {
                    ObjectNode config = mapper.valueToTree(_configuration);
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
