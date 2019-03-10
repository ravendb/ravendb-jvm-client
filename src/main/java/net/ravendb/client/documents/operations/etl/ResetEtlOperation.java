package net.ravendb.client.documents.operations.etl;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IVoidMaintenanceOperation;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.http.VoidRavenCommand;
import net.ravendb.client.json.ContentProviderHttpEntity;
import net.ravendb.client.primitives.HttpReset;
import net.ravendb.client.primitives.HttpResetWithEntity;
import net.ravendb.client.primitives.Reference;
import net.ravendb.client.util.UrlUtils;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;

import java.io.IOException;

public class ResetEtlOperation implements IVoidMaintenanceOperation {

    private final String _configurationName;
    private final String _transformationName;

    public ResetEtlOperation(String configurationName, String transformationName) {
        _configurationName = configurationName;
        _transformationName = transformationName;
    }

    @Override
    public VoidRavenCommand getCommand(DocumentConventions conventions) {
        return new ResetEtlCommand(_configurationName, _transformationName);
    }

    private static class ResetEtlCommand extends VoidRavenCommand {
        private final String _configurationName;
        private final String _transformationName;

        public ResetEtlCommand(String configurationName, String transformationName) {
            _configurationName = configurationName;
            _transformationName = transformationName;
        }

        @Override
        public boolean isReadRequest() {
            return false;
        }

        @Override
        public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
            url.value = node.getUrl() + "/databases/" + node.getDatabase() + "/admin/etl?configurationName=" + UrlUtils.escapeDataString(_configurationName)
                    + "&transformationName=" + UrlUtils.escapeDataString(_transformationName);

            HttpResetWithEntity request = new HttpResetWithEntity();
            request.setEntity(new ContentProviderHttpEntity(outputStream -> {
                try (JsonGenerator generator = mapper.getFactory().createGenerator(outputStream)) {
                    generator.writeStartObject();
                    generator.writeEndObject();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }, ContentType.APPLICATION_JSON));
            return request;
        }
    }
}
