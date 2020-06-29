package net.ravendb.client.util;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import com.google.common.primitives.Bytes;
import net.ravendb.client.exceptions.RavenException;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Collections;

public final class CertificateUtils {

    private static final String PKCS8_HEADER = "-----BEGIN PRIVATE KEY-----";
    private static final String PKCS8_FOOTER = "-----END PRIVATE KEY-----";
    private static final String PKCS1_HEADER = "-----BEGIN RSA PRIVATE KEY-----";
    private static final String PKCS1_FOOTER = "-----END RSA PRIVATE KEY-----";

    private CertificateUtils() {
    }

    public static Certificate readCertificate(String path) throws CertificateException, FileNotFoundException {
        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        return factory.generateCertificate(new FileInputStream(path));
    }

    /**
     * Reads private key from file.
     * It has support for PKCS1 (files with: -----BEGIN RSA PRIVATE KEY-----)
     * and PKCS8 (files with: -----BEGIN PRIVATE KEY-----)
     *
     * Type is autodetected.
     */
    public static PrivateKey readPrivateKey(String keyFilePath) throws GeneralSecurityException, IOException {
        byte[] bytes = IOUtils.toByteArray(new FileInputStream(keyFilePath));
        String keyString = new String(bytes, StandardCharsets.UTF_8);

        if (keyString.contains(PKCS1_HEADER)) {
            return readPkcs1PrivateKey(parseDERFromPEM(bytes, PKCS1_HEADER, PKCS1_FOOTER));
        }

        if (keyString.contains(PKCS8_HEADER)) {
            return readPkcs8PrivateKey(parseDERFromPEM(bytes, PKCS8_HEADER, PKCS8_FOOTER));
        }

        throw new RavenException("Unable to detect private key type. Expected '" + PKCS8_HEADER + "' or '" + PKCS1_HEADER + "'");
    }

    protected static byte[] parseDERFromPEM(byte[] pem, String beginDelimiter, String endDelimiter) {
        String data = new String(pem);
        String[] tokens = data.split(beginDelimiter);
        tokens = tokens[1].split(endDelimiter);
        return Base64.decodeBase64(tokens[0]);
    }

    /**
     * Create a PrivateKey instance from raw PKCS#8 bytes.
     */
    private static PrivateKey readPkcs8PrivateKey(byte[] pkcs8Bytes) throws GeneralSecurityException {
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(pkcs8Bytes);
        try {
            return keyFactory.generatePrivate(keySpec);
        } catch (InvalidKeySpecException e) {
            throw new IllegalArgumentException("Unexpected key format!", e);
        }
    }

    private static PrivateKey readPkcs1PrivateKey(byte[] bytes) throws GeneralSecurityException {
        int pkcs1Length = bytes.length;
        int totalLength = pkcs1Length + 22;
        byte[] pkcs8Header = new byte[] {
                0x30, (byte) 0x82, (byte) ((totalLength >> 8) & 0xff), (byte) (totalLength & 0xff), // Sequence + total length
                0x2, 0x1, 0x0, // Integer (0)
                0x30, 0xD, 0x6, 0x9, 0x2A, (byte) 0x86, 0x48, (byte) 0x86, (byte) 0xF7, 0xD, 0x1, 0x1, 0x1, 0x5, 0x0, // Sequence: 1.2.840.113549.1.1.1, NULL
                0x4, (byte) 0x82, (byte) ((pkcs1Length >> 8) & 0xff), (byte) (pkcs1Length & 0xff) // Octet string + length
        };
        byte[] pkcs8bytes = Bytes.concat(pkcs8Header, bytes);
        return readPkcs8PrivateKey(pkcs8bytes);
    }

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
