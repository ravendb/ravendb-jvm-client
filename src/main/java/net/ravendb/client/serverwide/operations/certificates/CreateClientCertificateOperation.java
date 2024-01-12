package net.ravendb.client.serverwide.operations.certificates;

import com.fasterxml.jackson.core.JsonGenerator;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.http.IRaftCommand;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.RavenCommandResponseType;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.json.ContentProviderHttpEntity;
import net.ravendb.client.primitives.SharpEnum;
import net.ravendb.client.serverwide.operations.IServerOperation;
import net.ravendb.client.util.RaftIdGenerator;
import org.apache.commons.io.IOUtils;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;

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
        return new CreateClientCertificateCommand(conventions, _name, _permissions, _clearance, _password);
    }

    private static class CreateClientCertificateCommand extends RavenCommand<CertificateRawData> implements IRaftCommand {
        private final DocumentConventions _conventions;
        private final String _name;
        private final Map<String, DatabaseAccess> _permissions;
        private final SecurityClearance _clearance;
        private final String _password;

        public CreateClientCertificateCommand(DocumentConventions conventions, String name, Map<String, DatabaseAccess> permissions, SecurityClearance clearance, String password) {
            super(CertificateRawData.class);

            if (name == null) {
                throw new IllegalArgumentException("Name cannot be null");
            }
            if (permissions == null) {
                throw new IllegalArgumentException("Permission cannot be null");
            }

            _conventions = conventions;
            _name = name;
            _permissions = permissions;
            _clearance = clearance;
            _password = password;

            responseType = RavenCommandResponseType.RAW;
        }

        @Override
        public boolean isReadRequest() {
            return false;
        }

        @Override
        public HttpUriRequestBase createRequest(ServerNode node) {
            String url = node.getUrl() + "/admin/certificates";

            HttpPost request = new HttpPost(url);

            request.setEntity(new ContentProviderHttpEntity(outputStream -> {
                try (JsonGenerator generator = createSafeJsonGenerator(outputStream)) {
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
                }
            }, ContentType.APPLICATION_JSON, _conventions));
            return request;
        }

        @Override
        public void setResponseRaw(ClassicHttpResponse response, InputStream stream) {
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

        @Override
        public String getRaftUniqueRequestId() {
            return RaftIdGenerator.newId();
        }
    }
}
