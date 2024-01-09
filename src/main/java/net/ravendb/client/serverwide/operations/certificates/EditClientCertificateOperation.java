package net.ravendb.client.serverwide.operations.certificates;

import com.fasterxml.jackson.core.JsonGenerator;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.http.IRaftCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.http.VoidRavenCommand;
import net.ravendb.client.json.ContentProviderHttpEntity;
import net.ravendb.client.primitives.Reference;
import net.ravendb.client.serverwide.operations.IVoidServerOperation;
import net.ravendb.client.util.RaftIdGenerator;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;

import java.io.IOException;
import java.util.Map;

public class EditClientCertificateOperation implements IVoidServerOperation {

    private final String _thumbprint;
    private final Map<String, DatabaseAccess> _permissions;
    private final String _name;
    private final SecurityClearance _clearance;

    public EditClientCertificateOperation(Parameters parameters) {
        if (parameters == null) {
            throw new IllegalArgumentException("Parameters cannot be null");
        }

        if (parameters.getName() == null) {
            throw new IllegalArgumentException("Name cannot be null");
        }

        if (parameters.getThumbprint() == null) {
            throw new IllegalArgumentException("Thumbprint cannot be null");
        }

        if (parameters.getPermissions() == null) {
            throw new IllegalArgumentException("Permissions cannot be null");
        }

        this._name = parameters.getName();
        this._thumbprint = parameters.getThumbprint();
        this._permissions = parameters.getPermissions();
        this._clearance = parameters.getClearance();
    }

    @Override
    public VoidRavenCommand getCommand(DocumentConventions conventions) {
        return new EditClientCertificateCommand(conventions, _thumbprint, _name, _permissions, _clearance);
    }

    private static class EditClientCertificateCommand extends VoidRavenCommand implements IRaftCommand {
        private final DocumentConventions _conventions;
        private final String _thumbprint;
        private final Map<String, DatabaseAccess> _permissions;
        private final String _name;
        private final SecurityClearance _clearance;

        public EditClientCertificateCommand(DocumentConventions conventions, String thumbprint, String name, Map<String, DatabaseAccess> permissions, SecurityClearance clearance) {
            _conventions = conventions;
            _thumbprint = thumbprint;
            _name = name;
            _permissions = permissions;
            _clearance = clearance;
        }

        @Override
        public boolean isReadRequest() {
            return false;
        }

        @Override
        public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
            url.value = node.getUrl() + "/admin/certificates/edit";

            CertificateDefinition definition = new CertificateDefinition();
            definition.setThumbprint(_thumbprint);
            definition.setPermissions(_permissions);
            definition.setSecurityClearance(_clearance);
            definition.setName(_name);

            HttpPost request = new HttpPost();

            request.setEntity(new ContentProviderHttpEntity(outputStream -> {
                try (JsonGenerator generator = createSafeJsonGenerator(outputStream)) {
                    generator.getCodec().writeValue(generator, definition);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }, ContentType.APPLICATION_JSON, _conventions));
            return request;

        }

        @Override
        public String getRaftUniqueRequestId() {
            return RaftIdGenerator.newId();
        }
    }

    public static class Parameters {
        private String thumbprint;
        private Map<String, DatabaseAccess> permissions;
        private String name;
        private SecurityClearance clearance;

        public String getThumbprint() {
            return thumbprint;
        }

        public void setThumbprint(String thumbprint) {
            this.thumbprint = thumbprint;
        }

        public Map<String, DatabaseAccess> getPermissions() {
            return permissions;
        }

        public void setPermissions(Map<String, DatabaseAccess> permissions) {
            this.permissions = permissions;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public SecurityClearance getClearance() {
            return clearance;
        }

        public void setClearance(SecurityClearance clearance) {
            this.clearance = clearance;
        }
    }
}
