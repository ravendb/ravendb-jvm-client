package net.ravendb.client.util;

import net.ravendb.client.primitives.Tuple;
import net.ravendb.client.serverwide.commands.TcpConnectionInfo;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.ssl.SSLContexts;

import javax.net.ssl.SSLContext;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;

public class TcpUtils {

    public static Socket connect(String urlString, String serverCertificate, KeyStore clientCertificate, char[] certificatePrivateKeyPassword) throws IOException, GeneralSecurityException {
        URL url = new URL(urlString.replace("tcp://", "http://"));
        String host = url.getHost();
        int port = url.getPort();

        if (serverCertificate != null && clientCertificate != null) {
            // using https
            KeyStore trustStore = KeyStore.getInstance("PKCS12");
            trustStore.load(null, null);

            CertificateFactory x509 = CertificateFactory.getInstance("X509");

            ByteArrayInputStream certificateInputStream = new ByteArrayInputStream(Base64.decodeBase64(serverCertificate));
            Certificate certificate = x509.generateCertificate(certificateInputStream);
            trustStore.setCertificateEntry("server-cert", certificate);

            SSLContext context = SSLContexts.custom()
                    .setProtocol("TLSv1.2")
                    .loadKeyMaterial(clientCertificate, certificatePrivateKeyPassword)
                    .loadTrustMaterial(trustStore, (chain, authType) -> chain[0].equals(certificate))
                    .build();

            return context.getSocketFactory().createSocket(host, port);
        } else {
            return new Socket(host, port);
        }
    }

    public static Tuple<Socket, String> connectWithPriority(
            TcpConnectionInfo info,
            String serverCertificate,
            KeyStore clientCertificate,
            char[] certificatePrivateKeyPassword) throws IOException, GeneralSecurityException {
        Socket socket;
        if (info.getUrls() != null) {
            for (String url : info.getUrls()) {
                try {
                    socket = connect(url, serverCertificate, clientCertificate, certificatePrivateKeyPassword);
                    return Tuple.create(socket, url);
                } catch (Exception e) {
                    // ignored
                }
            }
        }

        socket = connect(info.getUrl(), serverCertificate, clientCertificate, certificatePrivateKeyPassword);

        return Tuple.create(socket, info.getUrl());
    }
}
