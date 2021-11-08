package net.ravendb.client.documents.operations.etl;

import com.fasterxml.jackson.core.JsonGenerator;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IVoidMaintenanceOperation;
import net.ravendb.client.http.IRaftCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.http.VoidRavenCommand;
import net.ravendb.client.json.ContentProviderHttpEntity;
import net.ravendb.client.primitives.HttpResetWithEntity;
import net.ravendb.client.primitives.Reference;
import net.ravendb.client.util.RaftIdGenerator;
import net.ravendb.client.util.UrlUtils;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;

import java.io.IOException;

public class ResetEtlOperation implements IVoidMaintenanceOperation {

    private final String _configurationName;
    private final String _transformationName;

    public ResetEtlOperation(String configurationName, String transformationName) {
        if (configurationName == null) {
            throw new IllegalArgumentException("ConfigurationName cannot be null");
        }

        if (transformationName == null) {
            throw new IllegalArgumentException("TransformationName cannot be null");
        }

        _configurationName = configurationName;
        _transformationName = transformationName;
    }

    @Override
    public VoidRavenCommand getCommand(DocumentConventions conventions) {
        return new ResetEtlCommand(_configurationName, _transformationName);
    }

    private static class ResetEtlCommand extends VoidRavenCommand implements IRaftCommand {
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
            StringBuilder path = new StringBuilder(node.getUrl());

            path
                    .append("/databases/")
                    .append(node.getDatabase())
                    .append("/admin/etl?configurationName=")
                    .append(UrlUtils.escapeDataString(_configurationName))
                    .append("&transformationName=")
                    .append(UrlUtils.escapeDataString(_transformationName));

            url.value = path.toString();

            HttpResetWithEntity request = new HttpResetWithEntity();
            request.setEntity(new ContentProviderHttpEntity(outputStream -> {
                try (JsonGenerator generator = createSafeJsonGenerator(outputStream)) {
                    generator.writeStartObject();
                    generator.writeEndObject();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }, ContentType.APPLICATION_JSON));
            return request;
        }

        @Override
        public String getRaftUniqueRequestId() {
            return RaftIdGenerator.newId();
        }
    }
}
