package net.ravendb.client.serverwide.operations.configuration;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IVoidMaintenanceOperation;
import net.ravendb.client.http.IRaftCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.http.VoidRavenCommand;
import net.ravendb.client.json.ContentProviderHttpEntity;
import net.ravendb.client.util.RaftIdGenerator;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.core5.http.ContentType;

import java.io.IOException;
import java.util.Map;

public class PutDatabaseSettingsOperation implements IVoidMaintenanceOperation {

    private final String _databaseName;
    private final Map<String, String> _configurationSettings;

    public PutDatabaseSettingsOperation(String databaseName, Map<String, String> configurationSettings) {
        if (databaseName == null) {
            throw new IllegalArgumentException("DatabaseName cannot be null");
        }
        _databaseName = databaseName;

        if (configurationSettings == null) {
            throw new IllegalArgumentException("ConfigurationSettings cannot be null");
        }

        _configurationSettings = configurationSettings;
    }

    @Override
    public VoidRavenCommand getCommand(DocumentConventions conventions) {
        return new PutDatabaseConfigurationSettingsCommand(conventions, _configurationSettings, _databaseName);
    }

    private static class PutDatabaseConfigurationSettingsCommand extends VoidRavenCommand implements IRaftCommand {

        private final ObjectNode _configurationSettings;
        private final String _databaseName;
        private final DocumentConventions _conventions;

        public PutDatabaseConfigurationSettingsCommand(DocumentConventions conventions, Map<String, String> configurationSettings, String databaseName) {
            if (databaseName == null) {
                throw new IllegalArgumentException("DatabaseName cannot be null");
            }
            _conventions = conventions;
            _databaseName = databaseName;

            if (configurationSettings == null) {
                throw new IllegalArgumentException("ConfigurationSettings cannot be null");
            }

            _configurationSettings = mapper.valueToTree(configurationSettings);
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
            String url = node.getUrl() + "/databases/" + _databaseName + "/admin/configuration/settings";

            HttpPut request = new HttpPut(url);
            request.setEntity(new ContentProviderHttpEntity(outputStream -> {
                try (JsonGenerator generator = createSafeJsonGenerator(outputStream)) {
                    generator.getCodec().writeValue(generator, _configurationSettings);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }, ContentType.APPLICATION_JSON, _conventions));

            return request;
        }
    }
}
