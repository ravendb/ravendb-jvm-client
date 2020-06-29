package net.ravendb.client.test.client;

import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.infrastructure.entities.User;
import net.ravendb.client.serverwide.DatabaseRecord;
import net.ravendb.client.util.CertificateUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;

public class CloudTest {

    @Test
    @Disabled("This test is mainly for demonstration purposes")
    public void canConnect() throws Exception {
        try (DocumentStore store = new DocumentStore("https://a.free.marcintest.ravendb.cloud", "db1")) {

            //String path = "C:\\temp\\free.marcintest.client.certificate\\PEM\\free.marcintest.client.certificate.pem";
            String path = "C:\\temp\\free.marcintest.client.certificate\\PEM\\pkcs8-pair.txt";
            Certificate certificate = CertificateUtils.readCertificate(path);
            PrivateKey privateKey = CertificateUtils.readPrivateKey(path);

            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(null, null);

            keyStore.setKeyEntry("a", privateKey, "".toCharArray(), new Certificate[] { certificate });

            store.setCertificate(keyStore);
            store.initialize();

            try (IDocumentSession session = store.openSession()) {
                session.store(new User());
                session.saveChanges();
            }
        }
    }
}
