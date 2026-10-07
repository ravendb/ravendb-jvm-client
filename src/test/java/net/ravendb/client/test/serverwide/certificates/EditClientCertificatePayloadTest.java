package net.ravendb.client.test.serverwide.certificates;

import com.fasterxml.jackson.databind.JsonNode;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.extensions.JsonExtensions;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.http.VoidRavenCommand;
import net.ravendb.client.serverwide.operations.certificates.DatabaseAccess;
import net.ravendb.client.serverwide.operations.certificates.EditClientCertificateOperation;
import net.ravendb.client.serverwide.operations.certificates.SecurityClearance;
import net.ravendb.client.serverwide.operations.certificates.SsoIdentifier;
import net.ravendb.client.serverwide.operations.certificates.SsoProvider;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the wire payload of {@link EditClientCertificateOperation} without needing a server.
 *
 * <p>
 * The SSO fields are opt-in: the server fully replaces a stored value whenever the field is present,
 * so a plain client-certificate edit must not emit them at all - otherwise it would silently clear an
 * SSO user's authorizing servers and identifiers.
 * </p>
 */
public class EditClientCertificatePayloadTest {

    private static JsonNode buildPayload(EditClientCertificateOperation.Parameters parameters) throws Exception {
        DocumentConventions conventions = new DocumentConventions();
        conventions.setUseHttpCompression(false);

        VoidRavenCommand command = new EditClientCertificateOperation(parameters).getCommand(conventions);

        ServerNode node = new ServerNode();
        node.setUrl("http://localhost:8080");
        node.setDatabase("db1");

        HttpUriRequestBase request = command.createRequest(node);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        request.getEntity().writeTo(stream);

        return JsonExtensions.getDefaultMapper().readTree(new String(stream.toByteArray(), StandardCharsets.UTF_8));
    }

    private static EditClientCertificateOperation.Parameters baseParameters() {
        Map<String, DatabaseAccess> permissions = new HashMap<>();
        permissions.put("db1", DatabaseAccess.READ_WRITE);

        EditClientCertificateOperation.Parameters parameters = new EditClientCertificateOperation.Parameters();
        parameters.setName("cert1");
        parameters.setThumbprint("ABC123");
        parameters.setClearance(SecurityClearance.VALID_USER);
        parameters.setPermissions(permissions);
        parameters.setDisabled(true);
        return parameters;
    }

    @Test
    public void writesCoreFieldsAndOmitsSsoFieldsWhenNotProvided() throws Exception {
        JsonNode json = buildPayload(baseParameters());

        assertThat(json.get("Thumbprint").asText()).isEqualTo("ABC123");
        assertThat(json.get("Name").asText()).isEqualTo("cert1");
        assertThat(json.get("SecurityClearance").asText()).isEqualTo("ValidUser");
        assertThat(json.get("Disabled").asBoolean()).isTrue();
        assertThat(json.get("Permissions").get("db1").asText()).isEqualTo("ReadWrite");

        // Not provided => must be absent so the server keeps the stored SSO configuration.
        assertThat(json.has("SsoServerPublicKeyPinningHashes")).isFalse();
        assertThat(json.has("AllowAnySsoServer")).isFalse();
        assertThat(json.has("SsoIdentifiers")).isFalse();
    }

    @Test
    public void writesSsoFieldsWhenProvided() throws Exception {
        SsoIdentifier identifier = new SsoIdentifier();
        identifier.setProvider(SsoProvider.GITHUB);
        identifier.setIdentifier("octocat");
        identifier.setDomain("github.com");

        EditClientCertificateOperation.Parameters parameters = baseParameters();
        parameters.setSsoServerPublicKeyPinningHashes(Arrays.asList("hash1", "hash2"));
        parameters.setAllowAnySsoServer(true);
        parameters.setSsoIdentifiers(Arrays.asList(identifier));

        JsonNode json = buildPayload(parameters);

        assertThat(json.get("SsoServerPublicKeyPinningHashes")).hasSize(2);
        assertThat(json.get("SsoServerPublicKeyPinningHashes").get(0).asText()).isEqualTo("hash1");
        assertThat(json.get("AllowAnySsoServer").asBoolean()).isTrue();

        assertThat(json.get("SsoIdentifiers")).hasSize(1);
        JsonNode written = json.get("SsoIdentifiers").get(0);
        assertThat(written.get("Provider").asText()).isEqualTo("Github");
        assertThat(written.get("Identifier").asText()).isEqualTo("octocat");
        assertThat(written.get("Domain").asText()).isEqualTo("github.com");
    }

    @Test
    public void emptySsoListsAreWrittenSoTheyClearTheStoredValue() throws Exception {
        EditClientCertificateOperation.Parameters parameters = baseParameters();
        parameters.setSsoServerPublicKeyPinningHashes(new ArrayList<>());
        parameters.setSsoIdentifiers(new ArrayList<>());

        JsonNode json = buildPayload(parameters);

        assertThat(json.get("SsoServerPublicKeyPinningHashes").isArray()).isTrue();
        assertThat(json.get("SsoServerPublicKeyPinningHashes")).isEmpty();
        assertThat(json.get("SsoIdentifiers").isArray()).isTrue();
        assertThat(json.get("SsoIdentifiers")).isEmpty();

        // AllowAnySsoServer was left null and must still be absent.
        assertThat(json.has("AllowAnySsoServer")).isFalse();
    }

    @Test
    public void domainIsOmittedWhenEmpty() throws Exception {
        SsoIdentifier identifier = new SsoIdentifier();
        identifier.setProvider(SsoProvider.WINDOWS);
        identifier.setIdentifier("DOMAIN\\user");

        List<SsoIdentifier> identifiers = new ArrayList<>();
        identifiers.add(identifier);

        EditClientCertificateOperation.Parameters parameters = baseParameters();
        parameters.setSsoIdentifiers(identifiers);

        JsonNode json = buildPayload(parameters);

        JsonNode written = json.get("SsoIdentifiers").get(0);
        assertThat(written.get("Provider").asText()).isEqualTo("Windows");
        assertThat(written.has("Domain")).isFalse();
    }
}
