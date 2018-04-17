package net.ravendb.client.test.driver;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Sets;
import net.ravendb.client.Constants;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.indexes.IndexState;
import net.ravendb.client.documents.operations.MaintenanceOperationExecutor;
import net.ravendb.client.documents.operations.DatabaseStatistics;
import net.ravendb.client.documents.operations.GetStatisticsOperation;
import net.ravendb.client.documents.operations.IndexInformation;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.exceptions.TimeoutException;
import net.ravendb.client.exceptions.cluster.NoLeaderException;
import net.ravendb.client.exceptions.database.DatabaseDoesNotExistException;
import net.ravendb.client.primitives.CleanCloseable;
import net.ravendb.client.serverwide.DatabaseRecord;
import net.ravendb.client.serverwide.operations.CreateDatabaseOperation;
import net.ravendb.client.serverwide.operations.DeleteDatabasesOperation;
import net.ravendb.client.util.UrlUtils;
import org.apache.commons.io.IOUtils;

import java.awt.*;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
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
        }

        customizeStore(store);

        store.initialize();

        store.addAfterCloseListener(((sender, event) -> {
            if (!documentStores.contains(store)) {
                return;
            }

            try {
                store.maintenance().server().send(new DeleteDatabasesOperation(store.getDatabase(), true));
            } catch (DatabaseDoesNotExistException e) {
                // ignore
            } catch (NoLeaderException e) {
                // ignore
            }
        }));

        setupDatabase(store);

        /* TODO
         if (waitForIndexingTimeout.HasValue)
                WaitForIndexing(store, name, waitForIndexingTimeout);
         */

        documentStores.add(store);
        return store;
    }


    @SuppressWarnings("EmptyMethod")
    protected void setupDatabase(IDocumentStore documentStore) {
        // empty by design
    }

    private IDocumentStore runServer(boolean secured) throws Exception {
        Process process = RavenServerRunner.run(secured ? this.securedLocator : this.locator);
        setGlobalServerProcess(secured, process);

        reportInfo("Starting global server");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> killGlobalServerProcess(secured)));

        String url = null;
        InputStream stdout = getGlobalProcess(secured).getInputStream();

        StringBuilder sb = new StringBuilder();

        Stopwatch startupDuration = Stopwatch.createStarted();
        BufferedReader reader = new BufferedReader(new InputStreamReader(stdout));

        List<String> readLines = new ArrayList<>();

        while (true) {

            //TODO: handle timeout!
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

            /*TODO
             Task<string> readLineTask = null;
            while (true)
            {
                if (readLineTask == null)
                    readLineTask = output.ReadLineAsync();

                var task = Task.WhenAny(readLineTask, Task.Delay(TimeSpan.FromSeconds(5))).Result;

                if (startupDuration.Elapsed > TimeSpan.FromMinutes(1))
                    break;

                if (task != readLineTask)
                    continue;

                var line = readLineTask.Result;

                readLineTask = null;

                sb.AppendLine(line);

                if (line == null)
                {
                    try
                    {
                        process.Kill();
                    }
                    catch (Exception e)
                    {
                        ReportError(e);
                    }

                    throw new InvalidOperationException("Unable to start server, log is: " + Environment.NewLine + sb);
                }

            }
             */
        }

        if (url == null) {
            String log = sb.toString();
            reportInfo(log);

            try {
                process.destroyForcibly();
            } catch (Exception e) {
                reportError(e);
            }

            throw new IllegalStateException("Unable to start server, log is: " + log);
        }

        DocumentStore store = new DocumentStore();
        store.setUrls(new String[]{url});
        store.setDatabase("test.manager");
        store.getConventions().setDisableTopologyUpdates(true);

        if (secured) {
            globalSecuredServer = store;
            KeyStore clientCert = getTestClientCertificate();
            store.setCertificate(clientCert);
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
        } else {
            p = RavenTestDriver.globalServerProcess;
            globalServerProcess = null;
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

    public void waitForIndexing(IDocumentStore store) {
        waitForIndexing(store, null, null);
    }

    public void waitForIndexing(IDocumentStore store, String database) {
        waitForIndexing(store, database, null);
    }

    public void waitForIndexing(IDocumentStore store, String database, Duration timeout) {
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

        throw new TimeoutException(); //TODO:
        /* TODO
            var errors = admin.Send(new GetIndexErrorsOperation());

            string allIndexErrorsText = string.Empty;
            if (errors != null && errors.Length > 0)
            {
                var allIndexErrorsListText = string.Join("\r\n",
                    errors.Select(FormatIndexErrors));
                allIndexErrorsText = $"Indexing errors:\r\n{ allIndexErrorsListText }";

                string FormatIndexErrors(IndexErrors indexErrors)
                {
                    var errorsListText = string.Join("\r\n",
                        indexErrors.Errors.Select(x => $"- {x}"));
                    return $"Index '{indexErrors.Name}' ({indexErrors.Errors.Length} errors):\r\n{errorsListText}";
                }
            }

            throw new TimeoutException($"The indexes stayed stale for more than {timeout.Value}.{ allIndexErrorsText }");
        }
         */
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


        /* TODO:
        var msg = $"{DateTime.Now}: {e}\r\n";
            File.AppendAllText("raven_testdriver.log", msg);
            Console.WriteLine(msg);
         */
    }

    private static void reportInfo(String message) {
        /* TODO:
         if (Debug == false)
                return;

            if (string.IsNullOrWhiteSpace(message))
                throw new ArgumentNullException(nameof(message));

            var msg = $"{DateTime.Now}: {message}\r\n";
            File.AppendAllText("raven_testdriver.log", msg);
            Console.WriteLine(msg);
         */
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
