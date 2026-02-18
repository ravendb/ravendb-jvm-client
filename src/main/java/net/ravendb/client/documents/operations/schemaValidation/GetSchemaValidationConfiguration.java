package net.ravendb.client.documents.operations.schemaValidation;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IMaintenanceOperation;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;

import java.io.IOException;

public final class GetSchemaValidationConfiguration
        implements IMaintenanceOperation<SchemaValidationConfiguration> {

    @Override
    public RavenCommand<SchemaValidationConfiguration> getCommand(
            DocumentConventions conventions) {
        return new GetSchemaValidationCommand();
    }

    public static final class GetSchemaValidationCommand
            extends RavenCommand<SchemaValidationConfiguration> {

        public GetSchemaValidationCommand(){ super(SchemaValidationConfiguration.class); };

        @Override
        public boolean isReadRequest() {
            return true;
        }

        @Override
        public HttpUriRequestBase createRequest(ServerNode node) {
            String url = node.getUrl() + "/databases/" + node.getDatabase() + "/schema-validation/config";

            return new HttpGet(url);
        }

        @Override
        public void setResponse(String response, boolean fromCache) throws IOException {
            if (response == null) {
                return;
            }

            result = mapper.readValue(response, resultClass);
        }
    }
}
