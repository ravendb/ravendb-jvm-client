package net.ravendb.client.serverwide.operations.configuration;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IVoidMaintenanceOperation;
import net.ravendb.client.http.IRaftCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.http.VoidRavenCommand;
import net.ravendb.client.json.ContentProviderHttpEntity;
import net.ravendb.client.primitives.Reference;
import net.ravendb.client.util.RaftIdGenerator;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;

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
        return new PutDatabaseConfigurationSettingsCommand(_configurationSettings, _databaseName);
    }

    private static class PutDatabaseConfigurationSettingsCommand extends VoidRavenCommand implements IRaftCommand {

        private final ObjectNode _configurationSettings;
        private final String _databaseName;

        public PutDatabaseConfigurationSettingsCommand(Map<String, String> configurationSettings, String databaseName) {
            if (databaseName == null) {
                throw new IllegalArgumentException("DatabaseName cannot be null");
            }
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
        public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
            url.value = node.getUrl() + "/databases/" + _databaseName + "/admin/configuration/settings";

            HttpPut request = new HttpPut();
            request.setEntity(new ContentProviderHttpEntity(outputStream -> {
                try (JsonGenerator generator = createSafeJsonGenerator(outputStream)) {
                    generator.getCodec().writeValue(generator, _configurationSettings);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }, ContentType.APPLICATION_JSON));

            return request;
        }
    }
}
