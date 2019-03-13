package net.ravendb.client.serverwide.operations.certificates;

import com.fasterxml.jackson.core.JsonGenerator;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.http.VoidRavenCommand;
import net.ravendb.client.json.ContentProviderHttpEntity;
import net.ravendb.client.primitives.Reference;
import net.ravendb.client.primitives.SharpEnum;
import net.ravendb.client.serverwide.operations.IVoidServerOperation;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;

import java.io.IOException;
import java.util.Map;

public class PutClientCertificateOperation implements IVoidServerOperation {

    private final String _certificate;
    private final Map<String, DatabaseAccess> _permissions;
    private final String _name;
    private final SecurityClearance _clearance;

    public PutClientCertificateOperation(String name, String certificate, Map<String, DatabaseAccess> permissions, SecurityClearance clearance) {
        if (certificate == null) {
            throw new IllegalArgumentException("Certificate cannot be null");
        }

        if (permissions == null) {
            throw new IllegalArgumentException("Permissions cannot be null");
        }

        _certificate = certificate;
        _permissions = permissions;
        _name = name;
        _clearance = clearance;
    }

    @Override
    public VoidRavenCommand getCommand(DocumentConventions conventions) {
        return new PutClientCertificateCommand(_name, _certificate, _permissions, _clearance);
    }

    private static class PutClientCertificateCommand extends VoidRavenCommand {
        private final String _certificate;
        private final Map<String, DatabaseAccess> _permissions;
        private final String _name;
        private final SecurityClearance _clearance;

        public PutClientCertificateCommand(String name, String certificate, Map<String, DatabaseAccess> permissions, SecurityClearance clearance) {
            if (certificate == null) {
                throw new IllegalArgumentException("Certificate cannot be null");
            }
            if (permissions == null) {
                throw new IllegalArgumentException("Permissions cannot be null");
            }

            _certificate = certificate;
            _permissions = permissions;
            _name = name;
            _clearance = clearance;
        }

        @Override
        public boolean isReadRequest() {
            return false;
        }

        @Override
        public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
            url.value = node.getUrl() + "/admin/certificates";

            HttpPut request = new HttpPut();

            request.setEntity(new ContentProviderHttpEntity(outputStream -> {
                try (JsonGenerator generator = mapper.getFactory().createGenerator(outputStream)) {

                    generator.writeStartObject();
                    generator.writeStringField("Name", _name);
                    generator.writeStringField("Certificate", _certificate);
                    generator.writeStringField("SecurityClearance", SharpEnum.value(_clearance));

                    generator.writeFieldName("Permissions");
                    generator.writeStartObject();

                    for (Map.Entry<String, DatabaseAccess> kvp : _permissions.entrySet()) {
                        generator.writeStringField(kvp.getKey(), SharpEnum.value(kvp.getValue()));
                    }
                    generator.writeEndObject();
                    generator.writeEndObject();

                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }, ContentType.APPLICATION_JSON));
            return request;

        }
    }

}
