package net.ravendb.client;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Sets;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.driver.RavenServerLocator;
import net.ravendb.client.driver.RavenTestDriver;
import net.ravendb.client.exceptions.cluster.NoLeaderException;
import net.ravendb.client.exceptions.database.DatabaseDoesNotExistException;
import net.ravendb.client.primitives.CleanCloseable;
import net.ravendb.client.primitives.ExceptionsUtils;
import net.ravendb.client.primitives.Reference;
import net.ravendb.client.serverwide.DatabaseRecord;
import net.ravendb.client.serverwide.operations.CreateDatabaseOperation;
import net.ravendb.client.serverwide.operations.DeleteDatabasesOperation;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

@SuppressWarnings("SameParameterValue")
public class RemoteTestBase extends RavenTestDriver implements CleanCloseable {

    public final SamplesTestBase samples;

    public final IndexesTestBase indexes;

    public final ReplicationTestBase2 replication;

    private final RavenServerLocator locator;
    private final RavenServerLocator securedLocator;

    private static IDocumentStore globalServer;
    private static Process globalServerProcess;

    private static IDocumentStore globalSecuredServer;
    private static Process globalSecuredServerProcess;

    private final Set<DocumentStore> documentStores = Sets.newConcurrentHashSet();

    private static final AtomicInteger index = new AtomicInteger();

    private static class TestServiceLocator extends RavenServerLocator {

        @Override
        public String[] getCommandArguments() {
            return new String[] {
                    "--ServerUrl=http://127.0.0.1:0",
                    "--ServerUrl.Tcp=tcp://127.0.0.1:38881",
                    "--Features.Availability=Experimental"
            };
        }
    }

    private static class TestSecuredServiceLocator extends RavenServerLocator {

        public static final String ENV_CERTIFICATE_PATH = "RAVENDB_JAVA_TEST_CERTIFICATE_PATH";

        public static final String ENV_TEST_CA_PATH = "RAVENDB_JAVA_TEST_CA_PATH";

        public static final String ENV_HTTPS_SERVER_URL = "RAVENDB_JAVA_TEST_HTTPS_SERVER_URL";

        @Override
        public String[] getCommandArguments() {
            String httpsServerUrl = getHttpsServerUrl();

            try {
                URL url = new URL(httpsServerUrl);
                String host = url.getHost();
                String tcpServerUrl = "tcp://" + host + ":38882";

                return new String[]{
                        "--Security.Certificate.Path=" + getServerCertificatePath(),
                        "--ServerUrl=" + httpsServerUrl,
                        "--ServerUrl.Tcp=" + tcpServerUrl
                };
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }

        private String getHttpsServerUrl() {
            String httpsServerUrl = System.getenv(ENV_HTTPS_SERVER_URL);
            if (StringUtils.isBlank(httpsServerUrl)) {
                throw new IllegalStateException("Unable to find RavenDB https server url. " +
                        "Please make sure " + ENV_HTTPS_SERVER_URL + " environment variable is set and is valid " +
                        "(current value = " + httpsServerUrl + ")");
            }

            return httpsServerUrl;
        }

        @Override
        public String getServerCertificatePath() {
            String certificatePath = System.getenv(ENV_CERTIFICATE_PATH);
            if (StringUtils.isBlank(certificatePath)) {
                throw new IllegalStateException("Unable to find RavenDB server certificate path. " +
                        "Please make sure " + ENV_CERTIFICATE_PATH + " environment variable is set and is valid " +
                        "(current value = " + certificatePath + ")");
            }

            return certificatePath;
        }


        @Override
        public String getServerCaPath() {
            return System.getenv(ENV_TEST_CA_PATH);
        }
    }

    public RemoteTestBase() {
        this.locator = new TestServiceLocator();
        this.securedLocator = new TestSecuredServiceLocator();

        this.samples = new SamplesTestBase(this);
        this.indexes = new IndexesTestBase(this);
        this.replication = new ReplicationTestBase2(this);
    }

    protected void customizeDbRecord(DatabaseRecord dbRecord) {

    }

    protected void customizeStore(DocumentStore store) {

    }

    public DocumentStore getSecuredDocumentStore() throws Exception {
        return getDocumentStore("test_db", true, null);
    }

    public KeyStore getTestClientCertificate() throws IOException, GeneralSecurityException {
        KeyStore clientStore = KeyStore.getInstance("PKCS12");
        clientStore.load(new FileInputStream(securedLocator.getServerCertificatePath()), "".toCharArray());
        return clientStore;
    }

    public KeyStore getTestCaCertificate() throws IOException, GeneralSecurityException {
        String caPath = securedLocator.getServerCaPath();
        if (caPath != null) {
            KeyStore trustStore = KeyStore.getInstance("PKCS12");
            trustStore.load(null, null);

            CertificateFactory x509 = CertificateFactory.getInstance("X509");

            try (InputStream source = new FileInputStream(new File(caPath))) {
                Certificate certificate = x509.generateCertificate(source);
                trustStore.setCertificateEntry("ca-cert", certificate);
                return trustStore;
            }
        }

        return null;
    }

    @SuppressWarnings("UnusedReturnValue")
    private IDocumentStore runServer(boolean secured) throws Exception {
        Reference<Process> processReference = new Reference<>();
        IDocumentStore store = runServerInternal(getLocator(secured), processReference, s -> {
            if (secured) {
                try {
                    KeyStore clientCert = getTestClientCertificate();
                    s.setCertificate(clientCert);
                    s.setTrustStore(getTestCaCertificate());
                } catch (Exception e) {
                    throw ExceptionsUtils.unwrapException(e);
                }
            }
        });
        setGlobalServerProcess(secured, processReference.value);

        if (secured) {
            globalSecuredServer = store;
        } else {
            globalServer = store;
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> killGlobalServerProcess(secured)));
        return store;
    }

    private RavenServerLocator getLocator(boolean secured) {
        return secured ? securedLocator : locator;
    }

    private static IDocumentStore getGlobalServer(boolean secured) {
        return secured ? globalSecuredServer : globalServer;
    }

    private static Process getGlobalProcess(boolean secured) {
        return secured ? globalSecuredServerProcess : globalServerProcess;
    }

    private static void setGlobalServerProcess(boolean secured, Process p) {
        if (secured) {
            globalSecuredServerProcess = p;
        } else {
            globalServerProcess = p;
        }
    }

    private static void killGlobalServerProcess(boolean secured) {
        Process p;
        if (secured) {
            p = globalSecuredServerProcess;
            globalSecuredServerProcess = null;
            globalSecuredServer.close();
            globalSecuredServer = null;
        } else {
            p = globalServerProcess;
            globalServerProcess = null;
            globalServer.close();
            globalServer = null;
        }

        killProcess(p);
    }

    public DocumentStore getDocumentStore() throws Exception {
        return getDocumentStore("test_db");
    }

    public DocumentStore getSecuredDocumentStore(String database) throws Exception {
        return getDocumentStore(database, true, null);
    }

    public DocumentStore getDocumentStore(String database) throws Exception {
        return getDocumentStore(database, false, null);
    }

    public DocumentStore getDocumentStore(String database, boolean secured, Duration waitForIndexingTimeout) throws Exception {
        String name = database + "_" + index.incrementAndGet();
        reportInfo("getDocumentStore for db " + database + ".");

        if (getGlobalServer(secured) == null) {
            synchronized (RavenTestDriver.class) {
                if (getGlobalServer(secured) == null) {
                    runServer(secured);
                }
            }
        }

        IDocumentStore documentStore = getGlobalServer(secured);
        DatabaseRecord databaseRecord = new DatabaseRecord();
        databaseRecord.setDatabaseName(name);

        customizeDbRecord(databaseRecord);

        CreateDatabaseOperation createDatabaseOperation = new CreateDatabaseOperation(databaseRecord);
        documentStore.maintenance().server().send(createDatabaseOperation);


        DocumentStore store = new DocumentStore();
        store.setUrls(documentStore.getUrls());
        store.setDatabase(name);

        if (secured) {
            store.setCertificate(getTestClientCertificate());
            store.setTrustStore(getTestClientCertificate());
        }

        customizeStore(store);

        hookLeakedConnectionCheck(store);
        store.initialize();

        store.addAfterCloseListener(((sender, event) -> {
            if (!documentStores.contains(store)) {
                return;
            }

            try {
                store.maintenance().server().send(new DeleteDatabasesOperation(store.getDatabase(), true));
            } catch (DatabaseDoesNotExistException | NoLeaderException e) {
                // ignore
            }
        }));

        setupDatabase(store);

        if (waitForIndexingTimeout != null) {
            waitForIndexing(store, name, waitForIndexingTimeout);
        }

        documentStores.add(store);
        return store;
    }



    @Override
    public void close() {
        if (disposed) {
            return;
        }

        ArrayList<Exception> exceptions = new ArrayList<>();

        for (DocumentStore documentStore : documentStores) {
            try {
                documentStore.close();
            } catch (Exception e) {
                exceptions.add(e);
            }
        }

        disposed = true;

        if (exceptions.size() > 0) {
            throw new RuntimeException(exceptions
                    .stream()
                    .map(x -> x.toString())
                    .collect(Collectors.joining(", ")));
        }
    }

    protected <T> boolean waitForDocument(Class<T> clazz, IDocumentStore store, String docId) {
        return waitForDocument(clazz, store, docId, null, 10_000);
    }

    protected <T> boolean waitForDocument(Class<T> clazz, IDocumentStore store, String docId,
                                          Function<T, Boolean> predicate, long timeout) {
        Stopwatch sw = Stopwatch.createStarted();
        Exception ex = null;
        while (sw.elapsed().toMillis() < timeout) {
            try (IDocumentSession session = store.openSession(store.getDatabase())) {
                try {
                    T doc = session.load(clazz, docId);
                    if (doc != null) {
                        if (predicate == null || predicate.apply(doc)) {
                            return true;
                        }
                    }
                } catch (Exception e) {
                    ex = e;
                }
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // empty
            }
        }
        return false;
    }
}
