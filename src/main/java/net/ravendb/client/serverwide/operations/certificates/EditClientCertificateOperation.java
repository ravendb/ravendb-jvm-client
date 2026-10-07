package net.ravendb.client.serverwide.operations.certificates;

import com.fasterxml.jackson.core.JsonGenerator;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.http.IRaftCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.http.VoidRavenCommand;
import net.ravendb.client.json.ContentProviderHttpEntity;
import net.ravendb.client.primitives.SharpEnum;
import net.ravendb.client.serverwide.operations.IVoidServerOperation;
import net.ravendb.client.util.RaftIdGenerator;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.core5.http.ContentType;

import java.util.List;
import java.util.Map;

public class EditClientCertificateOperation implements IVoidServerOperation {

    private final String _thumbprint;
    private final Map<String, DatabaseAccess> _permissions;
    private final String _name;
    private final SecurityClearance _clearance;
    private final boolean _disabled;
    private final List<String> _ssoServerPublicKeyPinningHashes;
    private final Boolean _allowAnySsoServer;
    private final List<SsoIdentifier> _ssoIdentifiers;

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
        this._disabled = parameters.isDisabled();
        this._ssoServerPublicKeyPinningHashes = parameters.getSsoServerPublicKeyPinningHashes();
        this._allowAnySsoServer = parameters.getAllowAnySsoServer();
        this._ssoIdentifiers = parameters.getSsoIdentifiers();
    }

    @Override
    public VoidRavenCommand getCommand(DocumentConventions conventions) {
        return new EditClientCertificateCommand(conventions, _thumbprint, _name, _permissions, _clearance, _disabled,
                _ssoServerPublicKeyPinningHashes, _allowAnySsoServer, _ssoIdentifiers);
    }

    private static class EditClientCertificateCommand extends VoidRavenCommand implements IRaftCommand {
        private final DocumentConventions _conventions;
        private final String _thumbprint;
        private final Map<String, DatabaseAccess> _permissions;
        private final String _name;
        private final SecurityClearance _clearance;
        private final boolean _disabled;
        private final List<String> _ssoServerPublicKeyPinningHashes;
        private final Boolean _allowAnySsoServer;
        private final List<SsoIdentifier> _ssoIdentifiers;

        public EditClientCertificateCommand(DocumentConventions conventions, String thumbprint, String name,
                                            Map<String, DatabaseAccess> permissions, SecurityClearance clearance, boolean disabled,
                                            List<String> ssoServerPublicKeyPinningHashes, Boolean allowAnySsoServer,
                                            List<SsoIdentifier> ssoIdentifiers) {
            _conventions = conventions;
            _thumbprint = thumbprint;
            _name = name;
            _permissions = permissions;
            _clearance = clearance;
            _disabled = disabled;
            _ssoServerPublicKeyPinningHashes = ssoServerPublicKeyPinningHashes;
            _allowAnySsoServer = allowAnySsoServer;
            _ssoIdentifiers = ssoIdentifiers;
        }

        @Override
        public boolean isReadRequest() {
            return false;
        }

        @Override
        public HttpUriRequestBase createRequest(ServerNode node) {
            String url = node.getUrl() + "/admin/certificates/edit";

            HttpPost request = new HttpPost(url);

            request.setEntity(new ContentProviderHttpEntity(outputStream -> {
                try (JsonGenerator generator = createSafeJsonGenerator(outputStream)) {
                    generator.writeStartObject();

                    generator.writeStringField("Thumbprint", _thumbprint);
                    generator.writeStringField("Name", _name);
                    generator.writeStringField("SecurityClearance", _clearance != null ? SharpEnum.value(_clearance) : null);
                    generator.writeBooleanField("Disabled", _disabled);

                    generator.writeFieldName("Permissions");
                    generator.writeStartObject();
                    for (Map.Entry<String, DatabaseAccess> kvp : _permissions.entrySet()) {
                        generator.writeStringField(kvp.getKey(), kvp.getValue() != null ? SharpEnum.value(kvp.getValue()) : null);
                    }
                    generator.writeEndObject();

                    // The SSO fields are written only when explicitly provided so the server leaves the existing
                    // SSO configuration untouched on a partial edit, and clears it when an empty list is sent.
                    if (_ssoServerPublicKeyPinningHashes != null) {
                        generator.writeFieldName("SsoServerPublicKeyPinningHashes");
                        generator.writeStartArray();
                        for (String hash : _ssoServerPublicKeyPinningHashes) {
                            generator.writeString(hash);
                        }
                        generator.writeEndArray();
                    }

                    if (_allowAnySsoServer != null) {
                        generator.writeBooleanField("AllowAnySsoServer", _allowAnySsoServer);
                    }

                    if (_ssoIdentifiers != null) {
                        generator.writeFieldName("SsoIdentifiers");
                        generator.writeStartArray();
                        for (SsoIdentifier id : _ssoIdentifiers) {
                            generator.writeStartObject();
                            generator.writeStringField("Provider", id.getProvider() != null ? SharpEnum.value(id.getProvider()) : null);
                            generator.writeStringField("Identifier", id.getIdentifier());
                            if (StringUtils.isNotEmpty(id.getDomain())) {
                                generator.writeStringField("Domain", id.getDomain());
                            }
                            generator.writeEndObject();
                        }
                        generator.writeEndArray();
                    }

                    generator.writeEndObject();
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
        private boolean disabled;

        // SSO configuration is opt-in: leave these null to keep the existing SSO settings untouched (which is what
        // a regular client-certificate edit, or a disabled-only toggle on an SSO user, wants). Setting any of them
        // (even to an empty list) fully replaces the stored value - that is how an SSO user's authorizing servers
        // or identifiers can be cleared.
        private List<String> ssoServerPublicKeyPinningHashes;
        private Boolean allowAnySsoServer;
        private List<SsoIdentifier> ssoIdentifiers;

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

        public boolean isDisabled() {
            return disabled;
        }

        public void setDisabled(boolean disabled) {
            this.disabled = disabled;
        }

        /**
         * @return the public key pinning hashes of the SSO servers allowed to authorize this certificate's user,
         *         or null to leave the stored value untouched
         */
        public List<String> getSsoServerPublicKeyPinningHashes() {
            return ssoServerPublicKeyPinningHashes;
        }

        /**
         * Sets the public key pinning hashes of the SSO servers allowed to authorize this certificate's user.
         * Leave null to keep the existing value; pass an empty list to clear it.
         * @param ssoServerPublicKeyPinningHashes the pinning hashes, or null
         */
        public void setSsoServerPublicKeyPinningHashes(List<String> ssoServerPublicKeyPinningHashes) {
            this.ssoServerPublicKeyPinningHashes = ssoServerPublicKeyPinningHashes;
        }

        /**
         * @return whether any SSO server may authorize this certificate's user, or null to leave the stored
         *         value untouched
         */
        public Boolean getAllowAnySsoServer() {
            return allowAnySsoServer;
        }

        /**
         * Sets whether any SSO server may authorize this certificate's user. Leave null to keep the existing value.
         * @param allowAnySsoServer the flag, or null
         */
        public void setAllowAnySsoServer(Boolean allowAnySsoServer) {
            this.allowAnySsoServer = allowAnySsoServer;
        }

        /**
         * @return the SSO principals this certificate authorizes, or null to leave the stored value untouched
         */
        public List<SsoIdentifier> getSsoIdentifiers() {
            return ssoIdentifiers;
        }

        /**
         * Sets the SSO principals this certificate authorizes. Leave null to keep the existing value;
         * pass an empty list to clear it.
         * @param ssoIdentifiers the SSO identifiers, or null
         */
        public void setSsoIdentifiers(List<SsoIdentifier> ssoIdentifiers) {
            this.ssoIdentifiers = ssoIdentifiers;
        }
    }
}
