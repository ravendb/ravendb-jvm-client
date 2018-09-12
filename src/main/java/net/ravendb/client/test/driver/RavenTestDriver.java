package net.ravendb.client.test.driver;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Sets;
import net.ravendb.client.Constants;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.indexes.IndexErrors;
import net.ravendb.client.documents.indexes.IndexState;
import net.ravendb.client.documents.operations.DatabaseStatistics;
import net.ravendb.client.documents.operations.GetStatisticsOperation;
import net.ravendb.client.documents.operations.IndexInformation;
import net.ravendb.client.documents.operations.MaintenanceOperationExecutor;
import net.ravendb.client.documents.operations.indexes.GetIndexErrorsOperation;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.exceptions.TimeoutException;
import net.ravendb.client.exceptions.cluster.NoLeaderException;
import net.ravendb.client.exceptions.database.DatabaseDoesNotExistException;
import net.ravendb.client.primitives.CleanCloseable;
import net.ravendb.client.serverwide.DatabaseRecord;
import net.ravendb.client.serverwide.operations.CreateDatabaseOperation;
import net.ravendb.client.serverwide.operations.DeleteDatabasesOperation;
import net.ravendb.client.util.UrlUtils;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

import java.awt.*;
import java.io.*;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class RavenTestDriver implements CleanCloseable {

    private final RavenServerLocator locator;
    private final RavenServerLocator securedLocator;

    private static IDocumentStore globalServer;
    private static Process globalServerProcess;

    private static IDocumentStore globalSecuredServer;
    private static Process globalSecuredServerProcess;

    private final Set<DocumentStore> documentStores = Sets.newConcurrentHashSet();

    private static final AtomicInteger index = new AtomicInteger();

    protected boolean disposed;

    public RavenTestDriver(RavenServerLocator locator, RavenServerLocator securedLocator) {
        this.locator = locator;
        this.securedLocator = securedLocator;
    }

    public boolean isDisposed() {
        return disposed;
    }

    public static boolean debug;

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

    public DocumentStore getDocumentStore() throws Exception {
        return getDocumentStore("test_db");
    }

    public DocumentStore getSecuredDocumentStore(String database) throws Exception {
        return getDocumentStore(database, true, null);
    }

    public DocumentStore getDocumentStore(String database) throws Exception {
        return getDocumentStore(database, false, null);
    }

    protected void customizeDbRecord(DatabaseRecord dbRecord) {

    }

    protected void customizeStore(DocumentStore store) {

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

    private void hookLeakedConnectionCheck(DocumentStore store) {
        store.addBeforeCloseListener((sender, event) -> {
            try {
                CloseableHttpClient httpClient = store.getRequestExecutor().getHttpClient();

                Field connManager = httpClient.getClass().getDeclaredField("connManager");
                connManager.setAccessible(true);
                PoolingHttpClientConnectionManager connectionManager = (PoolingHttpClientConnectionManager) connManager.get(httpClient);

                int leased = connectionManager.getTotalStats().getLeased();
                if (leased > 0 ) {
                    Thread.sleep(100);

                    // give another try
                    leased = connectionManager.getTotalStats().getLeased();

                    if (leased > 0) {
                        throw new IllegalStateException("Looks like you have leaked " + leased + " connections!");
                    }

                    /*  debug code to find actual connections
                    Field poolField = connectionManager.getClass().getDeclaredField("pool");
                    poolField.setAccessible(true);
                    AbstractConnPool pool = (AbstractConnPool) poolField.get(connectionManager);
                    Field leasedField = pool.getClass().getSuperclass().getDeclaredField("leased");
                    leasedField.setAccessible(true);
                    Set leasedConnections = (Set) leasedField.get(pool);*/
                }
            } catch (NoSuchFieldException | IllegalAccessException | InterruptedException e) {
                throw new IllegalStateException("Unable to check for leaked connections", e);
            }

        });
    }


    @SuppressWarnings("EmptyMethod")
    protected void setupDatabase(IDocumentStore documentStore) {
        // empty by design
    }

    @SuppressWarnings("UnusedReturnValue")
    private IDocumentStore runServer(boolean secured) throws Exception {
        Process process = RavenServerRunner.run(secured ? this.securedLocator : this.locator);
        setGlobalServerProcess(secured, process);

        reportInfo("Starting global server");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> killGlobalServerProcess(secured)));

        String url = null;
        InputStream stdout = getGlobalProcess(secured).getInputStream();

        Stopwatch startupDuration = Stopwatch.createStarted();
        BufferedReader reader = new BufferedReader(new InputStreamReader(stdout));

        List<String> readLines = new ArrayList<>();

        while (true) {
            String line = reader.readLine();
            readLines.add(line);

            if (line == null) {
                throw new RuntimeException(readLines.stream().collect(Collectors.joining(System.lineSeparator())) +  IOUtils.toString(getGlobalProcess(secured).getInputStream(), "UTF-8"));
            }

            if (startupDuration.elapsed(TimeUnit.MINUTES) >= 1) {
                break;
            }

            String prefix = "Server available on: ";
            if (line.startsWith(prefix)) {
                url = line.substring(prefix.length());
                break;
            }
        }

        if (url == null) {
            reportInfo("Url is null");

            try {
                process.destroyForcibly();
            } catch (Exception e) {
                reportError(e);
            }

            throw new IllegalStateException("Unable to start server");
        }

        DocumentStore store = new DocumentStore();
        store.setUrls(new String[]{url});
        store.setDatabase("test.manager");
        store.getConventions().setDisableTopologyUpdates(true);

        if (secured) {
            globalSecuredServer = store;
            KeyStore clientCert = getTestClientCertificate();
            store.setCertificate(clientCert);
            store.setTrustStore(getTestCaCertificate());
        } else {
            globalServer = store;
        }
        return store.initialize();
    }

    private static void killGlobalServerProcess(boolean secured) {
        Process p;
        if (secured) {
            p = RavenTestDriver.globalSecuredServerProcess;
            globalSecuredServerProcess = null;
            globalSecuredServer.close();
            globalSecuredServer = null;
        } else {
            p = RavenTestDriver.globalServerProcess;
            globalServerProcess = null;
            globalServer.close();
            globalServer = null;
        }

        if (p != null && p.isAlive()) {
            reportInfo("Kill global server");

            try {
                p.destroyForcibly();
            } catch (Exception e) {
                reportError(e);
            }
        }
    }

    private IDocumentStore getGlobalServer(boolean secured) {
        return secured ? globalSecuredServer : globalServer;
    }

    private Process getGlobalProcess(boolean secured) {
        return secured ? globalSecuredServerProcess : globalServerProcess;
    }

    private void setGlobalServerProcess(boolean secured, Process p) {
        if (secured) {
            globalSecuredServerProcess = p;
        } else {
            globalServerProcess = p;
        }
    }

    public static void waitForIndexing(IDocumentStore store) {
        waitForIndexing(store, null, null);
    }

    public static void waitForIndexing(IDocumentStore store, String database) {
        waitForIndexing(store, database, null);
    }

    public static void waitForIndexing(IDocumentStore store, String database, Duration timeout) {
        MaintenanceOperationExecutor admin = store.maintenance().forDatabase(database);

        if (timeout == null) {
            timeout = Duration.ofMinutes(1);
        }

        Stopwatch sp = Stopwatch.createStarted();

        while (sp.elapsed(TimeUnit.MILLISECONDS) < timeout.toMillis()) {
            DatabaseStatistics databaseStatistics = admin.send(new GetStatisticsOperation());

            List<IndexInformation> indexes = Arrays.stream(databaseStatistics.getIndexes())
                    .filter(x -> !IndexState.DISABLED.equals(x.getState()))
                    .collect(Collectors.toList());

            if (indexes.stream().allMatch(x -> !x.isStale() &&
                    !x.getName().startsWith(Constants.Documents.Indexing.SIDE_BY_SIDE_INDEX_NAME_PREFIX))) {
                return;
            }

            if (Arrays.stream(databaseStatistics.getIndexes()).anyMatch(x -> IndexState.ERROR.equals(x.getState()))) {
                break;
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }


        IndexErrors[] errors = admin.send(new GetIndexErrorsOperation());
        String allIndexErrorsText = "";
        Function<IndexErrors, String> formatIndexErrors = indexErrors -> {
            String errorsListText = Arrays.stream(indexErrors.getErrors()).map(x -> "-" + x).collect(Collectors.joining(System.lineSeparator()));
            return "Index " + indexErrors.getName() + " (" + indexErrors.getErrors().length + " errors): "+ System.lineSeparator() + errorsListText;
        };
        if (errors != null && errors.length > 0) {
            allIndexErrorsText = Arrays.stream(errors).map(formatIndexErrors).collect(Collectors.joining(System.lineSeparator()));
        }

        throw new TimeoutException("The indexes stayed stale for more than " + timeout + "." + allIndexErrorsText);
    }


    public void waitForUserToContinueTheTest(IDocumentStore store) {
        String databaseNameEncoded = UrlUtils.escapeDataString(store.getDatabase());
        String documentsPage = store.getUrls()[0] + "/studio/index.html#databases/documents?&database=" + databaseNameEncoded + "&withStop=true";

        openBrowser(documentsPage);

        do {
            try {
                Thread.sleep(500);
            } catch (InterruptedException ignored) {
            }

            try (IDocumentSession session = store.openSession()) {
                if (session.load(ObjectNode.class, "Debug/Done") != null) {
                    break;
                }
            }

        } while (true);
    }

    protected void openBrowser(String url) {
        System.out.println(url);

        if (Desktop.isDesktopSupported()) {
            Desktop desktop = Desktop.getDesktop();
            try {
                desktop.browse(new URI(url));
            } catch (IOException | URISyntaxException e) {
                throw new RuntimeException(e);
            }
        } else {
            Runtime runtime = Runtime.getRuntime();
            try {
                runtime.exec("xdg-open " + url);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static void reportError(Exception e) {
        if (!debug) {
            return;
        }

        if (e == null) {
            throw new IllegalArgumentException("Exception can not be null");
        }
    }

    @SuppressWarnings("EmptyMethod")
    private static void reportInfo(String message) {
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
            throw new RuntimeException(exceptions.stream().map(x -> x.toString()).collect(Collectors.joining(", ")));
        }
    }
}
