package net.ravendb.client.test;

import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.infrastructure.entities.User;
import net.ravendb.client.util.CertificateUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class CloudTest {

    @Test
    @Disabled("This test is mainly for demonstration purposes")
    public void canConnect() throws Exception {
        try (DocumentStore store = new DocumentStore("https://a.free.marcintest.ravendb.cloud", "db1")) {

            //String path = "C:\\temp\\free.marcintest.client.certificate\\PEM\\free.marcintest.client.certificate.pem";
            String path = "C:\\temp\\free.marcintest.client.certificate\\PEM\\pkcs8-pair.txt";

            store.setCertificate(CertificateUtils.createKeystore(path));
            store.initialize();

            try (IDocumentSession session = store.openSession()) {
                session.store(new User());
                session.saveChanges();
            }
        }
    }
}
