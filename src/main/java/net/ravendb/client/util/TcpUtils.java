package net.ravendb.client.util;

import net.ravendb.client.serverwide.commands.TcpConnectionInfo;
import net.ravendb.client.serverwide.tcp.TcpConnectionHeaderMessage;
import org.apache.commons.lang3.NotImplementedException;

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

    public static ConnectSecuredTcpSocketResult connectSecuredTcpSocket(
            TcpConnectionInfo info,
            String serverCertificate,
            KeyStore clientCertificate,
            char[] certificatePrivateKeyPassword,
            TcpConnectionHeaderMessage.OperationTypes operationType,
            NegotiationCallback negotiationCallback) throws IOException, GeneralSecurityException {
        Socket socket;
        if (info.getUrls() != null) {
            for (String url : info.getUrls()) {
                try {
                    socket = connect(url, serverCertificate, clientCertificate, certificatePrivateKeyPassword);

                    TcpConnectionHeaderMessage.SupportedFeatures supportedFeatures = invokeNegotiation(info, operationType, negotiationCallback, url, socket);

                    return new ConnectSecuredTcpSocketResult(url, socket, supportedFeatures);
                } catch (Exception e) {
                    // ignored
                }
            }
        }

        socket = connect(info.getUrl(), serverCertificate, clientCertificate, certificatePrivateKeyPassword);

        TcpConnectionHeaderMessage.SupportedFeatures supportedFeatures = invokeNegotiation(info, operationType, negotiationCallback, info.getUrl(), socket);

        return new ConnectSecuredTcpSocketResult(info.getUrl(), socket, supportedFeatures);
    }

    private static TcpConnectionHeaderMessage.SupportedFeatures invokeNegotiation(
            TcpConnectionInfo info,
            TcpConnectionHeaderMessage.OperationTypes operationType,
            NegotiationCallback negotiationCallback,
            String url,
            Socket socket) throws IOException {

        switch (operationType) {
            case SUBSCRIPTION:
                return negotiationCallback.apply(url, info, socket);
            default:
                throw new NotImplementedException("Operation type '" + operationType + "' not supported");
        }
    }

    public interface NegotiationCallback {
        TcpConnectionHeaderMessage.SupportedFeatures apply(String url, TcpConnectionInfo info, Socket socket) throws IOException;
    }

    public static class ConnectSecuredTcpSocketResult {
        public String url;
        public Socket socket;
        public TcpConnectionHeaderMessage.SupportedFeatures supportedFeatures;

        public ConnectSecuredTcpSocketResult(String url, Socket socket, TcpConnectionHeaderMessage.SupportedFeatures supportedFeatures) {
            this.url = url;
            this.socket = socket;
            this.supportedFeatures = supportedFeatures;
        }
    }
}
