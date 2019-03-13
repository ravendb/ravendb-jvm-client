package net.ravendb.client.serverwide.operations.certificates;

import com.fasterxml.jackson.core.JsonGenerator;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.RavenCommandResponseType;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.json.ContentProviderHttpEntity;
import net.ravendb.client.primitives.Reference;
import net.ravendb.client.primitives.SharpEnum;
import net.ravendb.client.serverwide.operations.IServerOperation;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class CreateClientCertificateOperation implements IServerOperation<CertificateRawData> {

    private final String _name;
    private final Map<String, DatabaseAccess> _permissions;
    private final SecurityClearance _clearance;
    private final String _password;

    public CreateClientCertificateOperation(String name, Map<String, DatabaseAccess> permissions, SecurityClearance clearance) {
        this(name, permissions, clearance, null);
    }

    public CreateClientCertificateOperation(String name, Map<String, DatabaseAccess> permissions, SecurityClearance clearance, String password) {
        if (name == null) {
            throw new IllegalArgumentException("Name cannot be null");
        }

        if (permissions == null) {
            throw new IllegalArgumentException("Permission cannot be null");
        }

        _name = name;
        _permissions = permissions;
        _clearance = clearance;
        _password = password;
    }

    @Override
    public RavenCommand<CertificateRawData> getCommand(DocumentConventions conventions) {
        return new CreateClientCertificateCommand(_name, _permissions, _clearance, _password);
    }

    private static class CreateClientCertificateCommand extends RavenCommand<CertificateRawData> {
        private final String _name;
        private final Map<String, DatabaseAccess> _permissions;
        private final SecurityClearance _clearance;
        private final String _password;

        public CreateClientCertificateCommand(String name, Map<String, DatabaseAccess> permissions, SecurityClearance clearance, String password) {
            super(CertificateRawData.class);

            if (name == null) {
                throw new IllegalArgumentException("Name cannot be null");
            }
            if (permissions == null) {
                throw new IllegalArgumentException("Permission cannot be null");
            }

            _name = name;
            _permissions = permissions;
            _clearance = clearance;
            _password = password;

            responseType = RavenCommandResponseType.RAW;
        }

        @Override
        public boolean isReadRequest() {
            return true;
        }

        @Override
        public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
            url.value = node.getUrl() + "/admin/certificates";

            HttpPost request = new HttpPost();

            request.setEntity(new ContentProviderHttpEntity(outputStream -> {
                try (JsonGenerator generator = mapper.getFactory().createGenerator(outputStream)) {
                    generator.writeStartObject();

                    generator.writeStringField("Name", _name);
                    generator.writeStringField("SecurityClearance", SharpEnum.value(_clearance));
                    if (_password != null) {
                        generator.writeStringField("Password", _password);
                    }

                    generator.writeFieldName("Permissions");
                    generator.writeStartObject();
                    for (Map.Entry<String, DatabaseAccess> kvp : _permissions.entrySet()) {
                        generator.writeFieldName(kvp.getKey());
                        generator.writeString(SharpEnum.value(kvp.getValue()));
                    }
                    generator.writeEndObject();
                    generator.writeEndObject();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }, ContentType.APPLICATION_JSON));
            return request;
        }

        @Override
        public void setResponseRaw(CloseableHttpResponse response, InputStream stream) {
            if (response == null) {
                throwInvalidResponse();
            }

            result = new CertificateRawData();
            try {
                result.setRawData(IOUtils.toByteArray(stream));
            } catch (IOException e) {
                throwInvalidResponse(e);
            }
        }
    }
}
