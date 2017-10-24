package net.ravendb.client.test.driver;

import com.google.common.base.Stopwatch;
import com.sun.javaws.exceptions.InvalidArgumentException;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.exceptions.DatabaseDoesNotExistException;
import net.ravendb.client.primitives.CleanCloseable;
import net.ravendb.client.serverwide.DatabaseRecord;
import net.ravendb.client.serverwide.operations.CreateDatabaseOperation;
import net.ravendb.client.serverwide.operations.DeleteDatabasesOperation;
import org.apache.commons.lang3.NotImplementedException;

import java.io.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public abstract class RavenTestDriver implements CleanCloseable {

    private RavenServerLocator locator;

    private static IDocumentStore globalServer;

    private static Process globalServerProcess;

    private final ConcurrentMap<DocumentStore, Object> documentStores = new ConcurrentHashMap<>();

    private static AtomicInteger index = new AtomicInteger();

    protected boolean disposed;

    public RavenTestDriver(RavenServerLocator locator) {
        this.locator = locator;
    }

    public boolean isDisposed() {
        return disposed;
    }

    public static boolean debug;

    public static Process getGlobalServerProcess() {
        return globalServerProcess;
    }

    public IDocumentStore getDocumentStore() throws IOException {
        return getDocumentStore("test_db");
    }

    public IDocumentStore getDocumentStore(String database) throws IOException {
        return getDocumentStore(database, null);
    }

    public IDocumentStore getDocumentStore(String database, Duration waitForIndexingTimeout) throws IOException {
        String name = database + "_" + index.incrementAndGet();
        reportInfo("getDocumentStore for db " + database + ".");

        if (globalServer == null) {
            synchronized (RavenTestDriver.class) {
                if (globalServer == null) {
                    runServer();
                }
            }
        }

        IDocumentStore documentStore = globalServer;
        DatabaseRecord databaseRecord = new DatabaseRecord();
        databaseRecord.setDatabaseName(name);
        CreateDatabaseOperation createDatabaseOperation = new CreateDatabaseOperation(databaseRecord);
        documentStore.admin().server().send(createDatabaseOperation);


        DocumentStore store = new DocumentStore();
        store.setUrls(documentStore.getUrls());
        store.setDatabase(name);

        store.initialize();

        store.addAfterCloseListener(((sender, event) -> {
            if (!documentStores.containsKey(store)) {
                return;
            }

            try {
                store.admin().server().send(new DeleteDatabasesOperation(store.getDatabase(), true));
            } catch (DatabaseDoesNotExistException e) {
                // ignore
            } //TODO: NoLeaderException
        }));

        setupDatabase(store);

        /* TODO
         if (waitForIndexingTimeout.HasValue)
                WaitForIndexing(store, name, waitForIndexingTimeout);
         */

        documentStores.remove(store);
        return store;
    }


    protected void setupDatabase(IDocumentStore documentStore) {
        // empty by design
    }

    private IDocumentStore runServer() throws IOException {
        Process process = globalServerProcess = RavenServerRunner.run(this.locator);

        reportInfo("Starting global server");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> killGlobalServerProcess()));

        String url = null;
        InputStream stdout = globalServerProcess.getInputStream();

        StringBuilder sb = new StringBuilder();

        Stopwatch startupDuration = Stopwatch.createStarted();
        BufferedReader reader = new BufferedReader (new InputStreamReader(stdout));

        while (true) {

            //TODO: handle timeout!
            String line = reader.readLine();

            if (startupDuration.elapsed(TimeUnit.MINUTES) >= 1) {
                break;
            }
            String prefix = "Server available on: ";
            if (line.startsWith(prefix))
            {
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
        store.setUrls(new String[] { url });
        store.setDatabase("test.manager");

        globalServer = store;
        return store.initialize();
    }

    private static void killGlobalServerProcess() {
        Process p = RavenTestDriver.globalServerProcess;
        globalServerProcess = null;
        if (p != null && p.isAlive()) {
            reportInfo("Kill global server");

            try {
                p.destroyForcibly();
            } catch (Exception e) {
                reportError(e);
            }
        }
    }

    public void waitForIndexing(IDocumentStore store, String database) {
        throw new NotImplementedException("not yet impolemeneted");
        /* TODO


        public void WaitForIndexing(IDocumentStore store, string database = null, TimeSpan? timeout = null)
        {
            var admin = store.Admin.ForDatabase(database);

            timeout = timeout ?? TimeSpan.FromMinutes(1);

            var sp = Stopwatch.StartNew();
            while (sp.Elapsed < timeout.Value)
            {
                var databaseStatistics = admin.Send(new GetStatisticsOperation());
                var indexes = databaseStatistics.Indexes
                    .Where(x => x.State != IndexState.Disabled);

                if (indexes.All(x => x.IsStale == false
                    && x.Name.StartsWith(Constants.Documents.Indexing.SideBySideIndexNamePrefix) == false))
                    return;

                if (databaseStatistics.Indexes.Any(x => x.State == IndexState.Error))
                {
                    break;
                }

                Thread.Sleep(100);
            }

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


    /* TODO
    {




        public void WaitForUserToContinueTheTest(IDocumentStore store)
        {
            var databaseNameEncoded = Uri.EscapeDataString(store.Database);
            var documentsPage = store.Urls[0] + "/studio/index.html#databases/documents?&database=" + databaseNameEncoded + "&withStop=true";

            OpenBrowser(documentsPage); // start the server

            do
            {
                Thread.Sleep(500);

                using (var session = store.OpenSession())
                {
                    if (session.Load<object>("Debug/Done") != null)
                        break;
                }
            } while (true);
        }

        protected virtual void OpenBrowser(string url)
        {
            Console.WriteLine(url);

            if (RuntimeInformation.IsOSPlatform(OSPlatform.Windows))
            {
                Process.Start(new ProcessStartInfo("cmd", $"/c start \"Stop & look at studio\" \"{url}\"")); // Works ok on windows
                return;
            }

            if (RuntimeInformation.IsOSPlatform(OSPlatform.Linux))
            {
                Process.Start("xdg-open", url); // Works ok on linux
                return;
            }

            throw new NotImplementedException("Implement your own browser opening mechanism.");
        }
        */

    private static void reportError(Exception e) {
        if (!debug) {
            return;
        }

        if (e == null ){
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

        for (DocumentStore documentStore : documentStores.keySet()) {
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
