package net.ravendb.client.util;

import org.apache.commons.codec.binary.Hex;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.util.ArrayList;
import java.util.Collections;

public class CertificateUtils {
    public static String extractThumbprintFromCertificate(KeyStore certificate) {
        try {
            ArrayList<String> aliases = Collections.list(certificate.aliases());

            if (aliases.size() != 1) {
                throw new IllegalStateException("Expected single certificate in keystore.");
            }

            String alias = aliases.get(0);
            Certificate clientCertificate = certificate.getCertificate(alias);

            if (clientCertificate == null) {
                throw new IllegalStateException("Unable to find certificate for alias: '" + alias
                        + "'. If you generated certificate using RavenDB server, then it might be related to: " +
                        "https://github.com/dotnet/corefx/issues/30946. " +
                        "Please try to create Keystore using *.crt and *.key files instead of *.pfx.");
            }

            byte[] sha1 = MessageDigest.getInstance("SHA-1").digest(clientCertificate.getEncoded());
            return Hex.encodeHexString(sha1);

        } catch (KeyStoreException | NoSuchAlgorithmException | CertificateEncodingException e) {
            throw new IllegalStateException("Unable to extract certificate thumbprint " + e.getMessage(), e);
        }
    }
}
