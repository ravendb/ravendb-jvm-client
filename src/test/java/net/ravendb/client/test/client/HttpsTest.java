package net.ravendb.client.test.client;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.exceptions.security.AuthorizationException;
import net.ravendb.client.infrastructure.entities.User;
import net.ravendb.client.serverwide.operations.certificates.*;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class HttpsTest extends RemoteTestBase {

    @Test
    public void canConnectWithCertificate() throws Exception {
        try (IDocumentStore store = getSecuredDocumentStore()) {
            try (IDocumentSession newSession = store.openSession()) {
                User user1 = new User();
                user1.setLastName("user1");
                newSession.store(user1, "users/1");
                newSession.saveChanges();
            }
        }
    }

    @Test
    public void canCrudCertificates() throws Exception {
        try (IDocumentStore store = getSecuredDocumentStore()) {

            String cert1Thumbprint = null;
            String cert2Thumbprint = null;

            try {
                // create cert1
                CertificateRawData cert1 = store.maintenance().server().send(
                        new CreateClientCertificateOperation("cert1", new HashMap<>(), SecurityClearance.OPERATOR));

                assertThat(cert1)
                        .isNotNull();
                assertThat(cert1.getRawData())
                        .isNotNull();

                Map<String, DatabaseAccess> clearance = new HashMap<>();
                clearance.put(store.getDatabase(), DatabaseAccess.READ_WRITE);
                CertificateRawData cert2 = store.maintenance().server().send(
                        new CreateClientCertificateOperation("cert2", clearance, SecurityClearance.VALID_USER));

                // create cert2
                assertThat(cert2)
                        .isNotNull();
                assertThat(cert2.getRawData())
                        .isNotNull();

                // list certs
                CertificateDefinition[] certificateDefinitions = store.maintenance().server().send(
                        new GetCertificatesOperation(0, 20));
                assertThat(certificateDefinitions.length)
                        .isGreaterThanOrEqualTo(2);

                assertThat(certificateDefinitions)
                        .extracting("name", String.class)
                        .contains("cert1");

                assertThat(certificateDefinitions)
                        .extracting(x -> x.getName())
                        .contains("cert2");

                cert1Thumbprint = Stream.of(certificateDefinitions).filter(x -> x.getName().equals("cert1")).findFirst().get().getThumbprint();
                cert2Thumbprint = Stream.of(certificateDefinitions).filter(x -> x.getName().equals("cert2")).findFirst().get().getThumbprint();

                // delete cert1
                store.maintenance().server().send(new DeleteCertificateOperation(cert1Thumbprint));

                // get cert by thumbprint

                CertificateDefinition definition = store.maintenance().server().send(new GetCertificateOperation(cert1Thumbprint));
                assertThat(definition)
                        .isNull();

                CertificateDefinition definition2 = store.maintenance().server().send(new GetCertificateOperation(cert2Thumbprint));
                assertThat(definition2)
                        .isNotNull();
                assertThat(definition2)
                        .matches(x -> x.getName().equals("cert2"));

                // list again
                certificateDefinitions = store.maintenance().server().send(new GetCertificatesOperation(0, 20));
                assertThat(certificateDefinitions)
                        .extracting(x -> x.getName())
                        .contains("cert2")
                        .doesNotContain("cert1");

                // extract public key from generated private key
                String publicKey = extractCertificate(cert1);

                // put cert1 again, using put certificate command
                PutClientCertificateOperation putOperation = new PutClientCertificateOperation("cert3", publicKey, new HashMap<>(), SecurityClearance.CLUSTER_ADMIN);
                store.maintenance().server().send(putOperation);
                certificateDefinitions = store.maintenance().server().send(new GetCertificatesOperation(0, 20));
                assertThat(certificateDefinitions)
                        .extracting(x -> x.getName())
                        .contains("cert2")
                        .doesNotContain("cert1")
                        .contains("cert3");

            } finally {
                // try to clean up
                if (cert1Thumbprint != null) {
                    store.maintenance().server().send(new DeleteCertificateOperation(cert1Thumbprint));
                }
                if (cert2Thumbprint != null) {
                    store.maintenance().server().send(new DeleteCertificateOperation(cert2Thumbprint));
                }
            }
        }
    }

    private String extractCertificate(CertificateRawData certificateRawData) throws Exception {
        try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(certificateRawData.getRawData()))) {
            ZipEntry zipEntry = zipInputStream.getNextEntry();

            if (zipEntry != null) {
                byte[] pkcs12Bytes = IOUtils.toByteArray(zipInputStream);
                KeyStore keyStore = KeyStore.getInstance("PKCS12");
                keyStore.load(new ByteArrayInputStream(pkcs12Bytes), "".toCharArray());


                String certAlias = keyStore.aliases().nextElement();
                Certificate certificate = keyStore.getCertificate(certAlias);

                return Base64.encodeBase64String(certificate.getEncoded());
            }
        }

        return null;
    }

    @Test
    public void shouldThrowAuthorizationExceptionWhenNotAuthorized() throws Exception {
        try (DocumentStore store = getSecuredDocumentStore()) {
            CertificateRawData certificateRawData = store.maintenance().server().send(
                    new CreateClientCertificateOperation("user-auth-test", Collections.singletonMap("db1", DatabaseAccess.READ_WRITE), SecurityClearance.VALID_USER));

            try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(certificateRawData.getRawData()))) {
                ZipEntry zipEntry = zipInputStream.getNextEntry();

                if (zipEntry != null) {
                    byte[] pkcs12Bytes = IOUtils.toByteArray(zipInputStream);
                    KeyStore keyStore = KeyStore.getInstance("PKCS12");
                    keyStore.load(new ByteArrayInputStream(pkcs12Bytes), "".toCharArray());


                    try (DocumentStore storeWithOutCert = new DocumentStore(store.getUrls(), store.getDatabase())) {
                        storeWithOutCert.setTrustStore(store.getTrustStore());
                        storeWithOutCert.setCertificate(keyStore); // using this certificate user won't have an access to current db
                        storeWithOutCert.initialize();

                        assertThatThrownBy(() -> {
                            try (IDocumentSession session = storeWithOutCert.openSession()) {
                                User user = session.load(User.class, "users/1");
                            }
                        }).isExactlyInstanceOf(AuthorizationException.class);
                    }
                }
            }
        }
    }
}
